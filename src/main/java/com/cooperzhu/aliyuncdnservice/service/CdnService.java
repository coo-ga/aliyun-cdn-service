package com.cooperzhu.aliyuncdnservice.service;

import com.aliyun.cdn20180510.Client;
import com.aliyun.cdn20180510.models.*;
import com.aliyun.tea.NameInMap;
import com.aliyun.teaopenapi.models.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
public class CdnService {
	//region properties
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	@Autowired
	private IPService ipService;
	@Value("${userconfig.aliyun.cdn.end-point}")
	private String endPoint;
	@Value("${userconfig.aliyun.cdn.access-key-id}")
	private String accessKeyId;
	@Value("${userconfig.aliyun.cdn.access-key-secret}")
	private String accessKeySecret;
	@Value("${userconfig.aliyun.cdn.cert-root-path}")
	private String certRootPath;

	private Client client;
	//endregion

	public CdnService() throws Exception {
		this.client = this.createClient();
	}

	public String checkAndUpdate() {
		boolean ip = false;
		boolean ssl = false;
		try {
			var map = this.getDomainsConfigs();
			ip = this.checkAndUpdateIP(map);
			ssl = this.checkAndUpdateSSL(map);
		} catch (Exception ex) {
			log.error("checkAndUpdate Error", ex);
		} finally {
			return String.format("{\n" +
					"\t\"ip\":%s,\n" +
					"\t\"ssl\":%s\n" +
					"}", ip, ssl);
		}
	}

	//region get domains configs
	private Map<String, DescribeUserDomainsResponseBody.DescribeUserDomainsResponseBodyDomainsPageData> getDomainsConfigs() throws Exception {
		DescribeUserDomainsRequest request = new DescribeUserDomainsRequest();
		DescribeUserDomainsResponse response = this.client.describeUserDomains(request);
		DescribeUserDomainsResponseBody body = response.getBody();

		Map<String, DescribeUserDomainsResponseBody.DescribeUserDomainsResponseBodyDomainsPageData> map = new HashMap<>();
		body.getDomains().getPageData().forEach((item) -> {
			map.put(item.getDomainName(), item);
		});

		return map;
	}
	//endregion

	//region update ip
	public boolean checkAndUpdateIP(Map<String, DescribeUserDomainsResponseBody.DescribeUserDomainsResponseBodyDomainsPageData> map) throws Exception {
		try {
			String ip = this.ipService.getPublicIp();
			if (ip == null) {
				log.error("无法获取公网ip，终止更新CDN");
				return false;
			}

			return this.checkAndUpdateIP(ip, map);
		} catch (Exception ex) {
			log.error("update IP Error", ex);
			return false;
		}
	}

	private boolean checkAndUpdateIP(String newIP, Map<String, DescribeUserDomainsResponseBody.DescribeUserDomainsResponseBodyDomainsPageData> map) throws Exception {
		boolean success = true;
		for (String domain : map.keySet()) {
			var newSources = this.getUpdateIp(newIP, map.get(domain).getSources().source);
			if (null != newSources) {
				var sources = OBJECT_MAPPER.writeValueAsString(newSources);
				BatchUpdateCdnDomainRequest request = new BatchUpdateCdnDomainRequest()
						.setDomainName(domain)
						.setSources(sources);
				try {
					BatchUpdateCdnDomainResponse response = this.client.batchUpdateCdnDomain(request);
					if (response.getStatusCode() == 200) {
						log.info(String.format("update IP success [domain=%s, source=%s]", domain, sources));
					} else {
						success = false;
						log.error(String.format("update IP fail [domain=%s, source=%s, statusCode=%s]", domain, sources, response.getStatusCode()));
					}
				} catch (Exception ex) {
					success = false;
					log.error(String.format("update IP fail [domain=%s, source=%s]", domain, sources), ex);
				}
			}
		}
		return success;
	}

	private List<DescribeUserDomainsResponseBody.DescribeUserDomainsResponseBodyDomainsPageDataSourcesSource> getUpdateIp(String newIP, List<DescribeUserDomainsResponseBody.DescribeUserDomainsResponseBodyDomainsPageDataSourcesSource> sources) {
		if (null != sources && sources.size() > 0) {
			boolean hasIP = false;
			boolean needUpdateIP = false;
			for (var src : sources) {
				if (src.getType().equalsIgnoreCase("ipaddr")) {
					hasIP = true;
					if (!newIP.equalsIgnoreCase(src.getContent())) {
						src.setContent(newIP);
						needUpdateIP = true;
					}
				}
			}

			if (hasIP) {
				return needUpdateIP ? sources : null;
			}
		}

		if (null == sources) {
			sources = new ArrayList<>();
		}

		var src = new DescribeUserDomainsResponseBody.DescribeUserDomainsResponseBodyDomainsPageDataSourcesSource();
		src.setContent(newIP);
		src.setPort(81);
		src.setPriority("20");
		src.setType("ipaddr");
		src.setWeight("10");
		sources.add(src);
		return sources;
	}

	//endregion

	//region update ssl
	private boolean checkAndUpdateSSL(Map<String, DescribeUserDomainsResponseBody.DescribeUserDomainsResponseBodyDomainsPageData> map) throws Exception {
		boolean success = true;
		try {
			for (String domain : map.keySet()) {
				if (this.isNeedUpdateSSL(domain)) {
					if (!this.updateSSL(domain)) {
						success = false;
					}
				}
			}
		} catch (Exception ex) {
			log.error("update SSL fail", ex);
			success = false;
		}
		return success;
	}

	private boolean isNeedUpdateSSL(String domain) throws Exception {
		var req = new DescribeDomainCertificateInfoRequest().setDomainName(domain);
		var certBody = this.client.describeDomainCertificateInfo(req).getBody().getCertInfos();

		if (null == certBody) {
			return true;
		}

		for (var cert : certBody.getCertInfo()) {
			Instant expireTime = Instant.parse(cert.getCertExpireTime());
			Instant now = Instant.now();
			Instant tenDaysLater = now.plus(10, ChronoUnit.DAYS);
			return tenDaysLater.isAfter(expireTime);
		}

		return false;
	}

	private boolean updateSSL(String domain) throws Exception {
		boolean success = true;
		try {
			String pub = Files.readString(Path.of(String.format("%s/fullchain.pem", this.certRootPath)));
			String pri = Files.readString(Path.of(String.format("%s/key.pem", this.certRootPath)));
			var req = new SetCdnDomainSSLCertificateRequest()
					.setDomainName(domain)
					.setSSLProtocol("on")               // 启用HTTPS证书
					.setCertType("upload")              // 证书类型: 上传
//					.setCertName("my-latest-cert")      // 新证书名称
					.setSSLPub(pub)             // 证书内容
					.setSSLPri(pri); // 私钥内容;
			var resp = this.client.setCdnDomainSSLCertificate(req);
			if (resp.getStatusCode() == 200) {
				log.info(String.format("update SSL success [domain=%s]", domain));
			} else {
				success = false;
				log.error(String.format("update SSL fail [domain=%s, source=%s, statusCode=%s]", domain, resp.getStatusCode()));

			}
		} catch (Exception ex) {
			success = false;
			log.error(String.format("update SSL fail [domain=%s, source=%s]", domain), ex);
		} finally {
			return success;
		}
	}

	//endregion

	//region client
	private Client createClient() throws Exception {
		Config config = new Config()
				.setAccessKeyId(accessKeyId)
				.setAccessKeySecret(accessKeySecret);
		config.endpoint = this.endPoint; // Endpoint 请参考 https://api.aliyun.com/product/Cdn
		return new Client(config);
	}
	//endregion
}

package com.cooperzhu.aliyuncdnservice.service;

import com.aliyun.cdn20180510.Client;
import com.aliyun.cdn20180510.models.*;
import com.aliyun.teaopenapi.models.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

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
    //endregion

    public void checkAndUpdate() {
        String ip = this.ipService.getPublicIp();
        if (ip == null) {
            log.error("无法获取公网ip，终止更新CDN");
            return;
        }

        try {
            Client client = createClient();
            Map<String, DescribeUserDomainsResponseBody.DescribeUserDomainsResponseBodyDomainsPageDataSourcesSource> map = this.describeUserDomains(client);
            this.batchUpdateCdnDomain(ip, client, map);
        } catch (Exception ex) {
            log.error("Fail", ex);
        }
    }

    //region 获取解析记录列表
    private Map<String, DescribeUserDomainsResponseBody.DescribeUserDomainsResponseBodyDomainsPageDataSourcesSource> describeUserDomains(Client client) throws Exception {
        DescribeUserDomainsRequest request = new DescribeUserDomainsRequest();
        DescribeUserDomainsResponse response = client.describeUserDomains(request);
        DescribeUserDomainsResponseBody body = response.getBody();

        Map<String, DescribeUserDomainsResponseBody.DescribeUserDomainsResponseBodyDomainsPageDataSourcesSource> map = new HashMap<>();
        body.getDomains().getPageData().forEach((item) -> {
            item.getSources().source.forEach((src) -> {
                map.put(item.getDomainName(), src);
            });
        });
        return map;
    }

    private void batchUpdateCdnDomain(String ip, Client client, Map<String, DescribeUserDomainsResponseBody.DescribeUserDomainsResponseBodyDomainsPageDataSourcesSource> map) throws Exception {
        String source;

        DescribeUserDomainsResponseBody.DescribeUserDomainsResponseBodyDomainsPageDataSourcesSource src;
        for (String key : map.keySet()) {
            src = map.get(key);
            if (ip.equals(src.getContent())) {
                continue;
            }

            src.setContent(ip);
            source = String.format("[%s]", OBJECT_MAPPER.writeValueAsString(src));
            System.out.println();
            BatchUpdateCdnDomainRequest request = new BatchUpdateCdnDomainRequest()
                    .setDomainName(key)
                    .setSources(source);
            try {
                BatchUpdateCdnDomainResponse response = client.batchUpdateCdnDomain(request);
                if (response.getStatusCode() == 200) {
                    log.info(String.format("update success [domain=%s, source=%s]", key, source));
                } else {
                    log.error(String.format("update fail [domain=%s, source=%s, statusCode=%s]", key, source, response.getStatusCode()));
                }
            } catch (Exception ex) {
                log.error(String.format("update fail [domain=%s, source=%s]", key, source), ex);
            }
        }
    }
    //endregion

    private Client createClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret);
        config.endpoint = this.endPoint; // Endpoint 请参考 https://api.aliyun.com/product/Cdn
        return new Client(config);
    }
}

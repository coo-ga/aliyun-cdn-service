package com.cooperzhu.aliyuncdnservice.controller;

import com.cooperzhu.aliyuncdnservice.service.CdnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cdn")
public class CdnController {
	@Autowired
	private CdnService cdnService;

	//region renew ip
	@GetMapping("/renewip")
	public String renewip() {
		return this.renewip(null);
	}

	@GetMapping("/renewip/{ip}")
	public String renewip(@PathVariable(required = false) String ip) {
		return this.cdnService.checkAndUpdateIp(ip);
	}
	//endregion

	//region renew ip
	@GetMapping("/renewssl")
	public String renewssl() {
		return this.cdnService.checkAndUpdateSsl();
	}
	//endregion

	//region renew all
	@GetMapping("/renew")
	public String renewall() {
		return this.renewall(null);
	}

	@GetMapping("/renew/{ip}")
	public String renewall(@PathVariable(required = false) String ip) {
		return this.cdnService.checkAndUpdate(ip);
	}
	//endregion
}

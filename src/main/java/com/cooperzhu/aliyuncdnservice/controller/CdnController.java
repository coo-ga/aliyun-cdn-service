package com.cooperzhu.aliyuncdnservice.controller;

import com.cooperzhu.aliyuncdnservice.service.CdnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cdn")
public class CdnController {
	@Autowired
	private CdnService cdnService;

	@GetMapping("/renew")
	public String renew() {
		return this.cdnService.checkAndUpdate();
	}
}

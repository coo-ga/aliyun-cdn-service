package com.cooperzhu.aliyuncdnservice.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CdnServiceTests {

	@Autowired
	private CdnService cdnService;

	@Test
	public void testCheckAndUpdate() {
		cdnService.checkAndUpdate();
	}
}

package com.cooperzhu.aliyuncdnservice.task;

import com.cooperzhu.aliyuncdnservice.service.CdnService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CdnCheckTask {
	@Autowired
	private CdnService cdnService;

	@Scheduled(fixedDelayString = "${userconfig.task.period}", timeUnit = TimeUnit.MINUTES)
	public void start() {
		long begin = System.currentTimeMillis();
		log.info("begin check");
		cdnService.checkAndUpdate();
		long end = System.currentTimeMillis();
		log.info("finish check in " + (end - begin) + " ms");
	}
}

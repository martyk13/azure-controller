package com.kenesys.analysisplatform.services.templates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class GitScannerScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitScannerScheduler.class);

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Scheduled(fixedRateString  = "${templates.git.scanner.schedule}")
    public void reportCurrentTime() {
        LOGGER.info("The time is now {}", dateFormat.format(new Date()));
    }
}

package com.kenesys.analysisplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
public class AnalysisPlatformApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(AnalysisPlatformApplication.class, args);
	}
}

package com.kenesys.analysisplatform.config;

import com.kenesys.analysisplatform.services.templates.GitScanner;
import com.kenesys.analysisplatform.services.templates.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
@EnableScheduling
public class SchedulingConfig  implements SchedulingConfigurer {

    @Value("${templates.gitscanner.schedule}")
    private String gitScannerSchedule;

    @Value( "${templates.gitscanner.gitdir}" )
    private String gitDirectory;

    @Value( "${templates.gitscanner.gituri}" )
    private String gitRepositoryUri;

    @Value( "${templates.gitscanner.gitbranch}" )
    public String gitBranch;

    @Autowired
    private TemplateService templateService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(platformScheduler());
    }

    @Bean
    public TaskScheduler platformScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("platformScheduler");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.initialize();
        scheduler.scheduleWithFixedDelay(new GitScanner(gitDirectory, gitRepositoryUri, gitBranch, templateService), Long.valueOf(gitScannerSchedule));
        return scheduler;
    }
}

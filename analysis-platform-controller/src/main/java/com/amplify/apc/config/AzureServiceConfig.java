package com.amplify.apc.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amplify.apc.services.azure.AzureService;
import com.amplify.apc.services.azure.AzureServiceCloud;
import com.amplify.apc.services.azure.AzureServiceStack;

@Configuration
public class AzureServiceConfig {
 
    @Bean
    @ConditionalOnProperty(name = "azure.variant", havingValue = "cloud", matchIfMissing = true)
    public AzureService cloudAzureService() {
        return new AzureServiceCloud();
    }
 
    @Bean
    @ConditionalOnProperty(name = "azure.variant", havingValue = "stack")
    public AzureService stackAzureService() {
        return new AzureServiceStack();
    }
}

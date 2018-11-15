package com.kenesys.analysisplatform;

import com.kenesys.analysisplatform.api.TemplatesApi;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

@Component
public class JerseyConfig extends ResourceConfig
{
    public JerseyConfig()
    {
        register(TemplatesApi.class);
    }
}

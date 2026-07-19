package com.maplemetric.global.config;

import com.maplemetric.global.config.properties.NexonApiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    private static final String NEXON_API_KEY_HEADER = "x-nxopen-api-key";

    @Bean("nexonRestClient")
    public RestClient nexonRestClient(
            NexonApiProperties nexonApiProperties
    ) {
        return RestClient.builder()
                .baseUrl(nexonApiProperties.baseUrl())
                .defaultHeader(
                        NEXON_API_KEY_HEADER,
                        nexonApiProperties.key()
                )
                .build();
    }
}
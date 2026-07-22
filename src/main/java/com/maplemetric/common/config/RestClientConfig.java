package com.maplemetric.common.config;

import com.maplemetric.common.config.properties.NexonApiProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    private static final String NEXON_API_KEY_HEADER =
            "x-nxopen-api-key";

    private static final Duration CONNECT_TIMEOUT =
            Duration.ofSeconds(3);

    private static final Duration READ_TIMEOUT =
            Duration.ofSeconds(5);

    @Bean("nexonRestClient")
    public RestClient nexonRestClient(NexonApiProperties nexonApiProperties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(READ_TIMEOUT);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(nexonApiProperties.baseUrl())
                .defaultHeader(
                        NEXON_API_KEY_HEADER,
                        nexonApiProperties.key()
                )
                .build();
    }
}
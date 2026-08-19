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
                // Key를 여기 고정하지 않는다. 어느 Key로 보낼지는 관문이 요청마다
                // 정하므로 그때 헤더를 붙인다.
                .baseUrl(nexonApiProperties.baseUrl())
                .build();
    }
}
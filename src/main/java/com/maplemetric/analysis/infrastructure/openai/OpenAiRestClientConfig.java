package com.maplemetric.analysis.infrastructure.openai;

import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
class OpenAiRestClientConfig {

    @Bean("openAiRestClient")
    RestClient openAiRestClient(
            OpenAiProperties properties,
            RestClient.Builder restClientBuilder
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(properties.readTimeout());

        RestClient.Builder builder = restClientBuilder
                .requestFactory(requestFactory)
                .baseUrl(properties.baseUrl());

        if (StringUtils.hasText(properties.key())) {
            builder.defaultHeaders(
                    headers -> headers.setBearerAuth(properties.key())
            );
        }

        return builder.build();
    }
}

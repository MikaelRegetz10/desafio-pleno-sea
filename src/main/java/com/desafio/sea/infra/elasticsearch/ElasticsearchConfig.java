package com.desafio.sea.infra.elasticsearch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.desafio.sea.infra.elasticsearch")
public class ElasticsearchConfig extends ElasticsearchConfiguration {
    @Value( "${spring.elasticsearch.uris}")
    private String host;

    @Override
    public ClientConfiguration clientConfiguration() {
        String cleanUri = host.replace("http://", "").replace("https://", "");

        return ClientConfiguration.builder()
                .connectedTo(cleanUri)
                .withConnectTimeout(5000)
                .withSocketTimeout(30000)
                .build();
    }
}

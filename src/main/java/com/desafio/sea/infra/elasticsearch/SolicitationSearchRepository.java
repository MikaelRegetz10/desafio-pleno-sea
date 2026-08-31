package com.desafio.sea.infra.elasticsearch;

import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SolicitationSearchRepository extends ElasticsearchRepository<SolicitationDocument, UUID> {
}

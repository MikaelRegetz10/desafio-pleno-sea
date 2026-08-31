package com.desafio.sea.infra.elasticsearch;

import com.desafio.sea.domain.Solicitation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class SolicitationIndexerService {

    @Autowired
    private SolicitationSearchRepository repository;

    public void index(Solicitation solicitation) {
        if (solicitation == null || solicitation.getId() == null) {
            log.warn("Tentativa de indexar solicitação nula ou sem ID ignorada.");
            return;
        }

        SolicitationDocument document = SolicitationDocument.fromEntity(solicitation);
        repository.save(document);
        log.info("Solicitação [{}] indexada com sucesso no Elasticsearch.", solicitation.getId());
    }

    public void indexAll(Collection<Solicitation> solicitations) {
        if (solicitations == null || solicitations.isEmpty()) {
            return;
        }

        List<SolicitationDocument> documents = solicitations.stream()
                .filter(s -> s != null && s.getId() != null)
                .map(SolicitationDocument::fromEntity)
                .toList();

        if (!documents.isEmpty()) {
            repository.saveAll(documents);
            log.info("[{}] solicitações indexadas em lote no Elasticsearch.", documents.size());
        }
    }

    public void delete(UUID id) {
        if (id == null) {
            return;
        }

        repository.deleteById(id);
        log.info("Solicitação [{}] removida do Elasticsearch.", id);
    }
}
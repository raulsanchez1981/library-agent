package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.entity.AuthorEntity;
import com.libraryagent.ingestion.entity.VerifiedTitleEntity;
import com.libraryagent.ingestion.extractor.Confidence;
import com.libraryagent.ingestion.model.ExtractedBookEntity;
import com.libraryagent.ingestion.repository.AuthorRepository;
import com.libraryagent.ingestion.repository.ExtractedBookRepository;
import com.libraryagent.ingestion.repository.VerifiedTitleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookLinkingServiceImpl implements BookLinkingService {

    private final ExtractedBookRepository repository;
    private final VerifiedTitleRepository verifiedTitleRepository;
    private final AuthorRepository authorRepository;

    public BookLinkingServiceImpl(
            ExtractedBookRepository repository,
            VerifiedTitleRepository verifiedTitleRepository,
            AuthorRepository authorRepository) {
        this.repository = repository;
        this.verifiedTitleRepository = verifiedTitleRepository;
        this.authorRepository = authorRepository;
    }

    @Override
    @Transactional
    public void linkUnverifiedBooks() {
        // 1. Asignar verifiedTitle a libros que coincidan por titleEs (case-insensitive)
        // Búsqueda O(1) por nombre usando Map — evita O(n²) y N saves individuales
        List<ExtractedBookEntity> sinTituloVerificado = repository.findByVerifiedTitleIsNullAndTitleEsIsNotNull();
        if (!sinTituloVerificado.isEmpty()) {
            Map<String, VerifiedTitleEntity> vtByName = verifiedTitleRepository.findAll().stream()
                    .collect(Collectors.toMap(
                            vt -> vt.getName().toLowerCase(),
                            vt -> vt,
                            (a, b) -> a
                    ));

            List<ExtractedBookEntity> porVincular = new ArrayList<>();
            for (ExtractedBookEntity book : sinTituloVerificado) {
                VerifiedTitleEntity vt = vtByName.get(book.getTitleEs().toLowerCase());
                if (vt != null) {
                    book.setVerifiedTitle(vt);
                    porVincular.add(book);
                }
            }
            if (!porVincular.isEmpty()) {
                repository.saveAll(porVincular);
            }
        }

        // 2. Vincular autores verificados a libros no-VERIFIED donde coincida authorCorrected
        // Búsqueda O(1) por nombre usando Map — evita bucle anidado y N saves individuales
        List<AuthorEntity> autoresVerificados = authorRepository.findByVerifiedTrue();
        if (autoresVerificados.isEmpty()) {
            return;
        }

        Map<String, AuthorEntity> autorByName = autoresVerificados.stream()
                .collect(Collectors.toMap(
                        a -> a.getName().toLowerCase(),
                        a -> a,
                        (a, b) -> a
                ));

        List<ExtractedBookEntity> candidatos = repository.findByConfidenceNotAndAuthorCorrectedIsNotNull(Confidence.VERIFIED);
        List<ExtractedBookEntity> modificados = new ArrayList<>();
        for (ExtractedBookEntity book : candidatos) {
            List<String> nombresBook = AuthorNameParser.parse(book.getAuthorCorrected()).stream()
                    .map(String::toLowerCase)
                    .toList();

            Set<UUID> idsActuales = book.getAuthors().stream()
                    .map(AuthorEntity::getId)
                    .collect(Collectors.toSet());

            boolean cambio = false;
            for (String nombre : nombresBook) {
                AuthorEntity autor = autorByName.get(nombre);
                if (autor != null && !idsActuales.contains(autor.getId())) {
                    book.getAuthors().add(autor);
                    cambio = true;
                }
            }
            if (cambio) {
                modificados.add(book);
            }
        }

        if (!modificados.isEmpty()) {
            repository.saveAll(modificados);
        }
    }
}

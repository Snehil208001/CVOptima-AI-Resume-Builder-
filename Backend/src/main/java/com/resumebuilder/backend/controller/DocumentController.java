package com.resumebuilder.backend.controller;

import com.resumebuilder.backend.dto.DocumentDTO;
import com.resumebuilder.backend.entity.Document;
import com.resumebuilder.backend.repository.DocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentRepository documentRepository;

    public DocumentController(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @PostMapping
    public ResponseEntity<DocumentDTO> createDocument(@RequestBody DocumentDTO dto) {
        Long userId = dto.getUserId() != null ? dto.getUserId() : 1L;
        String status = dto.getStatus() != null ? dto.getStatus() : "PENDING";

        Document document = Document.builder()
                .userId(userId)
                .jobTitle(dto.getJobTitle())
                .companyName(dto.getCompanyName())
                .jdText(dto.getJdText())
                .resumeMd(dto.getResumeMd())
                .status(status)
                .build();

        document = documentRepository.save(document);

        DocumentDTO responseDto = mapToDTO(document);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @GetMapping
    public ResponseEntity<List<DocumentDTO>> getDocuments(
            @RequestParam(name = "userId", required = false, defaultValue = "1") Long userId) {
        List<Document> documents = documentRepository.findByUserId(userId);
        List<DocumentDTO> dtos = documents.stream()
                .map(this::mapToDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDTO> getDocumentById(@PathVariable UUID id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document with ID " + id + " not found"));

        DocumentDTO responseDto = mapToDTO(document);
        return ResponseEntity.ok(responseDto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentDTO> updateDocument(@PathVariable UUID id, @RequestBody DocumentDTO dto) {
        Document existingDocument = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document with ID " + id + " not found"));

        existingDocument.setJobTitle(dto.getJobTitle());
        existingDocument.setCompanyName(dto.getCompanyName());
        if (dto.getJdText() != null) {
            existingDocument.setJdText(dto.getJdText());
        }
        existingDocument.setResumeMd(dto.getResumeMd());
        if (dto.getStatus() != null) {
            existingDocument.setStatus(dto.getStatus());
        }

        existingDocument = documentRepository.save(existingDocument);
        return ResponseEntity.ok(mapToDTO(existingDocument));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) {
        if (!documentRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        documentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private DocumentDTO mapToDTO(Document document) {
        return DocumentDTO.builder()
                .id(document.getId())
                .userId(document.getUserId())
                .jobTitle(document.getJobTitle())
                .companyName(document.getCompanyName())
                .jdText(document.getJdText())
                .resumeMd(document.getResumeMd())
                .status(document.getStatus())
                .build();
    }
}

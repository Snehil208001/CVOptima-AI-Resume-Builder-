package com.resumebuilder.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumebuilder.backend.dto.DocumentDTO;
import com.resumebuilder.backend.entity.Document;
import com.resumebuilder.backend.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import com.resumebuilder.backend.security.SecurityConfig;
import com.resumebuilder.backend.security.JwtAuthenticationFilter;

@WebMvcTest(
    value = DocumentController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class},
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {SecurityConfig.class, JwtAuthenticationFilter.class}
    )
)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DocumentRepository documentRepository;

    @Test
    void testCreateDocument_Success() throws Exception {
        UUID docId = UUID.randomUUID();
        Document mockSaved = Document.builder()
                .id(docId)
                .userId(1L)
                .jobTitle("React Architect")
                .companyName("Netflix")
                .status("PENDING")
                .build();

        when(documentRepository.save(any(Document.class))).thenReturn(mockSaved);

        DocumentDTO requestDto = DocumentDTO.builder()
                .jobTitle("React Architect")
                .companyName("Netflix")
                .build();

        mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(docId.toString()))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.jobTitle").value("React Architect"))
                .andExpect(jsonPath("$.companyName").value("Netflix"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testGetDocuments_Success() throws Exception {
        UUID docId = UUID.randomUUID();
        Document doc = Document.builder()
                .id(docId)
                .userId(1L)
                .jobTitle("Architect")
                .companyName("Netflix")
                .status("PENDING")
                .build();

        when(documentRepository.findByUserId(1L)).thenReturn(Collections.singletonList(doc));

        mockMvc.perform(get("/api/v1/documents?userId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(docId.toString()))
                .andExpect(jsonPath("$[0].jobTitle").value("Architect"));
    }

    @Test
    void testGetDocumentById_Success() throws Exception {
        UUID docId = UUID.randomUUID();
        Document doc = Document.builder()
                .id(docId)
                .userId(1L)
                .jobTitle("React Architect")
                .companyName("Netflix")
                .status("COMPLETED")
                .resumeMd("# John Doe resume")
                .build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        mockMvc.perform(get("/api/v1/documents/" + docId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.resumeMd").value("# John Doe resume"));
    }

    @Test
    void testGetDocumentById_NotFound() throws Exception {
        UUID docId = UUID.randomUUID();
        when(documentRepository.findById(docId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/documents/" + docId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Document with ID " + docId + " not found"));
    }
}

package com.ainexus.service;

import com.ainexus.dto.TextChunk;
import com.ainexus.entity.Document;
import com.ainexus.entity.DocumentStatus;
import com.ainexus.entity.Workspace;
import com.ainexus.exception.EmbeddingException;
import com.ainexus.exception.VectorStoreException;
import com.ainexus.model.vector.VectorRecord;
import com.ainexus.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentProcessingServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private DocumentTextExtractionService textExtractionService;

    @Mock
    private DocumentTextCleaningService textCleaningService;

    @Mock
    private DocumentTextChunkingService textChunkingService;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private VectorStoreService vectorStoreService;

    @InjectMocks
    private DocumentProcessingService documentProcessingService;

    private Document testDocument;
    private Workspace testWorkspace;

    @BeforeEach
    void setUp() {
        testWorkspace = new Workspace();
        testWorkspace.setId(10L);
        testWorkspace.setName("AI Research Workspace");

        testDocument = Document.builder()
                .id(100L)
                .fileName("sample.pdf")
                .originalFilename("sample.pdf")
                .fileType("application/pdf")
                .storagePath("docs/sample.pdf")
                .status(DocumentStatus.UPLOADED)
                .workspace(testWorkspace)
                .build();
    }

    @Test
    @DisplayName("Should successfully execute pipeline and mark document INDEXED")
    void testSuccessfulIndexingPipeline() {
        when(documentRepository.findById(100L)).thenReturn(Optional.of(testDocument));
        when(fileStorageService.getRootLocation()).thenReturn(Paths.get("uploads"));
        when(textExtractionService.extractTextFromFile(any(Path.class))).thenReturn("Raw extracted content");
        when(textCleaningService.cleanText("Raw extracted content")).thenReturn("Cleaned content");

        List<TextChunk> chunks = List.of(
                new TextChunk(0, "Chunk zero content", 0, 18),
                new TextChunk(1, "Chunk one content", 19, 36)
        );
        when(textChunkingService.chunkText("Cleaned content")).thenReturn(chunks);

        List<Float> vector0 = Collections.nCopies(768, 0.1f);
        List<Float> vector1 = Collections.nCopies(768, 0.2f);
        when(embeddingService.generateEmbeddings(List.of("Chunk zero content", "Chunk one content")))
                .thenReturn(List.of(vector0, vector1));

        when(vectorStoreService.generateVectorId(10L, 100L, 0)).thenReturn("ws_10_doc_100_chk_0");
        when(vectorStoreService.generateVectorId(10L, 100L, 1)).thenReturn("ws_10_doc_100_chk_1");

        documentProcessingService.processDocumentAsync(100L);

        ArgumentCaptor<List<VectorRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStoreService).upsert(eq(10L), captor.capture());

        List<VectorRecord> records = captor.getValue();
        assertEquals(2, records.size());
        assertEquals("ws_10_doc_100_chk_0", records.get(0).id());
        assertEquals(10L, records.get(0).metadata().get("workspaceId"));
        assertEquals(100L, records.get(0).metadata().get("documentId"));
        assertEquals(0, records.get(0).metadata().get("chunkIndex"));

        verify(documentRepository, atLeastOnce()).saveAndFlush(argThat(d -> d.getStatus() == DocumentStatus.INDEXED));
    }

    @Test
    @DisplayName("Should mark FAILED when extraction fails")
    void testExtractionFailure() {
        when(documentRepository.findById(100L)).thenReturn(Optional.of(testDocument));
        when(fileStorageService.getRootLocation()).thenReturn(Paths.get("uploads"));
        when(textExtractionService.extractTextFromFile(any(Path.class))).thenThrow(new RuntimeException("Extraction error"));

        documentProcessingService.processDocumentAsync(100L);

        verify(embeddingService, never()).generateEmbeddings(any());
        verify(vectorStoreService, never()).upsert(any(), any());
        verify(documentRepository, atLeastOnce()).saveAndFlush(argThat(d -> d.getStatus() == DocumentStatus.FAILED));
    }

    @Test
    @DisplayName("Should mark FAILED when text extraction produces blank string")
    void testBlankExtractionFailure() {
        when(documentRepository.findById(100L)).thenReturn(Optional.of(testDocument));
        when(fileStorageService.getRootLocation()).thenReturn(Paths.get("uploads"));
        when(textExtractionService.extractTextFromFile(any(Path.class))).thenReturn("   ");

        documentProcessingService.processDocumentAsync(100L);

        verify(textCleaningService, never()).cleanText(any());
        verify(embeddingService, never()).generateEmbeddings(any());
        verify(vectorStoreService, never()).upsert(any(), any());
        verify(documentRepository, atLeastOnce()).saveAndFlush(argThat(d -> d.getStatus() == DocumentStatus.FAILED));
    }

    @Test
    @DisplayName("Should mark FAILED when text cleaning produces empty string")
    void testEmptyCleanedTextFailure() {
        when(documentRepository.findById(100L)).thenReturn(Optional.of(testDocument));
        when(fileStorageService.getRootLocation()).thenReturn(Paths.get("uploads"));
        when(textExtractionService.extractTextFromFile(any(Path.class))).thenReturn("Raw noisy unprocessable content");
        when(textCleaningService.cleanText("Raw noisy unprocessable content")).thenReturn("");

        documentProcessingService.processDocumentAsync(100L);

        verify(textChunkingService, never()).chunkText(any());
        verify(embeddingService, never()).generateEmbeddings(any());
        verify(vectorStoreService, never()).upsert(any(), any());
        verify(documentRepository, atLeastOnce()).saveAndFlush(argThat(d -> d.getStatus() == DocumentStatus.FAILED));
    }

    @Test
    @DisplayName("Should mark FAILED when embedding generation fails and perform cleanup")
    void testEmbeddingFailure() {
        when(documentRepository.findById(100L)).thenReturn(Optional.of(testDocument));
        when(fileStorageService.getRootLocation()).thenReturn(Paths.get("uploads"));
        when(textExtractionService.extractTextFromFile(any(Path.class))).thenReturn("Text");
        when(textCleaningService.cleanText("Text")).thenReturn("Text");
        when(textChunkingService.chunkText("Text")).thenReturn(List.of(new TextChunk(0, "Text", 0, 4)));
        when(embeddingService.generateEmbeddings(any())).thenThrow(new EmbeddingException("Embedding quota exceeded"));

        documentProcessingService.processDocumentAsync(100L);

        verify(vectorStoreService, never()).upsert(any(), any());
        verify(vectorStoreService).deleteByDocumentId(10L, 100L);
        verify(documentRepository, atLeastOnce()).saveAndFlush(argThat(d -> d.getStatus() == DocumentStatus.FAILED));
    }

    @Test
    @DisplayName("Should mark FAILED when Pinecone upsert fails and trigger cleanup")
    void testPineconeFailure() {
        when(documentRepository.findById(100L)).thenReturn(Optional.of(testDocument));
        when(fileStorageService.getRootLocation()).thenReturn(Paths.get("uploads"));
        when(textExtractionService.extractTextFromFile(any(Path.class))).thenReturn("Text");
        when(textCleaningService.cleanText("Text")).thenReturn("Text");
        when(textChunkingService.chunkText("Text")).thenReturn(List.of(new TextChunk(0, "Text", 0, 4)));
        when(embeddingService.generateEmbeddings(any())).thenReturn(List.of(Collections.nCopies(768, 0.1f)));
        when(vectorStoreService.generateVectorId(10L, 100L, 0)).thenReturn("ws_10_doc_100_chk_0");

        doThrow(new VectorStoreException("Pinecone network timeout")).when(vectorStoreService).upsert(eq(10L), any());

        documentProcessingService.processDocumentAsync(100L);

        verify(vectorStoreService).deleteByDocumentId(10L, 100L);
        verify(documentRepository, atLeastOnce()).saveAndFlush(argThat(d -> d.getStatus() == DocumentStatus.FAILED));
    }
}

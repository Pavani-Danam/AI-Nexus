package com.ainexus.service;

import com.ainexus.dto.SearchResponse;
import com.ainexus.entity.User;
import com.ainexus.entity.Workspace;
import com.ainexus.entity.WorkspaceMember;
import com.ainexus.model.vector.VectorQueryResult;
import com.ainexus.repository.WorkspaceMemberRepository;
import com.ainexus.repository.WorkspaceRepository;
import com.ainexus.service.impl.SemanticSearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SemanticSearchServiceImplTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private VectorStoreService vectorStoreService;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @InjectMocks
    private SemanticSearchServiceImpl semanticSearchService;

    private User testUser;
    private Workspace testWorkspace;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(semanticSearchService, "defaultTopK", 5);
        ReflectionTestUtils.setField(semanticSearchService, "maxTopK", 20);
        ReflectionTestUtils.setField(semanticSearchService, "minScore", 0.0);
        ReflectionTestUtils.setField(semanticSearchService, "maxQueryLength", 1000);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testWorkspace = new Workspace();
        testWorkspace.setId(10L);
        testWorkspace.setName("AI Research");
        testWorkspace.setOwner(testUser);
    }

    @Test
    @DisplayName("Should successfully return ordered search results for valid query")
    void testSuccessfulSearch() {
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(testWorkspace));

        List<Float> dummyVector = Collections.nCopies(768, 0.05f);
        when(embeddingService.generateEmbedding("vector databases")).thenReturn(dummyVector);

        VectorQueryResult r1 = new VectorQueryResult(
                "ws_10_doc_1_chk_0",
                0.89,
                Map.of("documentId", 1L, "originalFilename", "report.pdf", "chunkIndex", 0, "content", "Vector DB overview", "characterCount", 18, "fileType", "pdf")
        );
        VectorQueryResult r2 = new VectorQueryResult(
                "ws_10_doc_1_chk_1",
                0.94,
                Map.of("documentId", 1L, "originalFilename", "report.pdf", "chunkIndex", 1, "content", "Pinecone indexing details", "characterCount", 25, "fileType", "pdf")
        );

        when(vectorStoreService.query(10L, dummyVector, 5)).thenReturn(List.of(r1, r2));

        SearchResponse response = semanticSearchService.search("vector databases", 10L, 5, testUser);

        assertNotNull(response);
        assertEquals("vector databases", response.query());
        assertEquals(10L, response.workspaceId());
        assertEquals(2, response.totalResults());

        // Highest score (0.94) first
        assertEquals(0.94, response.results().get(0).score());
        assertEquals("Pinecone indexing details", response.results().get(0).content());
        assertEquals(0.89, response.results().get(1).score());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for blank query")
    void testBlankQuery() {
        assertThrows(IllegalArgumentException.class, () ->
                semanticSearchService.search("   ", 10L, 5, testUser));
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when user is not member or owner of workspace")
    void testUnauthorizedWorkspaceAccess() {
        User otherOwner = new User();
        otherOwner.setId(99L);
        Workspace privateWorkspace = new Workspace();
        privateWorkspace.setId(20L);
        privateWorkspace.setOwner(otherOwner);

        when(workspaceRepository.findById(20L)).thenReturn(Optional.of(privateWorkspace));
        when(workspaceMemberRepository.findByWorkspaceAndUser(privateWorkspace, testUser)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () ->
                semanticSearchService.search("test", 20L, 5, testUser));
    }

    @Test
    @DisplayName("Should cap topK at maxTopK limit")
    void testTopKCapping() {
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(testWorkspace));

        List<Float> dummyVector = Collections.nCopies(768, 0.05f);
        when(embeddingService.generateEmbedding("test query")).thenReturn(dummyVector);
        when(vectorStoreService.query(eq(10L), eq(dummyVector), eq(20))).thenReturn(Collections.emptyList());

        SearchResponse response = semanticSearchService.search("test query", 10L, 50, testUser);

        assertNotNull(response);
        assertEquals(0, response.totalResults());
        verify(vectorStoreService).query(10L, dummyVector, 20);
    }

    @Test
    @DisplayName("Should return empty list when vector store returns no matches")
    void testEmptySearchResults() {
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(testWorkspace));

        List<Float> dummyVector = Collections.nCopies(768, 0.05f);
        when(embeddingService.generateEmbedding("unknown topic")).thenReturn(dummyVector);
        when(vectorStoreService.query(10L, dummyVector, 5)).thenReturn(Collections.emptyList());

        SearchResponse response = semanticSearchService.search("unknown topic", 10L, 5, testUser);

        assertNotNull(response);
        assertEquals(0, response.totalResults());
        assertTrue(response.results().isEmpty());
    }
}

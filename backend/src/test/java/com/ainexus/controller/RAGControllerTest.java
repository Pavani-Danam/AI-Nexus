package com.ainexus.controller;

import com.ainexus.dto.RAGChunk;
import com.ainexus.dto.RAGQueryRequest;
import com.ainexus.dto.RAGResponse;
import com.ainexus.entity.User;
import com.ainexus.entity.Workspace;
import com.ainexus.repository.WorkspaceMemberRepository;
import com.ainexus.service.RAGGenerationService;
import com.ainexus.service.UserService;
import com.ainexus.service.WorkspaceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RAGControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private RAGGenerationService ragGenerationService;

    @Mock
    private UserService userService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private RAGController ragController;

    private User testUser;
    private Workspace testWorkspace;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ragController).build();
        objectMapper = new ObjectMapper();

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testWorkspace = new Workspace();
        testWorkspace.setId(10L);
        testWorkspace.setName("Dev Workspace");
        testWorkspace.setOwner(testUser);
    }

    @Test
    @DisplayName("TEST 1: Authenticated valid query returns 200 OK and grounded answer")
    void testValidQueryReturns200() throws Exception {
        RAGQueryRequest request = new RAGQueryRequest("What is the architecture?", 10L, 5);
        RAGChunk chunk = new RAGChunk(100L, "doc.pdf", 0, 0.95, "Architecture details", 20);
        RAGResponse ragResponse = new RAGResponse("This is the architecture.", "What is the architecture?", 10L, List.of(chunk), true);

        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(workspaceService.getWorkspaceById(10L)).thenReturn(Optional.of(testWorkspace));
        when(ragGenerationService.generateAnswer(eq("What is the architecture?"), eq(10L), eq(5), any(User.class)))
                .thenReturn(ragResponse);

        mockMvc.perform(post("/api/v1/rag/query")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("This is the architecture."))
                .andExpect(jsonPath("$.query").value("What is the architecture?"))
                .andExpect(jsonPath("$.workspaceId").value(10))
                .andExpect(jsonPath("$.hasContext").value(true));
    }

    @Test
    @DisplayName("TEST 2: Empty or blank query returns 400 Bad Request")
    void testBlankQueryReturns400() throws Exception {
        RAGQueryRequest blankRequest = new RAGQueryRequest("", 10L, 5);

        mockMvc.perform(post("/api/v1/rag/query")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 3: Unauthorized workspace access throws AccessDeniedException")
    void testUnauthorizedWorkspaceAccess() {
        User otherOwner = new User();
        otherOwner.setId(99L);
        otherOwner.setUsername("other");

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setId(20L);
        otherWorkspace.setOwner(otherOwner);

        RAGQueryRequest request = new RAGQueryRequest("Secret info", 20L, 5);

        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(workspaceService.getWorkspaceById(20L)).thenReturn(Optional.of(otherWorkspace));
        when(workspaceMemberRepository.findByWorkspaceAndUser(otherWorkspace, testUser)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () ->
                mockMvc.perform(post("/api/v1/rag/query")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))));
    }

    @Test
    @DisplayName("TEST 4: Omitted workspaceId resolves default user workspace automatically")
    void testDefaultWorkspaceResolution() throws Exception {
        RAGQueryRequest request = new RAGQueryRequest("Tell me about policies", null, 5);
        RAGResponse ragResponse = new RAGResponse("Company policy info.", "Tell me about policies", 10L, Collections.emptyList(), false);

        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(workspaceService.getWorkspacesByOwner(testUser)).thenReturn(List.of(testWorkspace));
        when(ragGenerationService.generateAnswer(eq("Tell me about policies"), eq(10L), eq(5), any(User.class)))
                .thenReturn(ragResponse);

        mockMvc.perform(post("/api/v1/rag/query")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Company policy info."))
                .andExpect(jsonPath("$.workspaceId").value(10));
    }
}

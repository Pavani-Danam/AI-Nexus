package com.ainexus.controller;

import com.ainexus.dto.SearchResponse;
import com.ainexus.entity.User;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.service.SemanticSearchService;
import com.ainexus.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/search", "/api/search"})
public class SearchController {

    private final SemanticSearchService semanticSearchService;
    private final UserService userService;

    public SearchController(SemanticSearchService semanticSearchService, UserService userService) {
        this.semanticSearchService = semanticSearchService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @RequestParam("q") String query,
            @RequestParam("workspaceId") Long workspaceId,
            @RequestParam(value = "topK", required = false) Integer topK,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        SearchResponse response = semanticSearchService.search(query, workspaceId, topK, user);
        return ResponseEntity.ok(response);
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("Authentication required");
        }
        return userService.getUserByUsername(authentication.getName())
                .or(() -> userService.getUserByEmail(authentication.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}

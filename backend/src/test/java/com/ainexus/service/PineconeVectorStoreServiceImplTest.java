package com.ainexus.service;

import com.ainexus.exception.VectorStoreException;
import com.ainexus.model.vector.VectorRecord;
import com.ainexus.service.impl.PineconeVectorStoreServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PineconeVectorStoreServiceImplTest {

    @Test
    @DisplayName("Should generate deterministic vector IDs")
    void testDeterministicVectorIdGeneration() {
        PineconeVectorStoreServiceImpl service = new PineconeVectorStoreServiceImpl("test-key", "https://test.pinecone.io", 768, 5, 15);
        String vectorId = service.generateVectorId(101L, 202L, 3);
        assertEquals("ws_101_doc_202_chk_3", vectorId);
    }

    @Test
    @DisplayName("Should reject null components in vector ID generation")
    void testNullComponentsVectorIdGeneration() {
        PineconeVectorStoreServiceImpl service = new PineconeVectorStoreServiceImpl("test-key", "https://test.pinecone.io", 768, 5, 15);
        assertThrows(IllegalArgumentException.class, () -> service.generateVectorId(null, 1L, 0));
        assertThrows(IllegalArgumentException.class, () -> service.generateVectorId(1L, null, 0));
        assertThrows(IllegalArgumentException.class, () -> service.generateVectorId(1L, 1L, null));
    }

    @Test
    @DisplayName("Should throw VectorStoreException when API key is missing")
    void testMissingApiKeyRejection() {
        PineconeVectorStoreServiceImpl service = new PineconeVectorStoreServiceImpl("", "https://test.pinecone.io", 768, 5, 15);
        List<Float> validDimVector = Collections.nCopies(768, 0.1f);
        VectorRecord record = new VectorRecord("v1", validDimVector, Map.of("test", "val"));

        VectorStoreException ex = assertThrows(VectorStoreException.class, () -> service.upsert(1L, List.of(record)));
        assertTrue(ex.getMessage().contains("Pinecone API key is not configured"));
    }

    @Test
    @DisplayName("Should throw VectorStoreException on vector dimension mismatch")
    void testDimensionMismatchRejection() {
        PineconeVectorStoreServiceImpl service = new PineconeVectorStoreServiceImpl("test-key", "https://test.pinecone.io", 768, 5, 15);
        List<Float> smallVector = List.of(0.1f, 0.2f, 0.3f);
        VectorRecord record = new VectorRecord("v1", smallVector, Map.of("chunkIndex", 0));

        VectorStoreException ex = assertThrows(VectorStoreException.class, () -> service.upsert(1L, List.of(record)));
        assertTrue(ex.getMessage().contains("Vector dimension mismatch"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when workspace ID is invalid")
    void testInvalidWorkspaceId() {
        PineconeVectorStoreServiceImpl service = new PineconeVectorStoreServiceImpl("test-key", "https://test.pinecone.io", 768, 5, 15);
        List<Float> vector = Collections.nCopies(768, 0.1f);
        assertThrows(IllegalArgumentException.class, () -> service.upsert(null, List.of(new VectorRecord("v1", vector, Map.of()))));
        assertThrows(IllegalArgumentException.class, () -> service.upsert(-1L, List.of(new VectorRecord("v1", vector, Map.of()))));
    }
}

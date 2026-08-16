package com.ainexus.service;

import com.ainexus.model.vector.VectorQueryResult;
import com.ainexus.model.vector.VectorRecord;

import java.util.List;

public interface VectorStoreService {

    /**
     * Upserts a list of vector records into the vector database isolated by workspace.
     *
     * @param workspaceId The authorized workspace ID.
     * @param records     The vector records containing ID, embeddings, and metadata.
     */
    void upsert(Long workspaceId, List<VectorRecord> records);

    /**
     * Queries the top-K most similar vectors for a query vector within a workspace.
     *
     * @param workspaceId The authorized workspace ID.
     * @param queryVector The query embedding vector.
     * @param topK        The number of results to retrieve.
     * @return List of matching VectorQueryResult objects.
     */
    List<VectorQueryResult> query(Long workspaceId, List<Float> queryVector, int topK);

    /**
     * Deletes all vectors associated with a specific document in a workspace.
     *
     * @param workspaceId The authorized workspace ID.
     * @param documentId  The document ID.
     */
    void deleteByDocumentId(Long workspaceId, Long documentId);

    /**
     * Generates a deterministic, collision-free vector ID.
     *
     * @param workspaceId Workspace ID.
     * @param documentId  Document ID.
     * @param chunkIndex  Chunk index.
     * @return Deterministic string ID.
     */
    default String generateVectorId(Long workspaceId, Long documentId, Integer chunkIndex) {
        if (workspaceId == null || documentId == null || chunkIndex == null) {
            throw new IllegalArgumentException("workspaceId, documentId, and chunkIndex must not be null");
        }
        return String.format("ws_%d_doc_%d_chk_%d", workspaceId, documentId, chunkIndex);
    }
}

package com.ainexus.service;

import java.util.List;

public interface EmbeddingService {

    /**
     * Generates a numerical vector embedding for the provided text.
     *
     * @param text The input text string to embed.
     * @return List of Float values representing the vector embedding.
     */
    List<Float> generateEmbedding(String text);

    /**
     * Generates numerical vector embeddings for a list of text chunks.
     *
     * @param texts The list of input texts.
     * @return List of vector embeddings corresponding to each input text.
     */
    List<List<Float>> generateEmbeddings(List<String> texts);
}

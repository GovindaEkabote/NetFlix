package ai_search_service.service;


import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;


/*
* converting text into vector embedding
* 1. Accept scene text or a user query.
* 2. Generate an embedding using the configured EmbeddingModel.
* 3. Return the embedding as a float[].
* */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public float[] generateEmbedding(String text) {
        try {
            return embeddingModel.embed(text);
        } catch (Exception e) {
            log.error("Failed to generate embedding", e);
            throw new RuntimeException("Unable to generate embedding.", e);
        }
    }

    public List<float[]> generateEmbeddings(List<String> texts) {
        return texts.stream()
                .map(this::generateEmbedding)
                .toList();
    }
}

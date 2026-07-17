package ai_search_service.service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public float[] generateEmbedding(String text) {
        try {
            if (text == null || text.trim().isEmpty()) {
                log.warn("Empty text provided for embedding generation");
                return new float[0];
            }

            log.debug("Generating embedding for text: {}",
                    text.length() > 50 ? text.substring(0, 50) + "..." : text);

            return embeddingModel.embed(text);

        } catch (Exception e) {
            log.error("Failed to generate embedding for text: {}", text, e);
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }

    public List<float[]> generateEmbeddings(List<String> texts) {
        List<float[]> embeddings = new ArrayList<>();
        for (String text : texts) {
            embeddings.add(generateEmbedding(text));
        }
        return embeddings;
    }
}

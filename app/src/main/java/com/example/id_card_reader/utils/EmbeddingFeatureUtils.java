package com.example.id_card_reader.utils;

public class EmbeddingFeatureUtils {
    public float calculateCosineSimilarity(float[] embedding1, float[] embedding2) {
        // Implement your similarity calculation here (e.g., cosine similarity)
        if (embedding1 == null || embedding2 == null || embedding1.length != embedding2.length) {
            return 0.0f; // Or handle error appropriately
        }
        float dotProduct = 0;
        float norm1 = 0;
        float norm2 = 0;
        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            norm1 += embedding1[i] * embedding1[i];
            norm2 += embedding2[i] * embedding2[i];
        }
        return (float) (dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2)));
    }
    public static float calculateDistance(float[] embedding1, float[] embedding2) {
        if (embedding1 == null || embedding2 == null || embedding1.length != embedding2.length) {
            return -1.0f; // Or handle error appropriately
        }
        float distance = 0;
        for (int i = 0; i < embedding1.length; i++) {
            distance += (embedding1[i] - embedding2[i]) * (embedding1[i] - embedding2[i]);
        }
        return (float) Math.sqrt(distance);
    }
}

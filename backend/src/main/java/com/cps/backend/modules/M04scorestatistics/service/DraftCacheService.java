package com.cps.backend.modules.M04scorestatistics.service;

import com.cps.backend.modules.M04scorestatistics.dto.AnswerItem;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DraftCacheService {

    // Key format: "draft:{examId}:{userId}"
    private final Map<String, DraftEntry> cache = new ConcurrentHashMap<>();
    
    // TTL: 2 hours
    private static final long TTL_MILLIS = 2 * 60 * 60 * 1000;

    private String buildKey(Integer examId, Integer userId) {
        return "draft:" + examId + ":" + userId;
    }

    public void saveDraft(Integer examId, Integer userId, List<AnswerItem> answers) {
        String key = buildKey(examId, userId);
        cache.put(key, new DraftEntry(answers, Instant.now().toEpochMilli()));
    }

    public List<AnswerItem> loadDraft(Integer examId, Integer userId) {
        String key = buildKey(examId, userId);
        DraftEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        // Check TTL
        if (Instant.now().toEpochMilli() - entry.timestamp() > TTL_MILLIS) {
            cache.remove(key);
            return null;
        }
        return entry.answers();
    }

    public void clearDraft(Integer examId, Integer userId) {
        cache.remove(buildKey(examId, userId));
    }

    private record DraftEntry(List<AnswerItem> answers, long timestamp) {}
}

package com.github.dimitryivaniuta.gateway.statement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dimitryivaniuta.gateway.statement.model.StatementPayload;
import org.springframework.stereotype.Service;

/**
 * Serializes statement payloads into canonical JSON text.
 */
@Service
public class CanonicalJsonStatementSerializer {

    private final ObjectMapper objectMapper;

    /**
     * Creates the serializer.
     *
     * @param objectMapper shared object mapper.
     */
    public CanonicalJsonStatementSerializer(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Serializes the payload into canonical JSON.
     *
     * @param payload statement payload.
     * @return canonical JSON text.
     */
    public String serialize(final StatementPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize statement payload", exception);
        }
    }
}

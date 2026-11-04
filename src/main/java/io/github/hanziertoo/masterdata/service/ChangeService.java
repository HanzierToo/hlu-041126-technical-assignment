package io.github.hanziertoo.masterdata.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.hanziertoo.masterdata.model.ChangeRecord;
import io.github.hanziertoo.masterdata.repository.ChangeRepository;

import org.springframework.stereotype.Service;

@Service
public class ChangeService {

    private final ChangeRepository changes;
    private final ObjectMapper objectMapper;

    public ChangeService(ChangeRepository changes, ObjectMapper objectMapper) {
        this.changes = changes;
        this.objectMapper = objectMapper;
    }

    public void record(String actor, String entity, Long id, String action, Object before, Object after) {
        changes.save(new ChangeRecord(actor, entity, id, action, serializeSnapshot(before), serializeSnapshot(after)));
    }

    private String serializeSnapshot(Object value) {
        if (value == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize change record", ex);
        }
    }
}

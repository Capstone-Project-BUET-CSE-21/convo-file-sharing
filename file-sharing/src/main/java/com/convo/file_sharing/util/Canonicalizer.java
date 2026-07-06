package com.convo.file_sharing.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Section 0.1 / 0.2 shared contract.
 *
 * This MUST produce byte-identical output to the frontend's canonicalize.js
 * for the same provenance block, or hash/signature verification will fail
 * silently (same input data, different bytes hashed on each side).
 *
 * Rules from the manual:
 *  - Keys in this exact order: fileName, fileSize, mimeType, previousHash,
 *    senderId, sessionId, timestamp, transferId (this happens to be plain
 *    alphabetical order — do not "fix" it if a new field breaks alpha order,
 *    the frontend's actual sort order is the source of truth, not the label).
 *  - No whitespace in the output JSON.
 *  - previousHash is emitted as JSON null (not omitted, not "null" string)
 *    when there is no previous transfer.
 *
 * If anyone changes this, they must change canonicalize.js identically and
 * re-run the cross-language test vector in CanonicalizeTest.
 */
public final class Canonicalizer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Canonicalizer() {}

    public static String canonicalize(ProvenanceBlock block) {
        // LinkedHashMap to pin the exact field order from section 0.2 —
        // do not switch this to a TreeMap/sorted map "for simplicity";
        // if a future field is added that doesn't sort alphabetically,
        // a sorted map would silently produce the wrong order.
        Map<String, Object> ordered = new LinkedHashMap<>();
        ordered.put("fileName", block.fileName());
        ordered.put("fileSize", block.fileSize());
        ordered.put("mimeType", block.mimeType());
        ordered.put("previousHash", block.previousHash()); // may be null
        ordered.put("senderId", block.senderId());
        ordered.put("sessionId", block.sessionId());
        ordered.put("timestamp", block.timestamp());
        ordered.put("transferId", block.transferId());

        try {
            return MAPPER.writeValueAsString(ordered);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to canonicalize provenance block", e);
        }
    }
}

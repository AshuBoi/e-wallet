package com.ashuboi.idempotency;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class IdempotencyHasherTest {

    @Test
    void sameBodyProducesSameHash() {
        Map<String, Object> a = Map.of("toUserId", 2, "amount", "10.00");
        Map<String, Object> b = Map.of("toUserId", 2, "amount", "10.00");
        assertEquals(IdempotencyHasher.hashRequest(a), IdempotencyHasher.hashRequest(b));
    }

    @Test
    void differentBodyProducesDifferentHash() {
        Map<String, Object> a = Map.of("toUserId", 2, "amount", "10.00");
        Map<String, Object> b = Map.of("toUserId", 2, "amount", "11.00");
        assertNotEquals(IdempotencyHasher.hashRequest(a), IdempotencyHasher.hashRequest(b));
    }
}

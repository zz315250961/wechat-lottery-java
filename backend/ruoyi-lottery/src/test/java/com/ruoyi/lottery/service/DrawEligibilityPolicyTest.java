package com.ruoyi.lottery.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DrawEligibilityPolicyTest {
    private final DrawEligibilityPolicy policy = new DrawEligibilityPolicy();

    @Test
    void remainingChancesIncludesCheckInAndAssists() {
        assertEquals(2, policy.remainingChances(1, 1, true, 2, 2));
    }

    @Test
    void remainingChancesNeverBecomesNegative() {
        assertEquals(0, policy.remainingChances(1, 0, false, 0, 3));
    }

    @Test
    void rejectsInvalidAssistInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> policy.remainingChances(1, 1, true, -1, 0));
    }
}

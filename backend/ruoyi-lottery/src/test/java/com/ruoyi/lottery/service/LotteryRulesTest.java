package com.ruoyi.lottery.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class LotteryRulesTest {
    private final LotteryRules rules = new LotteryRules();

    @Test
    void activityMustBeEnabledAndInsideConfiguredWindow() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 10, 12, 0);
        assertTrue(rules.isActivityOpen(true, now.minusHours(1), now.plusHours(1), now));
        assertFalse(rules.isActivityOpen(false, null, null, now));
        assertFalse(rules.isActivityOpen(true, now.plusSeconds(1), null, now));
        assertFalse(rules.isActivityOpen(true, null, now.minusSeconds(1), now));
    }

    @Test
    void probabilityTotalCannotExceedOneHundredPercent() {
        assertDoesNotThrow(() -> rules.validateProbabilityTotal(Arrays.asList(
                new BigDecimal("20.0000"), new BigDecimal("80.0000"))));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> rules.validateProbabilityTotal(Arrays.asList(
                        new BigDecimal("70.0000"), new BigDecimal("30.0001"))));
        assertEquals("启用奖品概率总和不能超过100%", error.getMessage());
    }

    @Test
    void ticketExpiryUsesFixedDeadlineOrDefaultsToThirtyDays() {
        LocalDateTime issued = LocalDateTime.of(2026, 9, 10, 12, 0);
        assertEquals(issued.plusDays(30), rules.ticketExpiresAt(issued, 30, null));
        assertEquals(issued.plusDays(3), rules.ticketExpiresAt(issued, 7, issued.plusDays(3)));
        assertEquals(issued.plusDays(90), rules.ticketExpiresAt(issued, 30, issued.plusDays(90)));
    }
}

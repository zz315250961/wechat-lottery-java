package com.ruoyi.lottery.service;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class TicketRedemptionPolicyTest {
    private final TicketRedemptionPolicy policy = new TicketRedemptionPolicy();
    private final LocalDateTime now = LocalDateTime.of(2026, 9, 10, 12, 0);

    @Test void allowsPendingTicketOwnedByMerchantBeforeExpiry() {
        assertDoesNotThrow(() -> policy.assertRedeemable("PENDING", 8L, 8L, now.plusMinutes(1), now));
    }

    @Test void rejectsTicketOwnedByAnotherMerchant() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> policy.assertRedeemable("PENDING", 8L, 9L, now.plusDays(1), now));
        assertEquals("该奖券不属于本店", error.getMessage());
    }

    @Test void rejectsRedeemedAndExpiredTickets() {
        assertEquals("该奖券已经核销", assertThrows(IllegalStateException.class,
            () -> policy.assertRedeemable("REDEEMED", 8L, 8L, now.plusDays(1), now)).getMessage());
        assertEquals("该奖券已过期", assertThrows(IllegalStateException.class,
            () -> policy.assertRedeemable("PENDING", 8L, 8L, now.minusSeconds(1), now)).getMessage());
    }
}

package com.ruoyi.lottery.service;

import java.time.LocalDateTime;

public class TicketRedemptionPolicy {
    public void assertRedeemable(String status, long ticketMerchantId, long currentMerchantId,
                                 LocalDateTime expiresAt, LocalDateTime now) {
        if (ticketMerchantId != currentMerchantId) throw new IllegalArgumentException("该奖券不属于本店");
        if ("REDEEMED".equals(status)) throw new IllegalStateException("该奖券已经核销");
        if (!"PENDING".equals(status)) throw new IllegalStateException("奖券状态异常");
        if (expiresAt == null || expiresAt.isBefore(now)) throw new IllegalStateException("该奖券已过期");
    }
}

package com.ruoyi.lottery.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;

/** 不依赖数据库的抽奖业务规则。 */
public class LotteryRules {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.0000");

    public boolean isActivityOpen(boolean enabled, LocalDateTime startAt,
                                  LocalDateTime endAt, LocalDateTime now) {
        if (!enabled) return false;
        if (startAt != null && now.isBefore(startAt)) return false;
        return endAt == null || !now.isAfter(endAt);
    }

    public void validateProbabilityTotal(Collection<BigDecimal> probabilities) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal probability : probabilities) {
            if (probability == null || probability.signum() < 0) {
                throw new IllegalArgumentException("奖品概率不能为负数");
            }
            total = total.add(probability);
        }
        if (total.compareTo(ONE_HUNDRED) > 0) {
            throw new IllegalArgumentException("启用奖品概率总和不能超过100%");
        }
    }

    public LocalDateTime ticketExpiresAt(LocalDateTime issuedAt, int validDays,
                                         LocalDateTime fixedDeadline) {
        if (validDays < 1) throw new IllegalArgumentException("奖券有效天数至少为1天");
        return fixedDeadline != null ? fixedDeadline : issuedAt.plusDays(validDays);
    }
}

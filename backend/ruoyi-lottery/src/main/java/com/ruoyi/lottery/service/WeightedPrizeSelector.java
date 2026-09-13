package com.ruoyi.lottery.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** 按后台真实概率选择奖品，未覆盖的概率区间即谢谢参与。 */
public class WeightedPrizeSelector {
    public Optional<Long> select(List<Option> options, double randomZeroToOne) {
        if (randomZeroToOne < 0 || randomZeroToOne >= 1) {
            throw new IllegalArgumentException("随机数必须在[0,1)范围内");
        }
        BigDecimal point = BigDecimal.valueOf(randomZeroToOne).multiply(BigDecimal.valueOf(100));
        BigDecimal cursor = BigDecimal.ZERO;
        for (Option option : options) {
            cursor = cursor.add(option.getProbability());
            if (point.compareTo(cursor) < 0) return Optional.of(option.getPrizeId());
        }
        return Optional.empty();
    }

    public static class Option {
        private final Long prizeId;
        private final BigDecimal probability;
        public Option(Long prizeId, BigDecimal probability) {
            this.prizeId = prizeId;
            this.probability = probability;
        }
        public Long getPrizeId() { return prizeId; }
        public BigDecimal getProbability() { return probability; }
    }
}

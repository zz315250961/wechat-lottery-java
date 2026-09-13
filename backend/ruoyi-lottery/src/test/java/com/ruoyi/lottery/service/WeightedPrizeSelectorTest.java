package com.ruoyi.lottery.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class WeightedPrizeSelectorTest {
    @Test
    void selectsPrizeByConfiguredProbabilityRange() {
        WeightedPrizeSelector selector = new WeightedPrizeSelector();
        WeightedPrizeSelector.Option first = new WeightedPrizeSelector.Option(1L, new BigDecimal("20"));
        WeightedPrizeSelector.Option second = new WeightedPrizeSelector.Option(2L, new BigDecimal("30"));
        assertEquals(Long.valueOf(1), selector.select(Arrays.asList(first, second), 0.199999).orElse(null));
        assertEquals(Long.valueOf(2), selector.select(Arrays.asList(first, second), 0.200001).orElse(null));
        assertFalse(selector.select(Arrays.asList(first, second), 0.9).isPresent());
    }
}

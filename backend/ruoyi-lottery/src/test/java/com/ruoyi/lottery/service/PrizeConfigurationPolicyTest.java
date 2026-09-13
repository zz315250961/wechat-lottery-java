package com.ruoyi.lottery.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class PrizeConfigurationPolicyTest {
    private final PrizeConfigurationPolicy policy=new PrizeConfigurationPolicy();
    @Test void acceptsTotalProbabilityAtMostOneHundred(){assertDoesNotThrow(()->policy.validateTotal(Arrays.asList(new BigDecimal("40"),new BigDecimal("60"))));}
    @Test void rejectsProbabilityAboveOneHundred(){assertEquals("启用奖品概率合计不能超过100%",assertThrows(IllegalArgumentException.class,()->policy.validateTotal(Arrays.asList(new BigDecimal("50.1"),new BigDecimal("50")))).getMessage());}
    @Test void rejectsNegativeStock(){assertEquals("库存不能小于0",assertThrows(IllegalArgumentException.class,()->policy.validatePrize(new BigDecimal("1"),-1,1,1)).getMessage());}
    @Test void rejectsMissingPerUserLimitWithoutNullPointer(){assertEquals("个人上限不能为空",assertThrows(IllegalArgumentException.class,()->policy.validatePrize(new BigDecimal("1"),1,null,30)).getMessage());}
}

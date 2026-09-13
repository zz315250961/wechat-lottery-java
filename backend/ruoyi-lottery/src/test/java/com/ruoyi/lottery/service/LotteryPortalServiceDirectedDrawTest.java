package com.ruoyi.lottery.service;

import com.ruoyi.lottery.mapper.LotteryPortalMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LotteryPortalServiceDirectedDrawTest {
    @Test
    void directedDrawConsumesOneDirectiveAndOneStockAndPersistsItsSource() {
        LotteryPortalMapper mapper = mock(LotteryPortalMapper.class);
        LotteryPortalService service = new LotteryPortalService(mapper);
        Map<String,Object> user = map("id", 101L, "enabled", true);
        Map<String,Object> activity = map("id", 1L, "enabled", true, "dailyFree", 1,
            "checkinReward", 0, "assistReward", 0, "assistDailyLimit", 0, "thanksLabel", "谢谢参与");
        Map<String,Object> usage = map("usedCount", 0, "checkedIn", false, "assistRewardCount", 0);
        Map<String,Object> prize = map("id", 202L, "merchantId", 303L, "name", "定向奖品",
            "imageUrl", "", "probability", BigDecimal.ZERO, "perUserLimit", 2, "validDays", 30, "redeemEndAt", null);
        Map<String,Object> directive = map("id", 404L, "prizeId", 202L);
        Map<String,Object> merchant = map("id", 303L, "name", "测试门店", "address", "一层", "usageRules", "到店使用");
        when(mapper.userById(101L)).thenReturn(user);
        when(mapper.drawByRequest(101L, "directed-001")).thenReturn(null);
        when(mapper.lockActivity(1L)).thenReturn(activity);
        when(mapper.usage(eq(1L), eq(101L), any(LocalDate.class))).thenReturn(usage);
        when(mapper.drawablePrizes(1L)).thenReturn(Collections.singletonList(prize));
        when(mapper.userPrizeWins(101L, 202L)).thenReturn(0);
        when(mapper.nextDirective(1L, 101L)).thenReturn(directive);
        when(mapper.consumeDirective(404L)).thenReturn(1);
        when(mapper.decrementStock(202L)).thenReturn(1);
        when(mapper.merchant(303L)).thenReturn(merchant);

        service.draw(101L, "directed-001");

        verify(mapper, times(1)).consumeDirective(404L);
        verify(mapper, times(1)).decrementStock(202L);
        verify(mapper).insertDraw(anyLong(), eq(1L), eq(101L), eq("directed-001"), eq(202L),
            eq("WON"), eq("DIRECTED"), eq(404L), eq("定向奖品"));
    }

    private static Map<String,Object> map(Object... values) {
        Map<String,Object> result = new HashMap<>();
        for (int i=0;i<values.length;i+=2) result.put((String) values[i], values[i+1]);
        return result;
    }
}

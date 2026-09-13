package com.ruoyi.lottery.service;

import com.ruoyi.lottery.mapper.LotteryPortalMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LotteryMerchantServiceTest {
    @Test
    void ticketRecordsReturnFilteredRowsAndStoreWideStatistics() {
        LotteryPortalMapper mapper = mock(LotteryPortalMapper.class);
        LotteryMerchantService service = new LotteryMerchantService(mapper);
        Map<String,Object> merchant = new HashMap<>();
        merchant.put("id", 9111L);
        merchant.put("enabled", true);
        Map<String,Object> ticket = new HashMap<>();
        ticket.put("ticketNo", "ABC123");
        Map<String,Object> statistics = new HashMap<>();
        statistics.put("total", 8L);
        statistics.put("pending", 4L);
        statistics.put("redeemed", 3L);
        statistics.put("expired", 1L);
        when(mapper.merchantBySysUserId(101L)).thenReturn(merchant);
        when(mapper.merchantTickets(9111L, "咖啡", "PENDING")).thenReturn(Collections.singletonList(ticket));
        when(mapper.merchantTicketStatistics(9111L)).thenReturn(statistics);

        Map<String,Object> result = service.ticketRecords(101L, " 咖啡 ", "pending");

        assertEquals(Collections.singletonList(ticket), result.get("records"));
        assertEquals(statistics, result.get("statistics"));
    }

    @Test
    void ticketRecordsRejectUnknownStatusFilter() {
        LotteryPortalMapper mapper = mock(LotteryPortalMapper.class);
        LotteryMerchantService service = new LotteryMerchantService(mapper);
        Map<String,Object> merchant = new HashMap<>();
        merchant.put("id", 9111L);
        merchant.put("enabled", true);
        when(mapper.merchantBySysUserId(101L)).thenReturn(merchant);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> service.ticketRecords(101L, "", "deleted"));

        assertEquals("无效的奖券状态筛选", error.getMessage());
    }

    @Test
    void disabledMerchantCannotOpenTheWorkbench() {
        LotteryPortalMapper mapper = mock(LotteryPortalMapper.class);
        LotteryMerchantService service = new LotteryMerchantService(mapper);
        Map<String,Object> merchant = new HashMap<>();
        merchant.put("id", 9111L);
        merchant.put("enabled", false);
        when(mapper.merchantBySysUserId(101L)).thenReturn(merchant);

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> service.profile(101L));

        assertEquals("当前账号未绑定可用商户", error.getMessage());
    }

    @Test
    void profileReturnsOnlyTheMerchantBoundToTheLoggedInAccount() {
        LotteryPortalMapper mapper = mock(LotteryPortalMapper.class);
        LotteryMerchantService service = new LotteryMerchantService(mapper);
        Map<String,Object> merchant = new HashMap<>();
        merchant.put("id", 9111L);
        merchant.put("name", "校园咖啡店");
        merchant.put("address", "图书馆一层");
        merchant.put("usageRules", "到店核销");
        merchant.put("enabled", true);
        when(mapper.merchantBySysUserId(101L)).thenReturn(merchant);

        Map<String,Object> profile = service.profile(101L);

        assertEquals("校园咖啡店", profile.get("name"));
        assertEquals("图书馆一层", profile.get("address"));
        assertEquals("到店核销", profile.get("usageRules"));
        assertFalse(profile.containsKey("id"));
        verify(mapper).merchantBySysUserId(101L);
    }
}

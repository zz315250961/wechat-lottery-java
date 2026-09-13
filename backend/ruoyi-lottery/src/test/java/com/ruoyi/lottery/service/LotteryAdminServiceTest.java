package com.ruoyi.lottery.service;

import com.ruoyi.lottery.domain.LotteryRuleInput;
import com.ruoyi.lottery.mapper.LotteryActivityMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LotteryAdminServiceTest {
    @Test
    void overviewSerializesPrizeSnowflakeIdsAsExactStrings() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        LotteryActivityMapper activities = mock(LotteryActivityMapper.class);
        LotteryAdminService service = new LotteryAdminService(activities, null, jdbc);
        Map<String,Object> prize = new HashMap<>();
        prize.put("id", 2098702487202197505L);
        prize.put("activity_id", 1L);
        prize.put("merchant_id", 2098702487202197506L);
        prize.put("enabled", true);
        when(jdbc.queryForList(anyString())).thenReturn(
            Collections.singletonList(prize),
            Collections.emptyList(),
            Collections.emptyList()
        );

        Map<String,Object> result = service.overview();
        Map<String,Object> serialized = ((java.util.List<Map<String,Object>>) result.get("prizes")).get(0);

        assertEquals("2098702487202197505", serialized.get("id"));
        assertEquals("1", serialized.get("activityId"));
        assertEquals("2098702487202197506", serialized.get("merchantId"));
    }

    @Test
    void overviewSerializesMerchantSnowflakeIdsAsExactStrings() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        LotteryActivityMapper activities = mock(LotteryActivityMapper.class);
        LotteryAdminService service = new LotteryAdminService(activities, null, jdbc);
        Map<String,Object> merchant = new HashMap<>();
        merchant.put("id", 9007199254740993123L);
        merchant.put("sys_user_id", 9007199254740993124L);
        when(jdbc.queryForList(anyString())).thenReturn(Collections.emptyList(), Collections.singletonList(merchant), Collections.emptyList());

        Map<String,Object> result = service.overview();
        Map<String,Object> serialized = ((java.util.List<Map<String,Object>>) result.get("merchants")).get(0);

        assertEquals("9007199254740993123", serialized.get("id"));
        assertEquals("9007199254740993124", serialized.get("sys_user_id"));
    }

    @Test
    void merchantCreateReturnsInsertedIdAndNextSaveUpdatesThatMerchant() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        LotteryAdminService service = new LotteryAdminService(null, null, jdbc);
        when(jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE user_name=?", Integer.class, "coffee")).thenReturn(0);
        Map<String,Object> draft = new HashMap<>();
        draft.put("username", "coffee");
        draft.put("password", "initial-password");
        draft.put("name", "校园咖啡");
        draft.put("address", "图书馆");
        draft.put("usageRules", "到店");
        draft.put("enabled", true);

        Map<String,Object> created = service.saveMerchant(draft);
        assertEquals(Collections.singleton("id"), created.keySet(), "response must never echo a password or account credentials");
        assertTrue(created.get("id") instanceof String, "Snowflake identity must stay exact in JavaScript");
        long id = Long.parseLong((String) created.get("id"));
        verify(jdbc).update(startsWith("INSERT INTO lottery_merchant"), eq(id), anyLong(), eq("校园咖啡"), eq("图书馆"), eq("到店"), eq(1));

        clearInvocations(jdbc);
        when(jdbc.queryForMap("SELECT sys_user_id FROM lottery_merchant WHERE id=?", id)).thenReturn(Collections.<String,Object>singletonMap("sys_user_id", 100L));
        when(jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE user_name=? AND user_id<>?", Integer.class, "coffee", 100L)).thenReturn(0);
        draft.put("id", created.get("id"));
        draft.put("password", "");
        draft.put("address", "新地址");
        assertEquals(created, service.saveMerchant(draft));
        verify(jdbc).update("INSERT IGNORE INTO sys_user_role(user_id,role_id) VALUES(?,3)", 100L);
        verify(jdbc).queryForMap("SELECT sys_user_id FROM lottery_merchant WHERE id=?", id);
        verify(jdbc).update("UPDATE lottery_merchant SET name=?,address=?,usage_rules=?,enabled=? WHERE id=?", "校园咖啡", "新地址", "到店", 1, id);
        verify(jdbc).queryForObject("SELECT COUNT(*) FROM sys_user WHERE user_name=? AND user_id<>?", Integer.class, "coffee", 100L);
        verify(jdbc).update("UPDATE sys_user SET user_name=?,nick_name=?,status=?,update_time=NOW() WHERE user_id=?", "coffee", "校园咖啡", "0", 100L);
        verifyNoMoreInteractions(jdbc);
    }

    @Test
    void ruleSaveKeepsDisabledDraftAndItsExplicitSortOrder() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        LotteryAdminService service = new LotteryAdminService(null, null, jdbc);
        LotteryRuleInput disabled = new LotteryRuleInput(9L, "保留的停用草稿", false, 7);
        LotteryRuleInput enabled = new LotteryRuleInput(null, "用户可见规则", true, 8);

        service.replaceRules(Arrays.asList(disabled, enabled));

        verify(jdbc).update("DELETE FROM lottery_rule WHERE activity_id=1");
        verify(jdbc).update(startsWith("INSERT INTO lottery_rule"), eq(9L), eq("保留的停用草稿"), eq(0), eq(7));
        verify(jdbc).update(startsWith("INSERT INTO lottery_rule"), anyLong(), eq("用户可见规则"), eq(1), eq(8));
    }

    @Test
    void directivesIncludeCompletedConfigurationsAndSerializeIds() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        LotteryAdminService service = new LotteryAdminService(null, null, jdbc);
        Map<String,Object> completed = new HashMap<>();
        completed.put("id", 9007199254740993123L);
        completed.put("user_id", 9007199254740993124L);
        completed.put("prize_id", 9007199254740993125L);
        completed.put("remaining_count", 0);
        when(jdbc.queryForList(anyString())).thenReturn(Collections.singletonList(completed));

        java.util.List<Map<String,Object>> rows = service.directives();

        assertEquals("9007199254740993123", rows.get(0).get("id"));
        assertEquals("9007199254740993124", rows.get(0).get("user_id"));
        assertEquals("9007199254740993125", rows.get(0).get("prize_id"));
        verify(jdbc).queryForList(argThat(sql ->
            sql.contains("d.remaining_count=0") &&
            sql.contains("source_type='DIRECTED'") &&
            !sql.contains("WHERE d.remaining_count>0")));
    }

    @Test
    void addDirectivePersistsInitialAndRemainingCounts() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        LotteryAdminService service = new LotteryAdminService(null, null, jdbc);
        when(jdbc.queryForObject("SELECT COUNT(*) FROM lottery_user WHERE id=?", Integer.class, 101L)).thenReturn(1);
        when(jdbc.queryForObject("SELECT COUNT(*) FROM lottery_prize WHERE id=? AND deleted=0", Integer.class, 202L)).thenReturn(1);

        service.addDirective(101L, 202L, 3);

        verify(jdbc).update(startsWith("INSERT INTO lottery_directive(id,activity_id,user_id,prize_id,initial_count,remaining_count)"),
            anyLong(), eq(101L), eq(202L), eq(3), eq(3));
    }

    @Test
    void directiveWithRecordedWinsCannotBeDeleted() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        LotteryAdminService service = new LotteryAdminService(null, null, jdbc);
        when(jdbc.queryForObject("SELECT COUNT(*) FROM lottery_draw WHERE directive_id=?", Integer.class, 404L)).thenReturn(1);

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.deleteDirective(404L));

        assertEquals("已有中奖记录的定向配置必须保留", error.getMessage());
        verify(jdbc, never()).update("DELETE FROM lottery_directive WHERE id=?", 404L);
    }

    @Test
    void ticketRecordsExposeDrawSource() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        LotteryAdminService service = new LotteryAdminService(null, null, jdbc);
        when(jdbc.queryForList(anyString(), any(), any(), any(), any())).thenReturn(Collections.emptyList());

        service.tickets("");

        verify(jdbc).queryForList(argThat(sql -> sql.contains("d.source_type") && sql.contains("JOIN lottery_draw d")),
            eq(""), eq(""), eq(""), eq(""));
    }
}

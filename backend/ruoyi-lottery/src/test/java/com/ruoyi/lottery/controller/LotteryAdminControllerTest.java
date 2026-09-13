package com.ruoyi.lottery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.lottery.domain.LotteryRuleInput;
import com.ruoyi.lottery.service.LotteryAdminService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LotteryAdminControllerTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void merchantEndpointAddsExactPersistedIdentityAndPreservesSuccessEnvelope() throws Exception {
        LotteryAdminService service = mock(LotteryAdminService.class);
        LotteryAdminController controller = new LotteryAdminController(service);
        Map<String,Object> request = Collections.<String,Object>singletonMap("name", "校园咖啡");
        when(service.saveMerchant(request)).thenReturn(Collections.<String,Object>singletonMap("id", "9007199254740993123"));
        com.fasterxml.jackson.databind.JsonNode response = json.readTree(json.writeValueAsString(controller.merchant(request)));
        assertEquals(200, response.get("code").asInt());
        assertEquals("商户资料已保存", response.get("msg").asText());
        assertEquals("9007199254740993123", response.get("data").get("id").asText());
        assertTrue(response.get("data").get("id").isTextual());
        assertEquals(1, response.get("data").size());
        verify(service, times(1)).saveMerchant(request);
    }

    @Test
    @SuppressWarnings("unchecked")
    void rulesEndpointKeepsDisabledRuleDtosAndAcceptsLegacyStringArrays() throws Exception {
        LotteryAdminService service = mock(LotteryAdminService.class);
        LotteryAdminController controller = new LotteryAdminController(service);

        controller.rules(json.readTree("[{\"id\":9,\"content\":\"停用草稿\",\"enabled\":false,\"sortOrder\":4},\"旧版规则\"]"));

        ArgumentCaptor<List<LotteryRuleInput>> captured = ArgumentCaptor.forClass(List.class);
        verify(service).replaceRules(captured.capture());
        List<LotteryRuleInput> rules = captured.getValue();
        assertEquals(2, rules.size());
        assertEquals(9L, rules.get(0).getId());
        assertFalse(rules.get(0).getEnabled());
        assertEquals(4, rules.get(0).getSortOrder());
        assertEquals("旧版规则", rules.get(1).getContent());
        assertTrue(rules.get(1).getEnabled());
        assertEquals(1, rules.get(1).getSortOrder());
    }
}

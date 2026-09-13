package com.ruoyi.lottery.service;

import com.ruoyi.lottery.mapper.LotteryPortalMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LotteryPortalServiceAccessTest {
    @Test
    void disabledParticipantCannotCreateANewSession() {
        LotteryPortalMapper mapper = mock(LotteryPortalMapper.class);
        LotteryPortalService service = new LotteryPortalService(mapper);
        Map<String,Object> user = new HashMap<>();
        user.put("id", 2301L);
        user.put("enabled", false);
        when(mapper.userByPhone("15519762301")).thenReturn(user);

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> service.findOrCreateTestUser("15519762301"));

        assertEquals("账号已停用，请联系管理员", error.getMessage());
    }
}

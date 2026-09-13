package com.ruoyi.lottery.service;

import com.ruoyi.lottery.mapper.LotteryPortalMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class LotteryMerchantService {
    private final LotteryPortalMapper mapper;
    private final TicketRedemptionPolicy policy = new TicketRedemptionPolicy();

    public LotteryMerchantService(LotteryPortalMapper mapper) { this.mapper = mapper; }

    public Map<String,Object> ticketRecords(long sysUserId, String keyword, String status) {
        Map<String,Object> merchant = requiredMerchant(sysUserId);
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!normalizedStatus.isEmpty() && !normalizedStatus.equals("PENDING") && !normalizedStatus.equals("REDEEMED") && !normalizedStatus.equals("EXPIRED"))
            throw new IllegalArgumentException("无效的奖券状态筛选");
        long merchantId = number(merchant, "id");
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("records", mapper.merchantTickets(merchantId, keyword == null ? "" : keyword.trim(), normalizedStatus));
        result.put("statistics", mapper.merchantTicketStatistics(merchantId));
        return result;
    }

    public Map<String,Object> profile(long sysUserId) {
        Map<String,Object> merchant = requiredMerchant(sysUserId);
        Map<String,Object> profile = new LinkedHashMap<>();
        profile.put("name", merchant.get("name"));
        profile.put("address", merchant.get("address"));
        profile.put("usageRules", merchant.get("usageRules"));
        return profile;
    }

    @Transactional
    public Map<String,Object> redeem(long sysUserId, String ticketNo) {
        if (ticketNo == null || !ticketNo.trim().matches("[A-Fa-f0-9]{24}"))
            throw new IllegalArgumentException("请输入正确的24位核销码");
        Map<String,Object> merchant = requiredMerchant(sysUserId);
        Map<String,Object> ticket = mapper.lockTicket(ticketNo.trim().toUpperCase());
        if (ticket == null) throw new IllegalArgumentException("未找到该奖券");
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        policy.assertRedeemable(String.valueOf(ticket.get("status")), number(ticket,"merchantId"),
            number(merchant,"id"), (LocalDateTime)ticket.get("expiresAt"), now);
        if (mapper.redeemTicket(number(ticket,"id"), sysUserId, now) != 1)
            throw new IllegalStateException("奖券状态刚刚发生变化，请刷新后重试");
        return mapper.lockTicket(ticketNo.trim().toUpperCase());
    }

    private Map<String,Object> requiredMerchant(long sysUserId) {
        Map<String,Object> merchant = mapper.merchantBySysUserId(sysUserId);
        if (merchant == null || !bool(merchant,"enabled")) throw new IllegalStateException("当前账号未绑定可用商户");
        return merchant;
    }
    private static long number(Map<String,Object> map,String key){return ((Number)map.get(key)).longValue();}
    private static boolean bool(Map<String,Object> map,String key){Object v=map.get(key);return v instanceof Boolean?(Boolean)v:v!=null&&((Number)v).intValue()!=0;}
}

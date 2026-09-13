package com.ruoyi.lottery.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ruoyi.lottery.mapper.LotteryPortalMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
public class LotteryPortalService {
    public static final long MAIN_ACTIVITY_ID = 1L;
    private final LotteryPortalMapper mapper;
    private final DrawEligibilityPolicy eligibility = new DrawEligibilityPolicy();
    private final LotteryRules rules = new LotteryRules();
    private final WeightedPrizeSelector selector = new WeightedPrizeSelector();
    private final SecureRandom secureRandom = new SecureRandom();
    private final UserAccessPolicy userAccess = new UserAccessPolicy();

    public LotteryPortalService(LotteryPortalMapper mapper) { this.mapper = mapper; }

    public Map<String,Object> publicActivity() {
        Map<String,Object> source = requiredActivity();
        Map<String,Object> result = new LinkedHashMap<>();
        copy(source,result,"title","description","enabled","startAt","endAt","dailyFree","checkinReward","assistReward","assistDailyLimit","primaryColor","buttonColor","backgroundColor","backgroundUrl","bannerUrl","thanksLabel","noticeEnabled","noticeText","poolVersion");
        result.put("prizes", mapper.publicPrizes(MAIN_ACTIVITY_ID));
        result.put("rules", mapper.publicRules(MAIN_ACTIVITY_ID));
        return result;
    }

    @Transactional
    public Map<String,Object> findOrCreateTestUser(String phone) {
        Map<String,Object> user = mapper.userByPhone(phone);
        if (user != null) {
            userAccess.assertEnabled(bool(user,"enabled"));
            return user;
        }
        long id = IdWorker.getId();
        mapper.insertTestUser(id, "测试用户" + phone.substring(phone.length()-4), phone);
        user = mapper.userById(id);
        userAccess.assertEnabled(bool(user,"enabled"));
        return user;
    }

    @Transactional
    public Map<String,Object> myState(long userId) {
        requiredUser(userId);
        Map<String,Object> activity = requiredActivity();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        mapper.ensureUsage(IdWorker.getId(), MAIN_ACTIVITY_ID, userId, today);
        Map<String,Object> usage = mapper.usage(MAIN_ACTIVITY_ID,userId,today);
        int remaining = eligibility.remainingChances(integer(activity,"dailyFree"), integer(activity,"checkinReward"), bool(usage,"checkedIn"), integer(usage,"assistRewardCount"), integer(usage,"usedCount"));
        Map<String,Object> result = new LinkedHashMap<>(mapper.userById(userId));
        result.put("remainingChances",remaining);
        result.put("checkedIn",bool(usage,"checkedIn"));
        result.put("assistCount",mapper.assistCount(MAIN_ACTIVITY_ID,userId,today));
        result.put("tickets",mapper.userTickets(userId));
        return result;
    }

    @Transactional
    public Map<String,Object> checkIn(long userId) {
        requiredUser(userId);
        assertOpen(requiredActivity());
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        mapper.ensureUsage(IdWorker.getId(),MAIN_ACTIVITY_ID,userId,today);
        if (mapper.checkIn(MAIN_ACTIVITY_ID,userId,today)==0) throw new IllegalStateException("今天已经签到过了");
        return myState(userId);
    }

    @Transactional
    public Map<String,Object> assist(long helperId,long inviterId) {
        requiredUser(helperId); requiredUser(inviterId);
        if (helperId==inviterId) throw new IllegalArgumentException("不能为自己助力");
        Map<String,Object> activity=requiredActivity(); assertOpen(activity);
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Shanghai"));
        int limit=integer(activity,"assistDailyLimit");
        if(mapper.assistCount(MAIN_ACTIVITY_ID,inviterId,today)>=limit) throw new IllegalStateException("好友今天获得的助力次数已达上限");
        try{mapper.insertAssist(IdWorker.getId(),MAIN_ACTIVITY_ID,inviterId,helperId,today);}catch(DuplicateKeyException e){throw new IllegalStateException("今天已经为该好友助力过了");}
        mapper.ensureUsage(IdWorker.getId(),MAIN_ACTIVITY_ID,inviterId,today);
        mapper.addAssistReward(MAIN_ACTIVITY_ID,inviterId,today,integer(activity,"assistReward"));
        Map<String,Object> result=new LinkedHashMap<>(); result.put("assistCount",mapper.assistCount(MAIN_ACTIVITY_ID,inviterId,today)); result.put("message","助力成功"); return result;
    }

    public List<Map<String,Object>> tickets(long userId){ requiredUser(userId); return mapper.userTickets(userId); }

    @Transactional
    public Map<String,Object> draw(long userId,String requestId) {
        requiredUser(userId);
        if(requestId==null||!requestId.matches("[A-Za-z0-9_-]{8,64}")) throw new IllegalArgumentException("抽奖请求编号不合法");
        Map<String,Object> previous=mapper.drawByRequest(userId,requestId);
        if(previous!=null)return drawResult(previous);
        Map<String,Object> activity=mapper.lockActivity(MAIN_ACTIVITY_ID); assertOpen(activity);
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Shanghai"));
        mapper.ensureUsage(IdWorker.getId(),MAIN_ACTIVITY_ID,userId,today);
        Map<String,Object> usage=mapper.usage(MAIN_ACTIVITY_ID,userId,today);
        int remaining=eligibility.remainingChances(integer(activity,"dailyFree"),integer(activity,"checkinReward"),bool(usage,"checkedIn"),integer(usage,"assistRewardCount"),integer(usage,"usedCount"));
        if(remaining<1)throw new IllegalStateException("今天的抽奖次数已用完");

        List<Map<String,Object>> prizes=mapper.drawablePrizes(MAIN_ACTIVITY_ID);
        Map<Long,Map<String,Object>> eligible=new LinkedHashMap<>();
        for(Map<String,Object> prize:prizes){long id=number(prize,"id");if(mapper.userPrizeWins(userId,id)<integer(prize,"perUserLimit"))eligible.put(id,prize);}
        Long selected=null; Long directiveId=null; Map<String,Object> directive=mapper.nextDirective(MAIN_ACTIVITY_ID,userId);
        if(directive!=null&&eligible.containsKey(number(directive,"prizeId"))){directiveId=number(directive,"id");if(mapper.consumeDirective(directiveId)!=1)throw new IllegalStateException("定向中奖配置刚刚发生变化，请重新抽奖");selected=number(directive,"prizeId");}
        if(selected==null){List<WeightedPrizeSelector.Option> options=new ArrayList<>();for(Map<String,Object> prize:eligible.values())options.add(new WeightedPrizeSelector.Option(number(prize,"id"),(BigDecimal)prize.get("probability")));selected=selector.select(options,secureRandom.nextDouble()).orElse(null);}

        mapper.incrementUsed(MAIN_ACTIVITY_ID,userId,today);
        long drawId=IdWorker.getId();
        if(selected==null){mapper.insertDraw(drawId,MAIN_ACTIVITY_ID,userId,requestId,null,"THANKS","NONE",null,String.valueOf(activity.get("thanksLabel")));return response(false,null,String.valueOf(activity.get("thanksLabel")),null);}
        Map<String,Object> prize=eligible.get(selected);
        if(mapper.decrementStock(selected)!=1)throw new IllegalStateException("奖品库存刚刚发生变化，请重新抽奖");
        Map<String,Object> merchant=mapper.merchant(number(prize,"merchantId"));
        if(merchant==null)throw new IllegalStateException("奖品所属商户已停用");
        mapper.insertDraw(drawId,MAIN_ACTIVITY_ID,userId,requestId,selected,"WON",directiveId==null?"RANDOM":"DIRECTED",directiveId,String.valueOf(prize.get("name")));
        LocalDateTime now=LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        LocalDateTime expiry=rules.ticketExpiresAt(now,integer(prize,"validDays"),(LocalDateTime)prize.get("redeemEndAt"));
        String ticketNo=randomTicketNo();
        mapper.insertTicket(IdWorker.getId(),ticketNo,MAIN_ACTIVITY_ID,drawId,userId,selected,number(prize,"merchantId"),String.valueOf(prize.get("name")),text(prize,"imageUrl"),text(merchant,"name"),text(merchant,"address"),text(merchant,"usageRules"),expiry);
        return response(true,selected,String.valueOf(prize.get("name")),ticketNo);
    }

    private Map<String,Object> requiredActivity(){Map<String,Object> value=mapper.activity(MAIN_ACTIVITY_ID);if(value==null)throw new IllegalStateException("活动尚未配置");return value;}
    private Map<String,Object> requiredUser(long id){Map<String,Object> value=mapper.userById(id);if(value==null)throw new IllegalArgumentException("用户不存在");userAccess.assertEnabled(bool(value,"enabled"));return value;}
    private void assertOpen(Map<String,Object> activity){if(!rules.isActivityOpen(bool(activity,"enabled"),(LocalDateTime)activity.get("startAt"),(LocalDateTime)activity.get("endAt"),LocalDateTime.now(ZoneId.of("Asia/Shanghai"))))throw new IllegalStateException("活动暂未开放");}
    private static int integer(Map<String,Object> map,String key){Object v=map.get(key);return v==null?0:((Number)v).intValue();}
    private static long number(Map<String,Object> map,String key){return ((Number)map.get(key)).longValue();}
    private static String text(Map<String,Object> map,String key){Object v=map.get(key);return v==null?"":String.valueOf(v);}
    private static boolean bool(Map<String,Object> map,String key){Object v=map.get(key);return v instanceof Boolean?(Boolean)v:v!=null&&((Number)v).intValue()!=0;}
    private static void copy(Map<String,Object>s,Map<String,Object>d,String...keys){for(String key:keys)d.put(key,s.get(key));}
    private Map<String,Object> drawResult(Map<String,Object> row){return response("WON".equals(row.get("resultType")),row.get("prizeId")==null?null:number(row,"prizeId"),text(row,"resultMessage"),text(row,"ticketNo"));}
    private static Map<String,Object> response(boolean won,Long prizeId,String message,String ticketNo){Map<String,Object> result=new LinkedHashMap<>();result.put("won",won);result.put("prizeId",prizeId);result.put("prizeName",won?message:null);result.put("message",message);result.put("ticketNo",ticketNo);return result;}
    private String randomTicketNo(){byte[] bytes=new byte[12];secureRandom.nextBytes(bytes);StringBuilder b=new StringBuilder(24);for(byte value:bytes)b.append(String.format("%02X",value));return b.toString();}
}

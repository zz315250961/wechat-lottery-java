package com.ruoyi.lottery.mapper;

import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface LotteryPortalMapper {
    @Select("SELECT id,title,description,enabled,start_at startAt,end_at endAt,daily_free dailyFree,checkin_reward checkinReward,assist_reward assistReward,assist_daily_limit assistDailyLimit,primary_color primaryColor,button_color buttonColor,background_color backgroundColor,background_url backgroundUrl,banner_url bannerUrl,thanks_label thanksLabel,notice_enabled noticeEnabled,notice_text noticeText,pool_version poolVersion FROM lottery_activity WHERE id=#{id}")
    Map<String,Object> activity(@Param("id") long id);

    @Select("SELECT id,name,image_url imageUrl FROM lottery_prize WHERE activity_id=#{activityId} AND enabled=1 AND deleted=0 ORDER BY sort_order,id")
    List<Map<String,Object>> publicPrizes(@Param("activityId") long activityId);

    @Select("SELECT content FROM lottery_rule WHERE activity_id=#{activityId} AND enabled=1 ORDER BY sort_order,id")
    List<String> publicRules(@Param("activityId") long activityId);

    @Select("SELECT id,openid,unionid,nickname,avatar_url avatarUrl,phone,subscribed,enabled FROM lottery_user WHERE phone=#{phone} LIMIT 1")
    Map<String,Object> userByPhone(@Param("phone") String phone);
    @Select("SELECT id,openid,unionid,nickname,avatar_url avatarUrl,phone,subscribed,enabled FROM lottery_user WHERE openid=#{openid} LIMIT 1") Map<String,Object> userByOpenid(@Param("openid")String openid);
    @Insert("INSERT INTO lottery_user(id,openid,unionid,nickname,avatar_url,subscribed,enabled) VALUES(#{id},#{openid},#{unionid},#{nickname},#{avatar},#{subscribed},1)") int insertWechatUser(@Param("id")long id,@Param("openid")String openid,@Param("unionid")String unionid,@Param("nickname")String nickname,@Param("avatar")String avatar,@Param("subscribed")boolean subscribed);
    @Update("UPDATE lottery_user SET unionid=#{unionid},nickname=#{nickname},avatar_url=#{avatar},subscribed=#{subscribed} WHERE openid=#{openid}") int updateWechatUser(@Param("openid")String openid,@Param("unionid")String unionid,@Param("nickname")String nickname,@Param("avatar")String avatar,@Param("subscribed")boolean subscribed);

    @Select("SELECT id,openid,unionid,nickname,avatar_url avatarUrl,phone,subscribed,enabled FROM lottery_user WHERE id=#{id}")
    Map<String,Object> userById(@Param("id") long id);

    @Insert("INSERT INTO lottery_user(id,nickname,phone,enabled) VALUES(#{id},#{nickname},#{phone},1)")
    int insertTestUser(@Param("id") long id,@Param("nickname") String nickname,@Param("phone") String phone);

    @Insert("INSERT INTO lottery_daily_usage(id,activity_id,user_id,usage_day) VALUES(#{id},#{activityId},#{userId},#{day}) ON DUPLICATE KEY UPDATE id=id")
    int ensureUsage(@Param("id") long id,@Param("activityId") long activityId,@Param("userId") long userId,@Param("day") LocalDate day);

    @Select("SELECT used_count usedCount,checked_in checkedIn,assist_reward_count assistRewardCount FROM lottery_daily_usage WHERE activity_id=#{activityId} AND user_id=#{userId} AND usage_day=#{day}")
    Map<String,Object> usage(@Param("activityId") long activityId,@Param("userId") long userId,@Param("day") LocalDate day);

    @Update("UPDATE lottery_daily_usage SET checked_in=1 WHERE activity_id=#{activityId} AND user_id=#{userId} AND usage_day=#{day} AND checked_in=0")
    int checkIn(@Param("activityId") long activityId,@Param("userId") long userId,@Param("day") LocalDate day);

    @Insert("INSERT INTO lottery_assist(id,activity_id,inviter_id,helper_id,assist_day) VALUES(#{id},#{activityId},#{inviterId},#{helperId},#{day})")
    int insertAssist(@Param("id") long id,@Param("activityId") long activityId,@Param("inviterId") long inviterId,@Param("helperId") long helperId,@Param("day") LocalDate day);

    @Select("SELECT COUNT(*) FROM lottery_assist WHERE activity_id=#{activityId} AND inviter_id=#{inviterId} AND assist_day=#{day}")
    int assistCount(@Param("activityId") long activityId,@Param("inviterId") long inviterId,@Param("day") LocalDate day);

    @Update("UPDATE lottery_daily_usage SET assist_reward_count=assist_reward_count+#{reward} WHERE activity_id=#{activityId} AND user_id=#{userId} AND usage_day=#{day}")
    int addAssistReward(@Param("activityId") long activityId,@Param("userId") long userId,@Param("day") LocalDate day,@Param("reward") int reward);

    @Select("SELECT t.id,t.ticket_no ticketNo,t.prize_name prizeName,t.prize_image_url prizeImageUrl,t.merchant_name merchantName,t.merchant_address merchantAddress,t.usage_rules usageRules,t.status,t.expires_at expiresAt,t.redeemed_at redeemedAt,t.create_time createTime FROM lottery_ticket t WHERE t.user_id=#{userId} ORDER BY t.create_time DESC")
    List<Map<String,Object>> userTickets(@Param("userId") long userId);

    @Select("SELECT id,title,enabled,start_at startAt,end_at endAt,daily_free dailyFree,checkin_reward checkinReward,assist_reward assistReward,assist_daily_limit assistDailyLimit,thanks_label thanksLabel FROM lottery_activity WHERE id=#{id} FOR UPDATE")
    Map<String,Object> lockActivity(@Param("id") long id);

    @Select("SELECT d.id,d.prize_id prizeId,d.result_type resultType,d.result_message resultMessage,t.ticket_no ticketNo FROM lottery_draw d LEFT JOIN lottery_ticket t ON t.draw_id=d.id WHERE d.user_id=#{userId} AND d.request_id=#{requestId}")
    Map<String,Object> drawByRequest(@Param("userId") long userId,@Param("requestId") String requestId);

    @Select("SELECT id,name,address,usage_rules usageRules,enabled FROM lottery_merchant WHERE sys_user_id=#{sysUserId} LIMIT 1")
    Map<String,Object> merchantBySysUserId(@Param("sysUserId") long sysUserId);

    @Select({"<script>",
        "SELECT t.id,t.ticket_no ticketNo,t.user_id userId,u.nickname,u.phone,t.prize_name prizeName,t.prize_image_url prizeImageUrl,t.status,t.expires_at expiresAt,t.redeemed_at redeemedAt,t.create_time createTime",
        "FROM lottery_ticket t JOIN lottery_user u ON u.id=t.user_id",
        "WHERE t.merchant_id=#{merchantId}",
        "AND (#{keyword}='' OR t.ticket_no LIKE CONCAT('%',#{keyword},'%') OR u.phone LIKE CONCAT('%',#{keyword},'%') OR u.nickname LIKE CONCAT('%',#{keyword},'%') OR t.prize_name LIKE CONCAT('%',#{keyword},'%'))",
        "<if test=\"status == 'PENDING'\">AND t.status='PENDING' AND (t.expires_at IS NULL OR t.expires_at &gt;= NOW())</if>",
        "<if test=\"status == 'REDEEMED'\">AND t.status='REDEEMED'</if>",
        "<if test=\"status == 'EXPIRED'\">AND t.status='PENDING' AND t.expires_at &lt; NOW()</if>",
        "ORDER BY t.create_time DESC LIMIT 200",
        "</script>"})
    List<Map<String,Object>> merchantTickets(@Param("merchantId") long merchantId,@Param("keyword") String keyword,@Param("status") String status);

    @Select("SELECT COUNT(*) total,COALESCE(SUM(t.status='PENDING' AND (t.expires_at IS NULL OR t.expires_at>=NOW())),0) pending,COALESCE(SUM(t.status='REDEEMED'),0) redeemed,COALESCE(SUM(t.status='PENDING' AND t.expires_at<NOW()),0) expired FROM lottery_ticket t WHERE t.merchant_id=#{merchantId}")
    Map<String,Object> merchantTicketStatistics(@Param("merchantId") long merchantId);

    @Select("SELECT t.id,t.ticket_no ticketNo,t.user_id userId,u.nickname,u.phone,t.prize_name prizeName,t.prize_image_url prizeImageUrl,t.merchant_id merchantId,t.status,t.expires_at expiresAt,t.redeemed_at redeemedAt FROM lottery_ticket t JOIN lottery_user u ON u.id=t.user_id WHERE t.ticket_no=#{ticketNo} FOR UPDATE")
    Map<String,Object> lockTicket(@Param("ticketNo") String ticketNo);

    @Update("UPDATE lottery_ticket SET status='REDEEMED',redeemed_at=#{redeemedAt},redeemed_by=#{sysUserId} WHERE id=#{id} AND status='PENDING'")
    int redeemTicket(@Param("id") long id,@Param("sysUserId") long sysUserId,@Param("redeemedAt") java.time.LocalDateTime redeemedAt);

    @Select("SELECT p.id,p.name,p.image_url imageUrl,p.probability,p.stock,p.per_user_limit perUserLimit,p.valid_days validDays,p.redeem_end_at redeemEndAt,p.merchant_id merchantId FROM lottery_prize p WHERE p.activity_id=#{activityId} AND p.enabled=1 AND p.deleted=0 AND p.stock>0 ORDER BY p.sort_order,p.id")
    List<Map<String,Object>> drawablePrizes(@Param("activityId") long activityId);

    @Select("SELECT COUNT(*) FROM lottery_draw WHERE user_id=#{userId} AND prize_id=#{prizeId} AND result_type='WON'")
    int userPrizeWins(@Param("userId") long userId,@Param("prizeId") long prizeId);

    @Select("SELECT d.id,d.prize_id prizeId FROM lottery_directive d JOIN lottery_prize p ON p.id=d.prize_id WHERE d.activity_id=#{activityId} AND d.user_id=#{userId} AND d.remaining_count>0 AND p.enabled=1 AND p.deleted=0 AND p.stock>0 ORDER BY d.sort_order,d.id LIMIT 1 FOR UPDATE")
    Map<String,Object> nextDirective(@Param("activityId") long activityId,@Param("userId") long userId);

    @Update("UPDATE lottery_directive SET remaining_count=remaining_count-1 WHERE id=#{id} AND remaining_count>0")
    int consumeDirective(@Param("id") long id);

    @Update("UPDATE lottery_prize SET stock=stock-1 WHERE id=#{id} AND stock>0 AND enabled=1 AND deleted=0")
    int decrementStock(@Param("id") long id);

    @Update("UPDATE lottery_daily_usage SET used_count=used_count+1 WHERE activity_id=#{activityId} AND user_id=#{userId} AND usage_day=#{day}")
    int incrementUsed(@Param("activityId") long activityId,@Param("userId") long userId,@Param("day") LocalDate day);

    @Insert("INSERT INTO lottery_draw(id,activity_id,user_id,request_id,prize_id,result_type,source_type,directive_id,result_message) VALUES(#{id},#{activityId},#{userId},#{requestId},#{prizeId},#{resultType},#{sourceType},#{directiveId},#{message})")
    int insertDraw(@Param("id") long id,@Param("activityId") long activityId,@Param("userId") long userId,@Param("requestId") String requestId,@Param("prizeId") Long prizeId,@Param("resultType") String resultType,@Param("sourceType") String sourceType,@Param("directiveId") Long directiveId,@Param("message") String message);

    @Select("SELECT id,name,address,usage_rules usageRules FROM lottery_merchant WHERE id=#{id} AND enabled=1")
    Map<String,Object> merchant(@Param("id") long id);

    @Insert("INSERT INTO lottery_ticket(id,ticket_no,activity_id,draw_id,user_id,prize_id,merchant_id,prize_name,prize_image_url,merchant_name,merchant_address,usage_rules,status,expires_at) VALUES(#{id},#{ticketNo},#{activityId},#{drawId},#{userId},#{prizeId},#{merchantId},#{prizeName},#{imageUrl},#{merchantName},#{merchantAddress},#{usageRules},'PENDING',#{expiresAt})")
    int insertTicket(@Param("id") long id,@Param("ticketNo") String ticketNo,@Param("activityId") long activityId,@Param("drawId") long drawId,@Param("userId") long userId,@Param("prizeId") long prizeId,@Param("merchantId") long merchantId,@Param("prizeName") String prizeName,@Param("imageUrl") String imageUrl,@Param("merchantName") String merchantName,@Param("merchantAddress") String merchantAddress,@Param("usageRules") String usageRules,@Param("expiresAt") java.time.LocalDateTime expiresAt);
}

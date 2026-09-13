package com.ruoyi.lottery.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ruoyi.lottery.domain.LotteryActivity;
import com.ruoyi.lottery.domain.LotteryPrize;
import com.ruoyi.lottery.domain.LotteryRuleInput;
import com.ruoyi.lottery.mapper.LotteryActivityMapper;
import com.ruoyi.lottery.mapper.LotteryPrizeMapper;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
public class LotteryAdminService {
    private final LotteryActivityMapper activities; private final LotteryPrizeMapper prizes; private final JdbcTemplate jdbc;
    private final PrizeConfigurationPolicy policy=new PrizeConfigurationPolicy();
    private final MerchantAccountPolicy merchantPolicy=new MerchantAccountPolicy();
    public LotteryAdminService(LotteryActivityMapper a,LotteryPrizeMapper p,JdbcTemplate j){activities=a;prizes=p;jdbc=j;}
    public Map<String,Object> overview(){Map<String,Object> r=new LinkedHashMap<>();r.put("activity",activities.selectById(1L));List<Map<String,Object>> prizeRows=jdbc.queryForList("SELECT p.*,m.name merchant_name,(SELECT COUNT(*) FROM lottery_draw d WHERE d.prize_id=p.id AND d.result_type='WON') won_count,(SELECT COUNT(*) FROM lottery_ticket t WHERE t.prize_id=p.id AND t.status='REDEEMED') redeemed_count FROM lottery_prize p JOIN lottery_merchant m ON m.id=p.merchant_id WHERE p.deleted=0 ORDER BY p.sort_order,p.id");r.put("prizes",camelizePrizes(prizeRows));List<Map<String,Object>> merchantRows=jdbc.queryForList("SELECT m.*,u.user_name username FROM lottery_merchant m JOIN sys_user u ON u.user_id=m.sys_user_id ORDER BY m.id");for(Map<String,Object> merchant:merchantRows){merchant.put("id",String.valueOf(merchant.get("id")));merchant.put("sys_user_id",String.valueOf(merchant.get("sys_user_id")));}r.put("merchants",merchantRows);r.put("rules",jdbc.queryForList("SELECT * FROM lottery_rule WHERE activity_id=1 ORDER BY sort_order,id"));return r;}
    public LotteryActivity saveActivity(LotteryActivity value){value.setId(1L);if(activities.selectById(1L)==null)activities.insert(value);else activities.updateById(value);return activities.selectById(1L);}
    @Transactional public LotteryPrize savePrize(LotteryPrize p){p.setActivityId(1L);policy.validatePrize(p.getProbability(),p.getStock(),p.getPerUserLimit(),p.getValidDays());if(p.getId()==null){p.setId(IdWorker.getId());p.setDeleted(false);prizes.insert(p);}else prizes.updateById(p);List<BigDecimal> total=jdbc.query("SELECT probability FROM lottery_prize WHERE activity_id=1 AND enabled=1 AND deleted=0",(rs,n)->rs.getBigDecimal(1));policy.validateTotal(total);jdbc.update("UPDATE lottery_activity SET pool_version=pool_version+1 WHERE id=1");return prizes.selectById(p.getId());}
    @Transactional public void deletePrize(long id){prizes.deleteById(id);jdbc.update("UPDATE lottery_activity SET pool_version=pool_version+1 WHERE id=1");}
    @Transactional public void movePrize(long id,String direction){
        Map<String,Object> current=jdbc.queryForMap("SELECT id,sort_order FROM lottery_prize WHERE id=? AND deleted=0",id);
        int order=((Number)current.get("sort_order")).intValue();
        String op="up".equalsIgnoreCase(direction)?"<":">"; String sort="up".equalsIgnoreCase(direction)?"DESC":"ASC";
        List<Map<String,Object>> nearby=jdbc.queryForList("SELECT id,sort_order FROM lottery_prize WHERE activity_id=1 AND deleted=0 AND sort_order "+op+" ? ORDER BY sort_order "+sort+",id "+sort+" LIMIT 1",order);
        if(nearby.isEmpty())return; Map<String,Object> other=nearby.get(0); long otherId=((Number)other.get("id")).longValue(); int otherOrder=((Number)other.get("sort_order")).intValue();
        jdbc.update("UPDATE lottery_prize SET sort_order=? WHERE id=?",otherOrder,id);jdbc.update("UPDATE lottery_prize SET sort_order=? WHERE id=?",order,otherId);jdbc.update("UPDATE lottery_activity SET pool_version=pool_version+1 WHERE id=1");
    }
    public void setUserEnabled(long id,boolean enabled){if(jdbc.update("UPDATE lottery_user SET enabled=? WHERE id=?",enabled?1:0,id)!=1)throw new IllegalArgumentException("用户不存在");}
    public List<Map<String,Object>> users(String q){List<Map<String,Object>> rows=jdbc.queryForList("SELECT u.id,u.nickname,u.phone,u.openid,u.subscribed,u.enabled,u.create_time,(SELECT COUNT(*) FROM lottery_draw d WHERE d.user_id=u.id) draw_count,(SELECT COUNT(*) FROM lottery_ticket t WHERE t.user_id=u.id) ticket_count FROM lottery_user u WHERE ?='' OR u.phone LIKE CONCAT('%',?,'%') OR u.nickname LIKE CONCAT('%',?,'%') OR u.openid LIKE CONCAT('%',?,'%') ORDER BY u.create_time DESC LIMIT 500",q,q,q,q);for(Map<String,Object> row:rows)row.put("id",String.valueOf(row.get("id")));return rows;}
    public List<Map<String,Object>> tickets(String q){return jdbc.queryForList("SELECT t.*,u.nickname,u.phone,d.source_type,d.directive_id FROM lottery_ticket t JOIN lottery_user u ON u.id=t.user_id JOIN lottery_draw d ON d.id=t.draw_id WHERE ?='' OR t.ticket_no LIKE CONCAT('%',?,'%') OR u.phone LIKE CONCAT('%',?,'%') OR t.prize_name LIKE CONCAT('%',?,'%') ORDER BY t.create_time DESC LIMIT 500",q,q,q,q);}
    public List<Map<String,Object>> directives(){List<Map<String,Object>> rows=jdbc.queryForList("SELECT d.*,u.nickname,u.phone,p.name prize_name,CASE WHEN d.remaining_count=0 THEN 'COMPLETED' ELSE 'ACTIVE' END status,(d.initial_count-d.remaining_count) consumed_count,(SELECT COUNT(*) FROM lottery_draw w WHERE w.directive_id=d.id AND w.source_type='DIRECTED' AND w.result_type='WON') directed_win_count,(SELECT MAX(w.create_time) FROM lottery_draw w WHERE w.directive_id=d.id AND w.source_type='DIRECTED' AND w.result_type='WON') last_win_time FROM lottery_directive d JOIN lottery_user u ON u.id=d.user_id JOIN lottery_prize p ON p.id=d.prize_id ORDER BY (d.remaining_count=0),d.sort_order,d.id");for(Map<String,Object> row:rows){row.put("id",stringId(row.get("id")));row.put("activity_id",stringId(row.get("activity_id")));row.put("user_id",stringId(row.get("user_id")));row.put("prize_id",stringId(row.get("prize_id")));}return rows;}
    public void addDirective(long userId,long prizeId,int count){if(count<1)throw new IllegalArgumentException("指定次数至少为1");if(jdbc.queryForObject("SELECT COUNT(*) FROM lottery_user WHERE id=?",Integer.class,userId)!=1)throw new IllegalArgumentException("所选参与用户已失效，请刷新后重新选择");if(jdbc.queryForObject("SELECT COUNT(*) FROM lottery_prize WHERE id=? AND deleted=0",Integer.class,prizeId)!=1)throw new IllegalArgumentException("所选奖品已失效，请刷新后重新选择");jdbc.update("INSERT INTO lottery_directive(id,activity_id,user_id,prize_id,initial_count,remaining_count) VALUES(?,1,?,?,?,?)",IdWorker.getId(),userId,prizeId,count,count);}
    public void deleteDirective(long id){if(jdbc.queryForObject("SELECT COUNT(*) FROM lottery_draw WHERE directive_id=?",Integer.class,id)>0)throw new IllegalStateException("已有中奖记录的定向配置必须保留");jdbc.update("DELETE FROM lottery_directive WHERE id=?",id);}
    @Transactional public void replaceRules(List<LotteryRuleInput> rules){
        jdbc.update("DELETE FROM lottery_rule WHERE activity_id=1");
        int fallbackSort=0;
        for(LotteryRuleInput rule:rules){
            String content=rule.getContent()==null?"":rule.getContent().trim();
            if(content.isEmpty())continue;
            long id=rule.getId()==null?IdWorker.getId():rule.getId();
            int sort=rule.getSortOrder()==null?fallbackSort:rule.getSortOrder();
            boolean enabled=!Boolean.FALSE.equals(rule.getEnabled());
            jdbc.update("INSERT INTO lottery_rule(id,activity_id,content,enabled,sort_order) VALUES(?,1,?,?,?)",id,content,enabled?1:0,sort);
            fallbackSort++;
        }
    }
    @Transactional public Map<String,Object> saveMerchant(Map<String,Object>b){String username=text(b,"username"),password=text(b,"password"),name=text(b,"name");merchantPolicy.validate(username,password,name);Long id=b.get("id")==null?null:Long.valueOf(String.valueOf(b.get("id")));if(id==null){if(password.isEmpty())throw new IllegalArgumentException("请输入商户初始密码");if(jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE user_name=?",Integer.class,username)>0)throw new IllegalArgumentException("商户账号已存在");long uid=IdWorker.getId();jdbc.update("INSERT INTO sys_user(user_id,dept_id,user_name,nick_name,password,status,del_flag,create_by,create_time) VALUES(?,103,?,?,?,'0','0','admin',NOW())",uid,username,name,SecurityUtils.encryptPassword(password));jdbc.update("INSERT INTO sys_user_role(user_id,role_id) VALUES(?,3)",uid);id=IdWorker.getId();jdbc.update("INSERT INTO lottery_merchant(id,sys_user_id,name,address,usage_rules,enabled) VALUES(?,?,?,?,?,?)",id,uid,name,text(b,"address"),text(b,"usageRules"),bool(b,"enabled")?1:0);}else{Map<String,Object> m=jdbc.queryForMap("SELECT sys_user_id FROM lottery_merchant WHERE id=?",id);long uid=((Number)m.get("sys_user_id")).longValue();if(jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE user_name=? AND user_id<>?",Integer.class,username,uid)>0)throw new IllegalArgumentException("商户账号已存在");jdbc.update("INSERT IGNORE INTO sys_user_role(user_id,role_id) VALUES(?,3)",uid);jdbc.update("UPDATE lottery_merchant SET name=?,address=?,usage_rules=?,enabled=? WHERE id=?",name,text(b,"address"),text(b,"usageRules"),bool(b,"enabled")?1:0,id);jdbc.update("UPDATE sys_user SET user_name=?,nick_name=?,status=?,update_time=NOW() WHERE user_id=?",username,name,bool(b,"enabled")?"0":"1",uid);if(!password.isEmpty())jdbc.update("UPDATE sys_user SET password=?,pwd_update_date=NOW() WHERE user_id=?",SecurityUtils.encryptPassword(password),uid);}return Collections.<String,Object>singletonMap("id",String.valueOf(id));}
    private static String text(Map<String,Object>b,String k){Object v=b.get(k);return v==null?"":String.valueOf(v).trim();}private static boolean bool(Map<String,Object>b,String k){Object v=b.get(k);return Boolean.TRUE.equals(v)||"1".equals(String.valueOf(v))||"true".equalsIgnoreCase(String.valueOf(v));}
    private static List<Map<String,Object>> camelizePrizes(List<Map<String,Object>> rows){List<Map<String,Object>> result=new ArrayList<>();for(Map<String,Object> row:rows){Map<String,Object> p=new LinkedHashMap<>();p.put("id",stringId(row.get("id")));p.put("activityId",stringId(row.get("activity_id")));p.put("merchantId",stringId(row.get("merchant_id")));p.put("name",row.get("name"));p.put("imageUrl",row.get("image_url"));p.put("probability",row.get("probability"));p.put("stock",row.get("stock"));p.put("perUserLimit",row.get("per_user_limit"));p.put("validDays",row.get("valid_days"));p.put("redeemEndAt",row.get("redeem_end_at"));p.put("enabled",row.get("enabled"));p.put("sortOrder",row.get("sort_order"));p.put("merchantName",row.get("merchant_name"));p.put("wonCount",row.get("won_count"));p.put("redeemedCount",row.get("redeemed_count"));result.add(p);}return result;}
    private static String stringId(Object value){return value==null?null:String.valueOf(value);}
}

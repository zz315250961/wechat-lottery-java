-- 本地联调数据：可重复执行，不用于生产环境
USE wechat_lottery_java;

INSERT INTO sys_role(role_id,role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time,remark)
VALUES(3,'商户核销员','merchant',3,'3',1,1,'0','0','admin',NOW(),'商户核销角色')
ON DUPLICATE KEY UPDATE role_key='merchant',status='0',del_flag='0';

INSERT INTO sys_user(user_id,dept_id,user_name,nick_name,user_type,password,status,del_flag,create_by,create_time,remark) VALUES
(9100,NULL,'test-admin','测试管理员','00','$2b$12$06fCLIgU9Ytkitmq/MNxj.aOLhi4Y82vXTMHhMu0xgYwL08FNPaiy','0','0','admin',NOW(),'测试管理员，密码 test1'),
(9101,NULL,'test1','校园咖啡站','00','$2b$12$06fCLIgU9Ytkitmq/MNxj.aOLhi4Y82vXTMHhMu0xgYwL08FNPaiy','0','0','admin',NOW(),'测试商户，密码 test1'),
(9102,NULL,'test2','师院美食坊','00','$2b$12$5XJo/HsD0fIYQSL2Uz33AeDjyCdrjCCdR/9EMF0TNSf68xmOonltG','0','0','admin',NOW(),'测试商户，密码 test2'),
(9103,NULL,'test3','青春文创店','00','$2b$12$H8M4k/3WUqkuzyuttxGGeeU0H29QLc7Ls4.U2nBshlWo3dJPBbK/S','0','0','admin',NOW(),'测试商户，密码 test3')
ON DUPLICATE KEY UPDATE dept_id=NULL,nick_name=VALUES(nick_name),password=VALUES(password),status='0',del_flag='0',remark=VALUES(remark);
DELETE FROM sys_user_role WHERE user_id IN (9100,9101,9102,9103);
INSERT INTO sys_user_role(user_id,role_id) VALUES(9100,1),(9101,3),(9102,3),(9103,3);
DELETE FROM lottery_merchant WHERE sys_user_id = 9100;

INSERT INTO lottery_merchant(id,sys_user_id,name,address,usage_rules,enabled) VALUES
(9111,9101,'[测试] 校园咖啡站','师院东区食堂一层 08 号','仅限本店使用；不可兑换现金；每日 08:00-20:00 核销。',1),
(9112,9102,'[测试] 师院美食坊','师院生活广场二楼 12 号','到店出示二维码；一券限用一次；不可与其他优惠叠加。',1),
(9113,9103,'[测试] 青春文创店','大学生活动中心一楼','凭券领取对应商品；奖品颜色以门店库存为准。',1)
ON DUPLICATE KEY UPDATE name=VALUES(name),address=VALUES(address),usage_rules=VALUES(usage_rules),enabled=1;

UPDATE lottery_activity SET title='校园幸运抽奖（测试）',description='校园商户联合福利活动',enabled=1,start_at=NULL,end_at=NULL,daily_free=1,checkin_reward=1,assist_reward=1,assist_daily_limit=3,notice_enabled=1,notice_text='测试活动已开启，奖券请在有效期内到对应商家核销。' WHERE id=1;

INSERT INTO lottery_prize(id,activity_id,merchant_id,name,image_url,probability,stock,per_user_limit,valid_days,redeem_end_at,enabled,sort_order,deleted) VALUES
(9201,1,9111,'[测试] 拿铁咖啡','/lottery-images/coffee.png',15,48,2,30,NULL,1,1,0),
(9202,1,9112,'[测试] 双人简餐','/lottery-images/meal.png',10,39,1,30,NULL,1,2,0),
(9203,1,9113,'[测试] 校园帆布袋','/lottery-images/canvas-bag.png',12,60,2,30,NULL,1,3,0),
(9204,1,9112,'[测试] 10元代金券','/lottery-images/voucher-10.jpg',15,79,2,30,NULL,1,4,0),
(9205,1,9113,'[测试] 精美笔记本','/lottery-images/notebook.jpg',10,50,2,30,NULL,1,5,0),
(9206,1,9111,'[测试] 水果茶','/lottery-images/fruit-tea.jpg',8,69,2,30,NULL,1,6,0),
(9207,1,9113,'[测试] 钥匙扣','/lottery-images/keychain.jpg',8,80,2,30,NULL,1,7,0)
ON DUPLICATE KEY UPDATE merchant_id=VALUES(merchant_id),name=VALUES(name),image_url=VALUES(image_url),probability=VALUES(probability),stock=VALUES(stock),per_user_limit=VALUES(per_user_limit),valid_days=VALUES(valid_days),enabled=1,sort_order=VALUES(sort_order),deleted=0;

INSERT INTO lottery_user(id,openid,nickname,phone,subscribed,enabled,create_time) VALUES
(9301,'test-openid-01','[测试] 用户01','18800000001',1,1,NOW()-INTERVAL 8 DAY),
(9302,'test-openid-02','[测试] 用户02','18800000002',1,1,NOW()-INTERVAL 7 DAY),
(9303,'test-openid-03','[测试] 用户03','18800000003',0,1,NOW()-INTERVAL 6 DAY),
(9304,'test-openid-04','[测试] 用户04','18800000004',1,1,NOW()-INTERVAL 5 DAY),
(9305,'test-openid-05','[测试] 用户05','18800000005',0,1,NOW()-INTERVAL 4 DAY),
(9306,'test-openid-06','[测试] 用户06','18800000006',1,1,NOW()-INTERVAL 3 DAY),
(9307,'test-openid-07','[测试] 用户07','18800000007',1,1,NOW()-INTERVAL 2 DAY),
(9308,'test-openid-08','[测试] 用户08','18800000008',0,1,NOW()-INTERVAL 1 DAY)
ON DUPLICATE KEY UPDATE nickname=VALUES(nickname),subscribed=VALUES(subscribed),enabled=1;

INSERT INTO lottery_daily_usage(id,activity_id,user_id,usage_day,used_count,checked_in,assist_reward_count) VALUES
(9401,1,9301,CURDATE(),2,1,1),(9402,1,9302,CURDATE(),1,1,0),(9403,1,9303,CURDATE(),1,0,1),(9404,1,9304,CURDATE(),1,1,0)
ON DUPLICATE KEY UPDATE used_count=VALUES(used_count),checked_in=VALUES(checked_in),assist_reward_count=VALUES(assist_reward_count);
INSERT IGNORE INTO lottery_assist(id,activity_id,inviter_id,helper_id,assist_day) VALUES(9411,1,9301,9305,CURDATE()),(9412,1,9303,9306,CURDATE());

INSERT INTO lottery_draw(id,activity_id,user_id,request_id,prize_id,result_type,result_message,create_time) VALUES
(9501,1,9301,'seed-draw-01',9201,'WON','恭喜中奖',NOW()-INTERVAL 3 DAY),
(9502,1,9301,'seed-draw-02',9204,'WON','恭喜中奖',NOW()-INTERVAL 2 DAY),
(9503,1,9302,'seed-draw-03',9201,'WON','恭喜中奖',NOW()-INTERVAL 1 DAY),
(9504,1,9303,'seed-draw-04',9202,'WON','恭喜中奖',NOW()-INTERVAL 10 HOUR),
(9505,1,9304,'seed-draw-05',NULL,'THANKS','谢谢参与',NOW()-INTERVAL 5 HOUR)
ON DUPLICATE KEY UPDATE prize_id=VALUES(prize_id),result_type=VALUES(result_type),result_message=VALUES(result_message),create_time=VALUES(create_time);

INSERT INTO lottery_ticket(id,ticket_no,activity_id,draw_id,user_id,prize_id,merchant_id,prize_name,prize_image_url,merchant_name,merchant_address,usage_rules,status,expires_at,redeemed_at,redeemed_by,create_time) VALUES
(9601,'TEST20260910000000000001',1,9501,9301,9201,9111,'[测试] 拿铁咖啡','/lottery-images/coffee.png','[测试] 校园咖啡站','师院东区食堂一层 08 号','仅限本店使用；不可兑换现金；每日 08:00-20:00 核销。','REDEEMED',NOW()+INTERVAL 27 DAY,NOW()-INTERVAL 2 DAY,9101,NOW()-INTERVAL 3 DAY),
(9602,'TEST20260910000000000002',1,9502,9301,9204,9112,'[测试] 10元代金券','/lottery-images/voucher-10.jpg','[测试] 师院美食坊','师院生活广场二楼 12 号','到店出示二维码；一券限用一次；不可与其他优惠叠加。','PENDING',NOW()+INTERVAL 8 DAY,NULL,NULL,NOW()-INTERVAL 2 DAY),
(9603,'TEST20260910000000000003',1,9503,9302,9201,9111,'[测试] 拿铁咖啡','/lottery-images/coffee.png','[测试] 校园咖啡站','师院东区食堂一层 08 号','仅限本店使用；不可兑换现金；每日 08:00-20:00 核销。','PENDING',NOW()+INTERVAL 29 DAY,NULL,NULL,NOW()-INTERVAL 1 DAY),
(9604,'TEST20260910000000000004',1,9504,9303,9202,9112,'[测试] 双人简餐','/lottery-images/meal.png','[测试] 师院美食坊','师院生活广场二楼 12 号','到店出示二维码；一券限用一次；不可与其他优惠叠加。','REDEEMED',NOW()+INTERVAL 14 DAY,NOW()-INTERVAL 2 HOUR,9102,NOW()-INTERVAL 10 HOUR)
ON DUPLICATE KEY UPDATE prize_image_url=VALUES(prize_image_url),status=VALUES(status),expires_at=VALUES(expires_at),redeemed_at=VALUES(redeemed_at),redeemed_by=VALUES(redeemed_by);

INSERT INTO lottery_rule(id,activity_id,content,enabled,sort_order) VALUES
(9701,1,'每天可获得基础抽奖次数，签到和好友助力可增加次数。',1,1),
(9702,1,'奖券须在有效期内到指定商户使用，一券仅限核销一次。',1,2),
(9703,1,'测试数据均带有“[测试]”标识，可在正式上线前清理。',1,3)
ON DUPLICATE KEY UPDATE content=VALUES(content),enabled=1,sort_order=VALUES(sort_order);

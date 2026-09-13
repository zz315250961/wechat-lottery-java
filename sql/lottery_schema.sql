-- 校园幸运抽奖业务表（MySQL 8，UTF8MB4）
CREATE DATABASE IF NOT EXISTS wechat_lottery_java DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE wechat_lottery_java;

INSERT INTO sys_role(role_id,role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time,remark)
VALUES(3,'商户核销员','merchant',3,'3',1,1,'0','0','admin',NOW(),'仅用于所属商户奖券查询与核销')
ON DUPLICATE KEY UPDATE role_name=VALUES(role_name),role_key=VALUES(role_key),status='0',del_flag='0';

CREATE TABLE lottery_activity (
  id BIGINT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  description TEXT NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 0,
  start_at DATETIME NULL,
  end_at DATETIME NULL,
  daily_free INT UNSIGNED NOT NULL DEFAULT 0,
  checkin_reward INT UNSIGNED NOT NULL DEFAULT 0,
  assist_reward INT UNSIGNED NOT NULL DEFAULT 0,
  assist_daily_limit INT UNSIGNED NOT NULL DEFAULT 0,
  primary_color CHAR(7) NOT NULL DEFAULT '#ff781a',
  button_color CHAR(7) NOT NULL DEFAULT '#ff781a',
  background_color CHAR(7) NOT NULL DEFAULT '#fff8ed',
  background_url VARCHAR(1024) NOT NULL DEFAULT '',
  banner_url VARCHAR(1024) NOT NULL DEFAULT '',
  thanks_label VARCHAR(100) NOT NULL DEFAULT '谢谢参与',
  notice_enabled TINYINT(1) NOT NULL DEFAULT 0,
  notice_text VARCHAR(500) NOT NULL DEFAULT '',
  pool_version INT UNSIGNED NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT ck_activity_time CHECK (end_at IS NULL OR start_at IS NULL OR end_at > start_at)
) ENGINE=InnoDB;

CREATE TABLE lottery_rule (
  id BIGINT PRIMARY KEY,
  activity_id BIGINT NOT NULL,
  content VARCHAR(300) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  sort_order INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_rule_activity FOREIGN KEY (activity_id) REFERENCES lottery_activity(id),
  INDEX idx_rule_activity_sort (activity_id, sort_order)
) ENGINE=InnoDB;

CREATE TABLE lottery_merchant (
  id BIGINT PRIMARY KEY,
  sys_user_id BIGINT NOT NULL,
  name VARCHAR(200) NOT NULL,
  address VARCHAR(500) NOT NULL DEFAULT '',
  usage_rules TEXT NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_merchant_sys_user (sys_user_id)
) ENGINE=InnoDB;

CREATE TABLE lottery_prize (
  id BIGINT PRIMARY KEY,
  activity_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  name VARCHAR(200) NOT NULL,
  image_url VARCHAR(1024) NOT NULL DEFAULT '',
  probability DECIMAL(8,4) UNSIGNED NOT NULL DEFAULT 0,
  stock INT UNSIGNED NOT NULL DEFAULT 0,
  per_user_limit INT UNSIGNED NOT NULL DEFAULT 1,
  valid_days INT UNSIGNED NOT NULL DEFAULT 30,
  redeem_end_at DATETIME NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  sort_order INT NOT NULL DEFAULT 0,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_prize_activity FOREIGN KEY (activity_id) REFERENCES lottery_activity(id),
  CONSTRAINT fk_prize_merchant FOREIGN KEY (merchant_id) REFERENCES lottery_merchant(id),
  CONSTRAINT ck_prize_probability CHECK (probability <= 100),
  INDEX idx_prize_pool (activity_id, deleted, enabled, sort_order)
) ENGINE=InnoDB;

CREATE TABLE lottery_user (
  id BIGINT PRIMARY KEY,
  openid VARCHAR(128) NULL,
  unionid VARCHAR(128) NULL,
  nickname VARCHAR(200) NOT NULL DEFAULT '',
  avatar_url VARCHAR(1024) NOT NULL DEFAULT '',
  phone VARCHAR(20) NULL,
  subscribed TINYINT(1) NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_lottery_user_openid (openid),
  UNIQUE KEY uk_lottery_user_phone (phone),
  INDEX idx_lottery_user_unionid (unionid)
) ENGINE=InnoDB;

CREATE TABLE lottery_daily_usage (
  id BIGINT PRIMARY KEY,
  activity_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  usage_day DATE NOT NULL,
  used_count INT UNSIGNED NOT NULL DEFAULT 0,
  checked_in TINYINT(1) NOT NULL DEFAULT 0,
  assist_reward_count INT UNSIGNED NOT NULL DEFAULT 0,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_daily_usage (activity_id, user_id, usage_day),
  CONSTRAINT fk_usage_activity FOREIGN KEY (activity_id) REFERENCES lottery_activity(id),
  CONSTRAINT fk_usage_user FOREIGN KEY (user_id) REFERENCES lottery_user(id)
) ENGINE=InnoDB;

CREATE TABLE lottery_assist (
  id BIGINT PRIMARY KEY,
  activity_id BIGINT NOT NULL,
  inviter_id BIGINT NOT NULL,
  helper_id BIGINT NOT NULL,
  assist_day DATE NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_assist_once (activity_id, inviter_id, helper_id, assist_day),
  CONSTRAINT fk_assist_activity FOREIGN KEY (activity_id) REFERENCES lottery_activity(id),
  CONSTRAINT fk_assist_inviter FOREIGN KEY (inviter_id) REFERENCES lottery_user(id),
  CONSTRAINT fk_assist_helper FOREIGN KEY (helper_id) REFERENCES lottery_user(id),
  CONSTRAINT ck_assist_not_self CHECK (inviter_id <> helper_id)
) ENGINE=InnoDB;

CREATE TABLE lottery_directive (
  id BIGINT PRIMARY KEY,
  activity_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  prize_id BIGINT NOT NULL,
  initial_count INT UNSIGNED NULL COMMENT '初始配置次数；升级前历史记录为空',
  remaining_count INT UNSIGNED NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_directive_user (activity_id, user_id, sort_order),
  CONSTRAINT fk_directive_activity FOREIGN KEY (activity_id) REFERENCES lottery_activity(id),
  CONSTRAINT fk_directive_user FOREIGN KEY (user_id) REFERENCES lottery_user(id),
  CONSTRAINT fk_directive_prize FOREIGN KEY (prize_id) REFERENCES lottery_prize(id)
) ENGINE=InnoDB;

CREATE TABLE lottery_draw (
  id BIGINT PRIMARY KEY,
  activity_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  request_id VARCHAR(64) NOT NULL,
  prize_id BIGINT NULL,
  result_type VARCHAR(20) NOT NULL,
  source_type VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
  directive_id BIGINT NULL,
  result_message VARCHAR(200) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_draw_request (user_id, request_id),
  INDEX idx_draw_activity_time (activity_id, create_time),
  INDEX idx_draw_directive (directive_id, create_time),
  CONSTRAINT fk_draw_activity FOREIGN KEY (activity_id) REFERENCES lottery_activity(id),
  CONSTRAINT fk_draw_user FOREIGN KEY (user_id) REFERENCES lottery_user(id),
  CONSTRAINT fk_draw_prize FOREIGN KEY (prize_id) REFERENCES lottery_prize(id),
  CONSTRAINT fk_draw_directive FOREIGN KEY (directive_id) REFERENCES lottery_directive(id),
  CONSTRAINT ck_draw_source CHECK (source_type IN ('UNKNOWN','NONE','RANDOM','DIRECTED'))
) ENGINE=InnoDB;

CREATE TABLE lottery_ticket (
  id BIGINT PRIMARY KEY,
  ticket_no CHAR(24) NOT NULL,
  activity_id BIGINT NOT NULL,
  draw_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  prize_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  prize_name VARCHAR(200) NOT NULL,
  prize_image_url VARCHAR(1024) NOT NULL DEFAULT '',
  merchant_name VARCHAR(200) NOT NULL,
  merchant_address VARCHAR(500) NOT NULL DEFAULT '',
  usage_rules TEXT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  expires_at DATETIME NOT NULL,
  redeemed_at DATETIME NULL,
  redeemed_by BIGINT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ticket_no (ticket_no),
  UNIQUE KEY uk_ticket_draw (draw_id),
  INDEX idx_ticket_user (user_id, create_time),
  INDEX idx_ticket_merchant_status (merchant_id, status, create_time),
  CONSTRAINT fk_ticket_activity FOREIGN KEY (activity_id) REFERENCES lottery_activity(id),
  CONSTRAINT fk_ticket_draw FOREIGN KEY (draw_id) REFERENCES lottery_draw(id),
  CONSTRAINT fk_ticket_user FOREIGN KEY (user_id) REFERENCES lottery_user(id),
  CONSTRAINT fk_ticket_prize FOREIGN KEY (prize_id) REFERENCES lottery_prize(id),
  CONSTRAINT fk_ticket_merchant FOREIGN KEY (merchant_id) REFERENCES lottery_merchant(id),
  CONSTRAINT ck_ticket_status CHECK (status IN ('PENDING','REDEEMED'))
) ENGINE=InnoDB;

INSERT INTO lottery_activity
(id, title, description, enabled, daily_free, checkin_reward, assist_reward, assist_daily_limit)
VALUES (1, '校园幸运抽奖', '校园商户联合福利活动', 0, 0, 1, 1, 3);

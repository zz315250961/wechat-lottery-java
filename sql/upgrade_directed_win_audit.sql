-- 定向中奖历史与中奖来源审计升级（MySQL 8）。
-- 历史中奖无法可靠判断来源，因此保留 UNKNOWN；不猜测、不回填伪造关系。

SET @schema_name = DATABASE();

SET @ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='lottery_directive' AND column_name='initial_count'),
  'SELECT 1',
  'ALTER TABLE lottery_directive ADD COLUMN initial_count INT UNSIGNED NULL COMMENT ''初始配置次数；升级前历史记录为空'' AFTER prize_id'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='lottery_draw' AND column_name='source_type'),
  'SELECT 1',
  'ALTER TABLE lottery_draw ADD COLUMN source_type VARCHAR(20) NOT NULL DEFAULT ''UNKNOWN'' AFTER result_type'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='lottery_draw' AND column_name='directive_id'),
  'SELECT 1',
  'ALTER TABLE lottery_draw ADD COLUMN directive_id BIGINT NULL AFTER source_type'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=@schema_name AND table_name='lottery_draw' AND index_name='idx_draw_directive'),
  'SELECT 1',
  'ALTER TABLE lottery_draw ADD INDEX idx_draw_directive (directive_id, create_time)'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema=@schema_name AND table_name='lottery_draw' AND constraint_name='fk_draw_directive'),
  'SELECT 1',
  'ALTER TABLE lottery_draw ADD CONSTRAINT fk_draw_directive FOREIGN KEY (directive_id) REFERENCES lottery_directive(id)'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

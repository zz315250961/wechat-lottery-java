USE wechat_lottery_java;

DROP PROCEDURE IF EXISTS repair_utf8_mojibake;
DELIMITER $$
CREATE PROCEDURE repair_utf8_mojibake()
BEGIN
  DECLARE finished INT DEFAULT 0;
  DECLARE table_name_value VARCHAR(64);
  DECLARE column_name_value VARCHAR(64);
  DECLARE text_columns CURSOR FOR
    SELECT TABLE_NAME, COLUMN_NAME
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND DATA_TYPE IN ('char', 'varchar', 'text', 'mediumtext', 'longtext')
       AND TABLE_NAME NOT LIKE 'QRTZ\_%';
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished = 1;

  OPEN text_columns;
  repair_loop: LOOP
    FETCH text_columns INTO table_name_value, column_name_value;
    IF finished = 1 THEN
      LEAVE repair_loop;
    END IF;

    SET @repair_sql = CONCAT(
      'UPDATE `', REPLACE(table_name_value, '`', '``'), '` SET `',
      REPLACE(column_name_value, '`', '``'), '` = ',
      'CONVERT(CAST(CONVERT(`', REPLACE(column_name_value, '`', '``'),
      '` USING latin1) AS BINARY) USING utf8mb4) ',
      'WHERE `', REPLACE(column_name_value, '`', '``'),
      '` REGEXP ''[ÃÂæåçèéäïð]'' ',
      'AND CONVERT(CAST(CONVERT(`', REPLACE(column_name_value, '`', '``'),
      '` USING latin1) AS BINARY) USING utf8mb4) IS NOT NULL'
    );
    PREPARE repair_statement FROM @repair_sql;
    EXECUTE repair_statement;
    DEALLOCATE PREPARE repair_statement;
  END LOOP;
  CLOSE text_columns;
END$$
DELIMITER ;

CALL repair_utf8_mojibake();
DROP PROCEDURE repair_utf8_mojibake;

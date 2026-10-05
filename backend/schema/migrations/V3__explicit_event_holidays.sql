-- Store school-defined holidays explicitly instead of guessing from event names.
-- Existing events remain unchanged and default to non-holiday until an admin marks them.
SET @migration_sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'school_events' AND column_name = 'holiday'),
  'SELECT 1',
  'ALTER TABLE `school_events` ADD COLUMN `holiday` bit(1) NOT NULL DEFAULT b''0'''
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

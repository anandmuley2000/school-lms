-- Keep the reviewer's displayed name with leave history without linking it to
-- an account row that may later be archived or removed.
SET @migration_sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'leave_applications'
      AND column_name = 'reviewer_name'),
  'SELECT 1',
  'ALTER TABLE `leave_applications` ADD COLUMN `reviewer_name` varchar(255) NULL'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

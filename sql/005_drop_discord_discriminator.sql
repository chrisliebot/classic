SET
    default_storage_engine = INNODB;

SELECT `version_4`
FROM `version`;

ALTER TABLE `discord_message_trace`
    DROP COLUMN `sourceUserDiscriminator`;

ALTER TABLE `version` RENAME COLUMN `version_4` TO `version_5`;

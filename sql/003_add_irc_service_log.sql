SET
    default_storage_engine = INNODB;

SELECT `version_2`
FROM `version`;

ALTER TABLE `chatlog`
    ADD COLUMN service TEXT NOT NULL
        AFTER `timestamp`;

ALTER TABLE `version` RENAME COLUMN `version_2` TO `version_3`;

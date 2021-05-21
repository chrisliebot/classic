SET
    default_storage_engine = INNODB;

SELECT `version_3`
FROM `version`;

ALTER TABLE `chatlog`
    CHANGE `timestamp` `timestamp` TIMESTAMP NOT NULL DEFAULT current_timestamp();

ALTER TABLE `version` RENAME COLUMN `version_3` TO `version_4`;

SET
default_storage_engine = INNODB;

SELECT `version_1`
FROM `version`;

CREATE TABLE `discord_message_trace`
(
    `channelId`               BIGINT UNSIGNED NOT NULL,
    `messageId`               BIGINT UNSIGNED NOT NULL,

    `sourceGuildId`           BIGINT UNSIGNED,
    `sourceChannelId`         BIGINT UNSIGNED NOT NULL,
    `sourceMessageId`         BIGINT UNSIGNED NOT NULL,

    `sourceUserNickname`      TEXT NOT NULL,
    `sourceUserDiscriminator` INT UNSIGNED NOT NULL,
    `sourceUserId`            BIGINT UNSIGNED NOT NULL,
    `sourceContent`           Text NOT NULL,

    PRIMARY KEY (channelId, messageId)

);

ALTER TABLE `version` RENAME COLUMN `version_1` TO `version_2`;

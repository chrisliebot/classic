SET default_storage_engine = INNODB;

CREATE TABLE `version`
(
    version_1 SERIAL
);

CREATE TABLE `chatlog`
(
    `id`        SERIAL PRIMARY KEY,
    `timestamp` TIMESTAMP                                                          NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    `context`   TEXT                                                               NOT NULL,
    `type`      ENUM ('NORMAL','NOTICE','CTCP','JOIN','PART','QUIT','NICK','KICK') NOT NULL,
    `nickname`  TEXT                                                               NOT NULL,
    `realname`  TEXT                                                                        DEFAULT NULL,
    `ident`     TEXT                                                                        DEFAULT NULL,
    `host`      TEXT                                                                        DEFAULT NULL,
    `account`   TEXT                                                                        DEFAULT NULL,
    `message`   TEXT
);


CREATE TABLE `timer`
(
    `id`          SERIAL PRIMARY KEY,

    `service`     TEXT         NOT NULL,
    `user`        TEXT         NOT NULL,
    `channel`     TEXT         NOT NULL,
    `text`        TEXT         NOT NULL,

    `creation`    TIMESTAMP    NOT NULL,
    `due`         TIMESTAMP    NOT NULL,
    `snooze`      TIMESTAMP    NULL,

    `snoozeCount` INT UNSIGNED NOT NULL,

    `deleted`     BOOLEAN      NOT NULL
);

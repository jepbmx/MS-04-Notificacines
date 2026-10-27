CREATE DATABASE IF NOT EXISTS ecommerce
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ecommerce;

CREATE TABLE IF NOT EXISTS notifications (
  id_notification  INT          NOT NULL AUTO_INCREMENT,
  id_receiver      VARCHAR(100) NOT NULL,
  title            VARCHAR(200) NOT NULL,
  content          TEXT         NOT NULL,
  type             VARCHAR(50)  NOT NULL,
  status           VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
  is_read          TINYINT(1)   NOT NULL DEFAULT 0,
  read_at          DATETIME(6)  NULL,
  idempotency_key  VARCHAR(120) NULL,
  retry_count      INT          NOT NULL DEFAULT 0,
  max_retries      INT          NOT NULL DEFAULT 3,
  last_error_code  VARCHAR(50)  NULL,
  created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  sent_at          DATETIME(6)  NULL,
  PRIMARY KEY (id_notification),
  UNIQUE KEY uq_notification_idempotency (idempotency_key),
  KEY ix_notif_receiver_created (id_receiver, created_at),
  KEY ix_notif_status (status),
  KEY ix_notif_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notification_attempts (
  id_attempt       INT          NOT NULL AUTO_INCREMENT,
  id_notification  INT          NOT NULL,
  attempt_number   INT          NOT NULL,
  status           VARCHAR(30)  NOT NULL,
  error_code       VARCHAR(50)  NULL,
  error_message    VARCHAR(255) NULL,
  attempted_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id_attempt),
  UNIQUE KEY uq_attempt_number (id_notification, attempt_number),
  CONSTRAINT fk_attempt_notification FOREIGN KEY (id_notification)
      REFERENCES notifications (id_notification) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notification_templates (
  id_template      INT          NOT NULL AUTO_INCREMENT,
  type             VARCHAR(50)  NOT NULL,
  title_template   VARCHAR(200) NOT NULL,
  content_template TEXT         NOT NULL,
  is_active        TINYINT(1)   NOT NULL DEFAULT 1,
  created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                               ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id_template),
  UNIQUE KEY uq_notification_template_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS processed_events (
  event_id        VARCHAR(120) NOT NULL,
  event_type      VARCHAR(100) NOT NULL,
  id_notification INT          NOT NULL,
  processed_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (event_id),
  UNIQUE KEY uq_processed_event_notification (id_notification),
  CONSTRAINT fk_processed_event_notification FOREIGN KEY (id_notification)
      REFERENCES notifications (id_notification) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

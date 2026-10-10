-- Backoff exponencial del OutboxPublisher (ADR-008, seccion 28). Antes reintentaba cada
-- ciclo (2 s) y abandonaba el evento tras 5 intentos: una caida de ~10 s del broker perdia
-- eventos. next_attempt_at = cuando puede volver a intentarse.
ALTER TABLE outbox_events ADD COLUMN next_attempt_at TIMESTAMPTZ;

CREATE INDEX ix_outbox_events_ready ON outbox_events (status, next_attempt_at);

-- Re-encola los eventos abandonados por la logica anterior. Es seguro: Notification es
-- idempotente por event_id (ADR-015), un duplicado no genera una segunda notificacion.
UPDATE outbox_events SET retry_count = 0 WHERE status = 'FAILED';

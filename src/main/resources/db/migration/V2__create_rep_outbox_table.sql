CREATE TABLE rep_outbox (
  id             UUID PRIMARY KEY,
  tenant_id      UUID NOT NULL,
  aggregate_id   UUID NOT NULL,
  event_type     VARCHAR(64) NOT NULL,
  payload        JSONB NOT NULL,
  status         VARCHAR(16) NOT NULL,
  attempt_count  INTEGER NOT NULL DEFAULT 0,
  last_error     TEXT,
  available_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_rep_outbox_status_available
  ON rep_outbox (status, available_at);

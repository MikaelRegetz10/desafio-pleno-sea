CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   name VARCHAR(150) NOT NULL,
   email VARCHAR(150) NOT NULL UNIQUE,
   password_hash VARCHAR(255) NOT NULL,
   role VARCHAR(20) NOT NULL,
   enabled BOOLEAN NOT NULL DEFAULT TRUE,
   created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

   CONSTRAINT chk_user_role CHECK (role IN ('CLIENT', 'ANALYST', 'ADMIN'))
);

CREATE INDEX idx_users_email ON users(email);

CREATE TABLE analyst_coverage (
  user_id UUID NOT NULL,
  state VARCHAR(2) NOT NULL,

  PRIMARY KEY (user_id, state),
  CONSTRAINT fk_analyst_coverage_user FOREIGN KEY (user_id)
      REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT chk_state_length CHECK (LENGTH(state) = 2)
);

CREATE INDEX idx_analyst_coverage_user_id ON analyst_coverage(user_id);
CREATE INDEX idx_analyst_coverage_state ON analyst_coverage(state);

CREATE TABLE solicitations (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   client_id UUID NOT NULL,
   status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
   current_step INT NOT NULL DEFAULT 1,

    -- Step 1 Dados básicos
    service_type VARCHAR(30),
    title VARCHAR(80),
    description VARCHAR(1000),

    -- Step 2 Endereço
    cep VARCHAR(10),
    street VARCHAR(255),
    number VARCHAR(20),
    complement VARCHAR(100),
    neighborhood VARCHAR(100),
    city VARCHAR(100),
    state VARCHAR(2),

    -- Step 3 Confirmação e dados finais
    priority VARCHAR(10),
    preferred_date DATE,
    estimated_value NUMERIC(12, 2),
    terms_accepted BOOLEAN NOT NULL DEFAULT FALSE,

    -- Auditoria
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP WITH TIME ZONE,
    analyzed_at TIMESTAMP WITH TIME ZONE,
    analyzed_by UUID,
    analysis_comment VARCHAR(1000),

    -- Constraints
    CONSTRAINT fk_solicitations_client FOREIGN KEY (client_id)
       REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_solicitations_analyst FOREIGN KEY (analyzed_by)
       REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_solicitation_status CHECK (
       status IN ('DRAFT', 'SUBMITTED', 'IN_REVIEW', 'APPROVED', 'REJECTED')
       ),
    CONSTRAINT chk_current_step CHECK (current_step BETWEEN 1 AND 3),
    CONSTRAINT chk_service_type CHECK (
       service_type IS NULL OR service_type IN ('INSTALLATION', 'MAINTENANCE', 'INSPECTION')
       ),
    CONSTRAINT chk_priority CHECK (
       priority IS NULL OR priority IN ('LOW', 'MEDIUM', 'HIGH')
       )
);

CREATE INDEX idx_solicitations_client_id ON solicitations(client_id);
CREATE INDEX idx_solicitations_status ON solicitations(status);
CREATE INDEX idx_solicitations_state ON solicitations(state);
CREATE INDEX idx_solicitations_created_at ON solicitations(created_at);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    role VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100),
    duration_ms BIGINT NOT NULL,
    success BOOLEAN NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
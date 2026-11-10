CREATE TABLE roles (
    role_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO roles (role_name, description)
VALUES
('ADMIN', 'Complete application access'),
('NOC_MANAGER', 'Manages network operations and incidents'),
('NOC_ANALYST', 'Monitors alarms and analyzes outages'),
('NETWORK_ENGINEER', 'Manages network components and dependencies'),
('FIELD_TECHNICIAN', 'Performs field restoration activities'),
('VIEWER', 'Read-only application access');

select * from roles;

CREATE TABLE users (
    user_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_users_role
        FOREIGN KEY (role_id)
        REFERENCES roles(role_id)
);

CREATE TABLE components (
    component_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    component_code VARCHAR(50) NOT NULL UNIQUE,
    component_name VARCHAR(150) NOT NULL,
    component_type VARCHAR(50) NOT NULL,
    location VARCHAR(200),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    status VARCHAR(30) NOT NULL DEFAULT 'UP',
    criticality VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_component_status
        CHECK (status IN (
            'UP', 'DOWN', 'DEGRADED', 'MAINTENANCE', 'UNKNOWN'
        )),

    CONSTRAINT chk_component_criticality
        CHECK (criticality IN (
            'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
        )),

    CONSTRAINT chk_component_latitude
        CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),

    CONSTRAINT chk_component_longitude
        CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
);

CREATE TABLE component_dependencies (
    dependency_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source_component_id BIGINT NOT NULL,
    dependent_component_id BIGINT NOT NULL,
    dependency_type VARCHAR(50) NOT NULL DEFAULT 'REQUIRED',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_dependency_source
        FOREIGN KEY (source_component_id)
        REFERENCES components(component_id),

    CONSTRAINT fk_dependency_dependent
        FOREIGN KEY (dependent_component_id)
        REFERENCES components(component_id),

    CONSTRAINT chk_no_self_dependency
        CHECK (source_component_id <> dependent_component_id),

    CONSTRAINT uq_component_dependency
        UNIQUE (source_component_id, dependent_component_id),

    CONSTRAINT chk_dependency_type
        CHECK (dependency_type IN (
            'REQUIRED', 'OPTIONAL', 'REDUNDANT'
        ))
);

CREATE TABLE services (
    service_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    service_code VARCHAR(50) NOT NULL UNIQUE,
    service_name VARCHAR(150) NOT NULL,
    service_type VARCHAR(50),
    estimated_customer_count BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_service_status
        CHECK (status IN (
            'ACTIVE', 'DEGRADED', 'DOWN', 'INACTIVE'
        )),

    CONSTRAINT chk_customer_count
        CHECK (estimated_customer_count >= 0)
);

CREATE TABLE service_components (
    service_component_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    service_id BIGINT NOT NULL,
    component_id BIGINT NOT NULL,
    is_critical BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_service_component_service
        FOREIGN KEY (service_id)
        REFERENCES services(service_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_service_component_component
        FOREIGN KEY (component_id)
        REFERENCES components(component_id),

    CONSTRAINT uq_service_component
        UNIQUE (service_id, component_id)
);

CREATE TABLE alarms (
    alarm_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    component_id BIGINT NOT NULL,
    alarm_code VARCHAR(50) NOT NULL,
    alarm_type VARCHAR(100),
    severity VARCHAR(20) NOT NULL,
    description TEXT,
    alarm_status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acknowledged_at TIMESTAMP,
    cleared_at TIMESTAMP,

    CONSTRAINT fk_alarm_component
        FOREIGN KEY (component_id)
        REFERENCES components(component_id),

    CONSTRAINT chk_alarm_severity
        CHECK (severity IN (
            'CRITICAL', 'MAJOR', 'MINOR', 'WARNING', 'INFO'
        )),

    CONSTRAINT chk_alarm_status
        CHECK (alarm_status IN (
            'OPEN', 'ACKNOWLEDGED', 'CLEARED'
        )),

    CONSTRAINT chk_alarm_dates
        CHECK (
            (cleared_at IS NULL OR cleared_at >= occurred_at)
            AND
            (acknowledged_at IS NULL OR acknowledged_at >= occurred_at)
        )
);

CREATE TABLE incidents (
    incident_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    incident_code VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    root_component_id BIGINT,
    severity VARCHAR(20) NOT NULL,
    incident_status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    created_by BIGINT,
    inventory_completeness VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_incident_root_component
        FOREIGN KEY (root_component_id)
        REFERENCES components(component_id),

    CONSTRAINT fk_incident_creator
        FOREIGN KEY (created_by)
        REFERENCES users(user_id),

    CONSTRAINT chk_incident_severity
        CHECK (severity IN (
            'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'
        )),

    CONSTRAINT chk_incident_status
        CHECK (incident_status IN (
            'OPEN', 'INVESTIGATING', 'IN_PROGRESS',
            'RESOLVED', 'CLOSED'
        )),

    CONSTRAINT chk_inventory_completeness
        CHECK (inventory_completeness IN (
            'COMPLETE', 'PARTIAL', 'UNKNOWN'
        )),

    CONSTRAINT chk_incident_resolution_date
        CHECK (resolved_at IS NULL OR resolved_at >= started_at)
);

CREATE TABLE incident_alarms (
    incident_id BIGINT NOT NULL,
    alarm_id BIGINT NOT NULL,
    linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (incident_id, alarm_id),

    CONSTRAINT fk_incident_alarm_incident
        FOREIGN KEY (incident_id)
        REFERENCES incidents(incident_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_incident_alarm_alarm
        FOREIGN KEY (alarm_id)
        REFERENCES alarms(alarm_id)
        ON DELETE CASCADE
);

CREATE TABLE incident_affected_components (
    incident_affected_component_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    incident_id BIGINT NOT NULL,
    component_id BIGINT NOT NULL,
    impact_status VARCHAR(30) NOT NULL DEFAULT 'ESTIMATED',
    impact_level VARCHAR(20),
    is_root_cause BOOLEAN NOT NULL DEFAULT FALSE,
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_affected_component_incident
        FOREIGN KEY (incident_id)
        REFERENCES incidents(incident_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_affected_component_component
        FOREIGN KEY (component_id)
        REFERENCES components(component_id),

    CONSTRAINT uq_incident_affected_component
        UNIQUE (incident_id, component_id),

    CONSTRAINT chk_component_impact_status
        CHECK (impact_status IN (
            'ESTIMATED', 'CONFIRMED', 'CLEARED'
        )),

    CONSTRAINT chk_component_impact_level
        CHECK (
            impact_level IS NULL OR
            impact_level IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')
        )
);

CREATE TABLE incident_affected_services (
    incident_affected_service_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    incident_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    impact_status VARCHAR(30) NOT NULL DEFAULT 'ESTIMATED',
    estimated_affected_customers BIGINT NOT NULL DEFAULT 0,
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_affected_service_incident
        FOREIGN KEY (incident_id)
        REFERENCES incidents(incident_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_affected_service_service
        FOREIGN KEY (service_id)
        REFERENCES services(service_id),

    CONSTRAINT uq_incident_affected_service
        UNIQUE (incident_id, service_id),

    CONSTRAINT chk_service_impact_status
        CHECK (impact_status IN (
            'ESTIMATED', 'CONFIRMED', 'CLEARED'
        )),

    CONSTRAINT chk_affected_customers
        CHECK (estimated_affected_customers >= 0)
);

CREATE TABLE restoration_history (
    restoration_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    incident_id BIGINT NOT NULL,
    component_id BIGINT,
    performed_by BIGINT,
    action_description TEXT NOT NULL,
    previous_status VARCHAR(30),
    new_status VARCHAR(30),
    restoration_status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS',
    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_restoration_incident
        FOREIGN KEY (incident_id)
        REFERENCES incidents(incident_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_restoration_component
        FOREIGN KEY (component_id)
        REFERENCES components(component_id),

    CONSTRAINT fk_restoration_user
        FOREIGN KEY (performed_by)
        REFERENCES users(user_id),

    CONSTRAINT chk_restoration_status
        CHECK (restoration_status IN (
            'PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED'
        ))
);

CREATE TABLE audit_logs (
    audit_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id BIGINT,
    details JSONB,
    ip_address INET,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE SET NULL
);

-- Users
CREATE INDEX idx_users_role_id
ON users(role_id);

-- Components
CREATE INDEX idx_components_status
ON components(status);

CREATE INDEX idx_components_type
ON components(component_type);

-- Component dependencies
CREATE INDEX idx_dependency_source
ON component_dependencies(source_component_id);

CREATE INDEX idx_dependency_dependent
ON component_dependencies(dependent_component_id);

-- Services
CREATE INDEX idx_service_components_component
ON service_components(component_id);

-- Alarms
CREATE INDEX idx_alarms_component
ON alarms(component_id);

CREATE INDEX idx_alarms_severity
ON alarms(severity);

CREATE INDEX idx_alarms_status
ON alarms(alarm_status);

CREATE INDEX idx_alarms_occurred_at
ON alarms(occurred_at DESC);

-- Incidents
CREATE INDEX idx_incidents_status
ON incidents(incident_status);

CREATE INDEX idx_incidents_severity
ON incidents(severity);

CREATE INDEX idx_incidents_started_at
ON incidents(started_at DESC);

-- Incident impact
CREATE INDEX idx_affected_components_incident
ON incident_affected_components(incident_id);

CREATE INDEX idx_affected_services_incident
ON incident_affected_services(incident_id);

-- Restoration
CREATE INDEX idx_restoration_incident
ON restoration_history(incident_id);

-- Audit logs
CREATE INDEX idx_audit_user
ON audit_logs(user_id);

CREATE INDEX idx_audit_created_at
ON audit_logs(created_at DESC);

SELECT
    table_name
FROM information_schema.tables
WHERE table_schema = 'public'
AND table_type = 'BASE TABLE'
ORDER BY table_name;

SELECT 'components' AS table_name, COUNT(*) FROM components
UNION ALL
SELECT 'services', COUNT(*) FROM services
UNION ALL
SELECT 'alarms', COUNT(*) FROM alarms
UNION ALL
SELECT 'incidents', COUNT(*) FROM incidents;

INSERT INTO components
(component_code, component_name, component_type, location,
 latitude, longitude, status, criticality)
VALUES
('CORE-RTR-01', 'Core Router 01', 'ROUTER',
 'Chennai Core Network', 13.0827, 80.2707, 'UP', 'CRITICAL'),

('AGG-RTR-01', 'Aggregation Router 01', 'ROUTER',
 'Chennai Central', 13.0604, 80.2496, 'UP', 'HIGH'),

('BTS-001', 'Base Tower 001', 'BTS',
 'Chennai North', 13.1150, 80.2900, 'UP', 'HIGH'),

('BTS-002', 'Base Tower 002', 'BTS',
 'Chennai South', 12.9500, 80.2400, 'UP', 'HIGH'),

('FIBER-001', 'Fiber Link 001', 'FIBER',
 'Chennai West', 13.0400, 80.2100, 'UP', 'MEDIUM'),

('BTS-003', 'Base Tower 003', 'BTS',
 'Chennai West', 13.0500, 80.2000, 'UP', 'HIGH');

 SELECT
    component_id,
    component_code,
    component_name,
    component_type,
    status,
    criticality
FROM components
ORDER BY component_id;

select * from roles;

BEGIN;

-- 1. USERS
-- Uses the ADMIN role you have already inserted.

INSERT INTO users
(full_name, email, password_hash, role_id, is_active)
SELECT
    'System Administrator',
    'admin@telecom.com',
    'TEMP_NOT_FOR_LOGIN',
    role_id,
    TRUE
FROM roles
WHERE role_name = 'ADMIN'
ON CONFLICT (email) DO NOTHING;


-- 2. COMPONENT DEPENDENCIES
-- Existing component IDs are fetched using component_code.

INSERT INTO component_dependencies
(source_component_id, dependent_component_id,
 dependency_type, is_active)
SELECT
    source.component_id,
    dependent.component_id,
    'REQUIRED',
    TRUE
FROM (
    VALUES
        ('CORE-RTR-01', 'AGG-RTR-01'),
        ('AGG-RTR-01', 'BTS-001'),
        ('AGG-RTR-01', 'BTS-002'),
        ('AGG-RTR-01', 'FIBER-001'),
        ('FIBER-001', 'BTS-003')
) AS network(source_code, dependent_code)
JOIN components source
    ON source.component_code = network.source_code
JOIN components dependent
    ON dependent.component_code = network.dependent_code
WHERE NOT EXISTS (
    SELECT 1
    FROM component_dependencies cd
    WHERE cd.source_component_id = source.component_id
      AND cd.dependent_component_id = dependent.component_id
);


-- 3. SERVICES

INSERT INTO services
(service_code, service_name, service_type,
 estimated_customer_count, status)
VALUES
('SVC-001', 'Mobile Internet Service',
 'INTERNET', 5000, 'ACTIVE'),

('SVC-002', 'Voice Communication Service',
 'VOICE', 3000, 'ACTIVE')
ON CONFLICT (service_code) DO NOTHING;


-- 4. SERVICE COMPONENT MAPPING

INSERT INTO service_components
(service_id, component_id, is_critical)
SELECT
    s.service_id,
    c.component_id,
    TRUE
FROM (
    VALUES
        ('SVC-001', 'BTS-001'),
        ('SVC-001', 'BTS-002'),
        ('SVC-002', 'BTS-003')
) AS mapping(service_code, component_code)
JOIN services s
    ON s.service_code = mapping.service_code
JOIN components c
    ON c.component_code = mapping.component_code
WHERE NOT EXISTS (
    SELECT 1
    FROM service_components sc
    WHERE sc.service_id = s.service_id
      AND sc.component_id = c.component_id
);


-- 5. ALARMS

INSERT INTO alarms
(component_id, alarm_code, alarm_type, severity,
 description, alarm_status, occurred_at)
SELECT
    c.component_id,
    'ALM-001',
    'NODE_FAILURE',
    'CRITICAL',
    'Core router failure detected in Chennai network',
    'OPEN',
    CURRENT_TIMESTAMP
FROM components c
WHERE c.component_code = 'CORE-RTR-01'
AND NOT EXISTS (
    SELECT 1
    FROM alarms a
    WHERE a.alarm_code = 'ALM-001'
);


-- 6. INCIDENTS

INSERT INTO incidents
(incident_code, title, description,
 root_component_id, severity, status,
 started_at, created_by, inventory_completeness)
SELECT
    'INC-001',
    'Core Router Failure',
    'Critical failure of the core router affecting downstream network components and services.',
    c.component_id,
    'CRITICAL',
    'OPEN',
    CURRENT_TIMESTAMP,
    u.user_id,
    'COMPLETE'
FROM components c
CROSS JOIN users u
WHERE c.component_code = 'CORE-RTR-01'
  AND u.email = 'admin@telecom.com'
ON CONFLICT (incident_code) DO NOTHING;


-- 7. INCIDENT ALARM MAPPING

INSERT INTO incident_alarms
(incident_id, alarm_id)
SELECT
    i.incident_id,
    a.alarm_id
FROM incidents i
CROSS JOIN alarms a
WHERE i.incident_code = 'INC-001'
  AND a.alarm_code = 'ALM-001'
ON CONFLICT DO NOTHING;


-- 8. INCIDENT AFFECTED COMPONENTS

INSERT INTO incident_affected_components
(incident_id, component_id, impact_status,
 impact_level, is_root_cause, detected_at)
SELECT
    i.incident_id,
    c.component_id,
    CASE
        WHEN c.component_code = 'CORE-RTR-01'
        THEN 'CONFIRMED'
        ELSE 'ESTIMATED'
    END,
    CASE
        WHEN c.component_code = 'CORE-RTR-01'
        THEN 'CRITICAL'
        ELSE 'HIGH'
    END,
    c.component_code = 'CORE-RTR-01',
    CURRENT_TIMESTAMP
FROM incidents i
CROSS JOIN components c
WHERE i.incident_code = 'INC-001'
AND c.component_code IN (
    'CORE-RTR-01',
    'AGG-RTR-01',
    'BTS-001',
    'BTS-002',
    'FIBER-001',
    'BTS-003'
)
ON CONFLICT (incident_id, component_id) DO NOTHING;


-- 9. INCIDENT AFFECTED SERVICES

INSERT INTO incident_affected_services
(incident_id, service_id, impact_status,
 estimated_affected_customers, detected_at)
SELECT
    i.incident_id,
    s.service_id,
    'ESTIMATED',
    s.estimated_customer_count,
    CURRENT_TIMESTAMP
FROM incidents i
CROSS JOIN services s
WHERE i.incident_code = 'INC-001'
AND s.service_code IN ('SVC-001', 'SVC-002')
ON CONFLICT (incident_id, service_id) DO NOTHING;


-- 10. RESTORATION HISTORY

INSERT INTO restoration_history
(incident_id, component_id, performed_by,
 action_description, previous_status, new_status,
 restoration_status, performed_at)
SELECT
    i.incident_id,
    c.component_id,
    u.user_id,
    'Core router failure recorded. Restoration pending.',
    'UP',
    'DOWN',
    'PENDING',
    CURRENT_TIMESTAMP
FROM incidents i
JOIN components c
    ON c.component_code = 'CORE-RTR-01'
JOIN users u
    ON u.email = 'admin@telecom.com'
WHERE i.incident_code = 'INC-001'
AND NOT EXISTS (
    SELECT 1
    FROM restoration_history rh
    WHERE rh.incident_id = i.incident_id
      AND rh.component_id = c.component_id
      AND rh.action_description =
          'Core router failure recorded. Restoration pending.'
);


-- 11. AUDIT LOGS

INSERT INTO audit_logs
(user_id, action, entity_type, entity_id,
 details, ip_address)
SELECT
    u.user_id,
    'SAMPLE_INCIDENT_CREATED',
    'INCIDENT',
    i.incident_id,
    '{"incident_code":"INC-001","source":"sample_seed_data"}'::jsonb,
    '127.0.0.1'::inet
FROM users u
CROSS JOIN incidents i
WHERE u.email = 'admin@telecom.com'
  AND i.incident_code = 'INC-001'
AND NOT EXISTS (
    SELECT 1
    FROM audit_logs al
    WHERE al.action = 'SAMPLE_INCIDENT_CREATED'
      AND al.entity_type = 'INCIDENT'
      AND al.entity_id = i.incident_id
);


-- SIMULATE CORE ROUTER FAILURE

UPDATE components
SET status = 'DOWN'
WHERE component_code = 'CORE-RTR-01';

COMMIT;

ROLLBACK;

SELECT
    column_name,
    data_type
FROM information_schema.columns
WHERE table_name = 'incidents'
ORDER BY ordinal_position;

SELECT
    table_name,
    ordinal_position,
    column_name,
    data_type,
    character_maximum_length,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = 'public'
AND table_name IN (
    'roles',
    'users',
    'components',
    'component_dependencies',
    'services',
    'service_components',
    'alarms',
    'incidents',
    'incident_alarms',
    'incident_affected_components',
    'incident_affected_services',
    'restoration_history',
    'audit_logs'
)
ORDER BY
    CASE table_name
        WHEN 'roles' THEN 1
        WHEN 'users' THEN 2
        WHEN 'components' THEN 3
        WHEN 'component_dependencies' THEN 4
        WHEN 'services' THEN 5
        WHEN 'service_components' THEN 6
        WHEN 'alarms' THEN 7
        WHEN 'incidents' THEN 8
        WHEN 'incident_alarms' THEN 9
        WHEN 'incident_affected_components' THEN 10
        WHEN 'incident_affected_services' THEN 11
        WHEN 'restoration_history' THEN 12
        WHEN 'audit_logs' THEN 13
    END,
    ordinal_position;

SELECT
    tc.table_name,
    tc.constraint_name,
    tc.constraint_type,
    cc.check_clause
FROM information_schema.table_constraints tc
LEFT JOIN information_schema.check_constraints cc
    ON tc.constraint_name = cc.constraint_name
   AND tc.constraint_schema = cc.constraint_schema
WHERE tc.table_schema = 'public'
AND tc.table_name IN (
    'roles',
    'users',
    'components',
    'component_dependencies',
    'services',
    'service_components',
    'alarms',
    'incidents',
    'incident_alarms',
    'incident_affected_components',
    'incident_affected_services',
    'restoration_history',
    'audit_logs'
)
ORDER BY tc.table_name, tc.constraint_type;


BEGIN;

-- 1. USERS
-- Use the ADMIN role that already exists.

INSERT INTO users
    (full_name, email, password_hash, role_id, is_active)
SELECT
    'System Administrator',
    'admin@telecom.com',
    'TEMP_NOT_FOR_LOGIN',
    role_id,
    TRUE
FROM roles
WHERE role_name = 'ADMIN'
ON CONFLICT (email) DO NOTHING;


-- 2. COMPONENT DEPENDENCIES

INSERT INTO component_dependencies
    (source_component_id, dependent_component_id,
     dependency_type, is_active)
SELECT
    src.component_id,
    dst.component_id,
    'REQUIRED',
    TRUE
FROM (
    VALUES
        ('CORE-RTR-01', 'AGG-RTR-01'),
        ('AGG-RTR-01', 'BTS-001'),
        ('AGG-RTR-01', 'BTS-002'),
        ('AGG-RTR-01', 'FIBER-001'),
        ('FIBER-001', 'BTS-003')
) AS network(source_code, dependent_code)
JOIN components src
    ON src.component_code = network.source_code
JOIN components dst
    ON dst.component_code = network.dependent_code
ON CONFLICT (source_component_id, dependent_component_id)
DO NOTHING;


-- 3. SERVICES

INSERT INTO services
    (service_code, service_name, service_type,
     estimated_customer_count, status)
VALUES
    ('SVC-001', 'Mobile Internet Service',
     'INTERNET', 5000, 'ACTIVE'),

    ('SVC-002', 'Voice Communication Service',
     'VOICE', 3000, 'ACTIVE')
ON CONFLICT (service_code) DO NOTHING;


-- 4. SERVICE COMPONENT MAPPING

INSERT INTO service_components
    (service_id, component_id, is_critical)
SELECT
    s.service_id,
    c.component_id,
    TRUE
FROM (
    VALUES
        ('SVC-001', 'BTS-001'),
        ('SVC-001', 'BTS-002'),
        ('SVC-002', 'BTS-003')
) AS mapping(service_code, component_code)
JOIN services s
    ON s.service_code = mapping.service_code
JOIN components c
    ON c.component_code = mapping.component_code
ON CONFLICT (service_id, component_id) DO NOTHING;


-- 5. ALARMS

INSERT INTO alarms
    (component_id, alarm_code, alarm_type, severity,
     description, alarm_status, occurred_at)
SELECT
    c.component_id,
    'ALM-001',
    'NODE_FAILURE',
    'CRITICAL',
    'Core router failure detected in Chennai network',
    'OPEN',
    CURRENT_TIMESTAMP
FROM components c
WHERE c.component_code = 'CORE-RTR-01'
AND NOT EXISTS (
    SELECT 1
    FROM alarms a
    WHERE a.alarm_code = 'ALM-001'
);


-- 6. INCIDENTS
-- Correct column: incident_status

INSERT INTO incidents
    (incident_code, title, description,
     root_component_id, severity, incident_status,
     started_at, created_by, inventory_completeness)
SELECT
    'INC-001',
    'Core Router Failure',
    'Critical failure of the core router affecting downstream network components and services.',
    c.component_id,
    'CRITICAL',
    'OPEN',
    CURRENT_TIMESTAMP,
    u.user_id,
    'COMPLETE'
FROM components c
CROSS JOIN users u
WHERE c.component_code = 'CORE-RTR-01'
AND u.email = 'admin@telecom.com'
ON CONFLICT (incident_code) DO NOTHING;


-- 7. INCIDENT ALARM MAPPING

INSERT INTO incident_alarms
    (incident_id, alarm_id)
SELECT
    i.incident_id,
    a.alarm_id
FROM incidents i
CROSS JOIN alarms a
WHERE i.incident_code = 'INC-001'
AND a.alarm_code = 'ALM-001'
ON CONFLICT (incident_id, alarm_id) DO NOTHING;


-- 8. INCIDENT AFFECTED COMPONENTS

INSERT INTO incident_affected_components
    (incident_id, component_id, impact_status,
     impact_level, is_root_cause, detected_at)
SELECT
    i.incident_id,
    c.component_id,
    CASE
        WHEN c.component_code = 'CORE-RTR-01'
        THEN 'CONFIRMED'
        ELSE 'ESTIMATED'
    END,
    CASE
        WHEN c.component_code = 'CORE-RTR-01'
        THEN 'CRITICAL'
        ELSE 'HIGH'
    END,
    (c.component_code = 'CORE-RTR-01'),
    CURRENT_TIMESTAMP
FROM incidents i
CROSS JOIN components c
WHERE i.incident_code = 'INC-001'
AND c.component_code IN (
    'CORE-RTR-01',
    'AGG-RTR-01',
    'BTS-001',
    'BTS-002',
    'FIBER-001',
    'BTS-003'
)
ON CONFLICT (incident_id, component_id) DO NOTHING;


-- 9. INCIDENT AFFECTED SERVICES

INSERT INTO incident_affected_services
    (incident_id, service_id, impact_status,
     estimated_affected_customers, detected_at)
SELECT
    i.incident_id,
    s.service_id,
    'ESTIMATED',
    s.estimated_customer_count,
    CURRENT_TIMESTAMP
FROM incidents i
CROSS JOIN services s
WHERE i.incident_code = 'INC-001'
AND s.service_code IN ('SVC-001', 'SVC-002')
ON CONFLICT (incident_id, service_id) DO NOTHING;


-- 10. RESTORATION HISTORY

INSERT INTO restoration_history
    (incident_id, component_id, performed_by,
     action_description, previous_status, new_status,
     restoration_status, performed_at)
SELECT
    i.incident_id,
    c.component_id,
    u.user_id,
    'Core router failure recorded. Restoration pending.',
    'UP',
    'DOWN',
    'PENDING',
    CURRENT_TIMESTAMP
FROM incidents i
JOIN components c
    ON c.component_code = 'CORE-RTR-01'
JOIN users u
    ON u.email = 'admin@telecom.com'
WHERE i.incident_code = 'INC-001'
AND NOT EXISTS (
    SELECT 1
    FROM restoration_history rh
    WHERE rh.incident_id = i.incident_id
      AND rh.component_id = c.component_id
      AND rh.action_description =
          'Core router failure recorded. Restoration pending.'
);


-- 11. AUDIT LOGS

INSERT INTO audit_logs
    (user_id, action, entity_type, entity_id,
     details, ip_address)
SELECT
    u.user_id,
    'SAMPLE_INCIDENT_CREATED',
    'INCIDENT',
    i.incident_id,
    '{"incident_code":"INC-001","source":"sample_seed_data"}'::jsonb,
    '127.0.0.1'::inet
FROM users u
CROSS JOIN incidents i
WHERE u.email = 'admin@telecom.com'
AND i.incident_code = 'INC-001'
AND NOT EXISTS (
    SELECT 1
    FROM audit_logs al
    WHERE al.action = 'SAMPLE_INCIDENT_CREATED'
      AND al.entity_type = 'INCIDENT'
      AND al.entity_id = i.incident_id
);


-- 12. SIMULATE THE CORE ROUTER FAILURE

UPDATE components
SET status = 'DOWN',
    updated_at = CURRENT_TIMESTAMP
WHERE component_code = 'CORE-RTR-01';

COMMIT;


SELECT 'roles' AS table_name, COUNT(*) AS total FROM roles
UNION ALL
SELECT 'users', COUNT(*) FROM users
UNION ALL
SELECT 'components', COUNT(*) FROM components
UNION ALL
SELECT 'component_dependencies', COUNT(*) FROM component_dependencies
UNION ALL
SELECT 'services', COUNT(*) FROM services
UNION ALL
SELECT 'service_components', COUNT(*) FROM service_components
UNION ALL
SELECT 'alarms', COUNT(*) FROM alarms
UNION ALL
SELECT 'incidents', COUNT(*) FROM incidents
UNION ALL
SELECT 'incident_alarms', COUNT(*) FROM incident_alarms
UNION ALL
SELECT 'incident_affected_components', COUNT(*) FROM incident_affected_components
UNION ALL
SELECT 'incident_affected_services', COUNT(*) FROM incident_affected_services
UNION ALL
SELECT 'restoration_history', COUNT(*) FROM restoration_history
UNION ALL
SELECT 'audit_logs', COUNT(*) FROM audit_logs
ORDER BY table_name;

SELECT
    component_code,
    component_name,
    status,
    criticality
FROM components
ORDER BY component_id;


-- Telecom Network Outage Blast-Radius Mapper
-- Deployment schema generated from the user's PostgreSQL 18.6 schema-only dump.
-- Contains schema objects only; it does not contain table data.
-- Preserves the extracted tables, identity sequences, constraints, indexes,
-- and foreign keys. No guessed tables or columns have been added.
--
-- Apply this file to a NEW, EMPTY PostgreSQL database:
--   psql -U postgres -d telecom -v ON_ERROR_STOP=1 -f telecom_deployment_schema.sql
--
-- Do not run against a database that already contains these objects.

BEGIN;

--
-- PostgreSQL database dump
--


-- Dumped from database version 18.6
-- Dumped by pg_dump version 18.6




--
-- Name: alarms; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.alarms (
    alarm_id bigint NOT NULL,
    component_id bigint NOT NULL,
    alarm_code character varying(50) NOT NULL,
    alarm_type character varying(100),
    severity character varying(20) NOT NULL,
    description text,
    alarm_status character varying(30) DEFAULT 'OPEN'::character varying NOT NULL,
    occurred_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    acknowledged_at timestamp without time zone,
    cleared_at timestamp without time zone,
    CONSTRAINT chk_alarm_dates CHECK ((((cleared_at IS NULL) OR (cleared_at >= occurred_at)) AND ((acknowledged_at IS NULL) OR (acknowledged_at >= occurred_at)))),
    CONSTRAINT chk_alarm_severity CHECK (((severity)::text = ANY ((ARRAY['CRITICAL'::character varying, 'MAJOR'::character varying, 'MINOR'::character varying, 'WARNING'::character varying, 'INFO'::character varying])::text[]))),
    CONSTRAINT chk_alarm_status CHECK (((alarm_status)::text = ANY ((ARRAY['OPEN'::character varying, 'ACKNOWLEDGED'::character varying, 'CLEARED'::character varying])::text[])))
);


--
-- Name: alarms_alarm_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.alarms ALTER COLUMN alarm_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.alarms_alarm_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_logs (
    audit_id bigint NOT NULL,
    user_id bigint,
    action character varying(100) NOT NULL,
    entity_type character varying(100),
    entity_id bigint,
    details jsonb,
    ip_address inet,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: audit_logs_audit_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.audit_logs ALTER COLUMN audit_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.audit_logs_audit_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: component_dependencies; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.component_dependencies (
    dependency_id bigint NOT NULL,
    source_component_id bigint NOT NULL,
    dependent_component_id bigint NOT NULL,
    dependency_type character varying(50) DEFAULT 'REQUIRED'::character varying NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_dependency_type CHECK (((dependency_type)::text = ANY ((ARRAY['REQUIRED'::character varying, 'OPTIONAL'::character varying, 'REDUNDANT'::character varying])::text[]))),
    CONSTRAINT chk_no_self_dependency CHECK ((source_component_id <> dependent_component_id))
);


--
-- Name: component_dependencies_dependency_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.component_dependencies ALTER COLUMN dependency_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.component_dependencies_dependency_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: components; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.components (
    component_id bigint NOT NULL,
    component_code character varying(50) NOT NULL,
    component_name character varying(150) NOT NULL,
    component_type character varying(50) NOT NULL,
    location character varying(200),
    latitude numeric(10,7),
    longitude numeric(10,7),
    status character varying(30) DEFAULT 'UP'::character varying NOT NULL,
    criticality character varying(20) DEFAULT 'MEDIUM'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_component_criticality CHECK (((criticality)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying, 'CRITICAL'::character varying])::text[]))),
    CONSTRAINT chk_component_latitude CHECK (((latitude IS NULL) OR ((latitude >= ('-90'::integer)::numeric) AND (latitude <= (90)::numeric)))),
    CONSTRAINT chk_component_longitude CHECK (((longitude IS NULL) OR ((longitude >= ('-180'::integer)::numeric) AND (longitude <= (180)::numeric)))),
    CONSTRAINT chk_component_status CHECK (((status)::text = ANY ((ARRAY['UP'::character varying, 'DOWN'::character varying, 'DEGRADED'::character varying, 'MAINTENANCE'::character varying, 'UNKNOWN'::character varying])::text[])))
);


--
-- Name: components_component_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.components ALTER COLUMN component_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.components_component_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: failure_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.failure_events (
    failure_event_id bigint NOT NULL,
    failed_component_id bigint NOT NULL,
    event_source character varying(20) NOT NULL,
    failure_type character varying(50),
    event_status character varying(30) DEFAULT 'ACTIVE'::character varying NOT NULL,
    started_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    scheduled_recovery_at timestamp without time zone,
    recovered_at timestamp without time zone,
    incident_id bigint,
    alarm_id bigint,
    CONSTRAINT chk_event_source CHECK (((event_source)::text = ANY ((ARRAY['REALTIME'::character varying, 'SIMULATED'::character varying])::text[]))),
    CONSTRAINT chk_event_status CHECK (((event_status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'RECOVERING'::character varying, 'RECOVERED'::character varying, 'FAILED'::character varying])::text[])))
);


--
-- Name: failure_events_failure_event_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.failure_events ALTER COLUMN failure_event_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.failure_events_failure_event_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: incident_affected_components; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.incident_affected_components (
    incident_affected_component_id bigint CONSTRAINT incident_affected_component_incident_affected_componen_not_null NOT NULL,
    incident_id bigint NOT NULL,
    component_id bigint NOT NULL,
    impact_status character varying(30) DEFAULT 'ESTIMATED'::character varying NOT NULL,
    impact_level character varying(20),
    is_root_cause boolean DEFAULT false NOT NULL,
    detected_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_component_impact_level CHECK (((impact_level IS NULL) OR ((impact_level)::text = ANY ((ARRAY['CRITICAL'::character varying, 'HIGH'::character varying, 'MEDIUM'::character varying, 'LOW'::character varying])::text[])))),
    CONSTRAINT chk_component_impact_status CHECK (((impact_status)::text = ANY ((ARRAY['ESTIMATED'::character varying, 'CONFIRMED'::character varying, 'CLEARED'::character varying])::text[])))
);


--
-- Name: incident_affected_components_incident_affected_component_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.incident_affected_components ALTER COLUMN incident_affected_component_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.incident_affected_components_incident_affected_component_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: incident_affected_services; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.incident_affected_services (
    incident_affected_service_id bigint CONSTRAINT incident_affected_services_incident_affected_service_i_not_null NOT NULL,
    incident_id bigint NOT NULL,
    service_id bigint NOT NULL,
    impact_status character varying(30) DEFAULT 'ESTIMATED'::character varying NOT NULL,
    estimated_affected_customers bigint DEFAULT 0 CONSTRAINT incident_affected_services_estimated_affected_customer_not_null NOT NULL,
    detected_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_affected_customers CHECK ((estimated_affected_customers >= 0)),
    CONSTRAINT chk_service_impact_status CHECK (((impact_status)::text = ANY ((ARRAY['ESTIMATED'::character varying, 'CONFIRMED'::character varying, 'CLEARED'::character varying])::text[])))
);


--
-- Name: incident_affected_services_incident_affected_service_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.incident_affected_services ALTER COLUMN incident_affected_service_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.incident_affected_services_incident_affected_service_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: incident_alarms; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.incident_alarms (
    incident_id bigint NOT NULL,
    alarm_id bigint NOT NULL,
    linked_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: incidents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.incidents (
    incident_id bigint NOT NULL,
    incident_code character varying(50) NOT NULL,
    title character varying(200) NOT NULL,
    description text,
    root_component_id bigint,
    severity character varying(20) NOT NULL,
    incident_status character varying(30) DEFAULT 'OPEN'::character varying NOT NULL,
    started_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    resolved_at timestamp without time zone,
    created_by bigint,
    inventory_completeness character varying(30) DEFAULT 'UNKNOWN'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_incident_resolution_date CHECK (((resolved_at IS NULL) OR (resolved_at >= started_at))),
    CONSTRAINT chk_incident_severity CHECK (((severity)::text = ANY ((ARRAY['CRITICAL'::character varying, 'HIGH'::character varying, 'MEDIUM'::character varying, 'LOW'::character varying])::text[]))),
    CONSTRAINT chk_incident_status CHECK (((incident_status)::text = ANY ((ARRAY['OPEN'::character varying, 'INVESTIGATING'::character varying, 'IN_PROGRESS'::character varying, 'RESOLVED'::character varying, 'CLOSED'::character varying])::text[]))),
    CONSTRAINT chk_inventory_completeness CHECK (((inventory_completeness)::text = ANY ((ARRAY['COMPLETE'::character varying, 'PARTIAL'::character varying, 'UNKNOWN'::character varying])::text[])))
);


--
-- Name: incidents_incident_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.incidents ALTER COLUMN incident_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.incidents_incident_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: restoration_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.restoration_history (
    restoration_id bigint NOT NULL,
    incident_id bigint NOT NULL,
    component_id bigint,
    performed_by bigint,
    action_description text NOT NULL,
    previous_status character varying(30),
    new_status character varying(30),
    restoration_status character varying(30) DEFAULT 'IN_PROGRESS'::character varying NOT NULL,
    performed_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    verified_by bigint,
    verified_at timestamp without time zone,
    verification_notes text,
    CONSTRAINT chk_restoration_status CHECK (((restoration_status)::text = ANY ((ARRAY['PENDING'::character varying, 'IN_PROGRESS'::character varying, 'VERIFICATION_PENDING'::character varying, 'VERIFICATION_REJECTED'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying])::text[])))
);


--
-- Name: restoration_history_restoration_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.restoration_history ALTER COLUMN restoration_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.restoration_history_restoration_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.roles (
    role_id bigint NOT NULL,
    role_name character varying(50) NOT NULL,
    description text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: roles_role_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.roles ALTER COLUMN role_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.roles_role_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: service_components; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.service_components (
    service_component_id bigint NOT NULL,
    service_id bigint NOT NULL,
    component_id bigint NOT NULL,
    is_critical boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: service_components_service_component_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.service_components ALTER COLUMN service_component_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.service_components_service_component_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: services; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.services (
    service_id bigint NOT NULL,
    service_code character varying(50) NOT NULL,
    service_name character varying(150) NOT NULL,
    service_type character varying(50),
    estimated_customer_count bigint DEFAULT 0 NOT NULL,
    status character varying(30) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_customer_count CHECK ((estimated_customer_count >= 0)),
    CONSTRAINT chk_service_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'DEGRADED'::character varying, 'DOWN'::character varying, 'INACTIVE'::character varying])::text[])))
);


--
-- Name: services_service_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.services ALTER COLUMN service_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.services_service_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    user_id bigint NOT NULL,
    full_name character varying(100) NOT NULL,
    email character varying(150) NOT NULL,
    password_hash text NOT NULL,
    role_id bigint NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: users_user_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.users ALTER COLUMN user_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.users_user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: alarms alarms_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alarms
    ADD CONSTRAINT alarms_pkey PRIMARY KEY (alarm_id);


--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (audit_id);


--
-- Name: component_dependencies component_dependencies_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.component_dependencies
    ADD CONSTRAINT component_dependencies_pkey PRIMARY KEY (dependency_id);


--
-- Name: components components_component_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.components
    ADD CONSTRAINT components_component_code_key UNIQUE (component_code);


--
-- Name: components components_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.components
    ADD CONSTRAINT components_pkey PRIMARY KEY (component_id);


--
-- Name: failure_events failure_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.failure_events
    ADD CONSTRAINT failure_events_pkey PRIMARY KEY (failure_event_id);


--
-- Name: incident_affected_components incident_affected_components_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incident_affected_components
    ADD CONSTRAINT incident_affected_components_pkey PRIMARY KEY (incident_affected_component_id);


--
-- Name: incident_affected_services incident_affected_services_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incident_affected_services
    ADD CONSTRAINT incident_affected_services_pkey PRIMARY KEY (incident_affected_service_id);


--
-- Name: incident_alarms incident_alarms_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incident_alarms
    ADD CONSTRAINT incident_alarms_pkey PRIMARY KEY (incident_id, alarm_id);


--
-- Name: incidents incidents_incident_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incidents
    ADD CONSTRAINT incidents_incident_code_key UNIQUE (incident_code);


--
-- Name: incidents incidents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incidents
    ADD CONSTRAINT incidents_pkey PRIMARY KEY (incident_id);


--
-- Name: restoration_history restoration_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.restoration_history
    ADD CONSTRAINT restoration_history_pkey PRIMARY KEY (restoration_id);


--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (role_id);


--
-- Name: roles roles_role_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_role_name_key UNIQUE (role_name);


--
-- Name: service_components service_components_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_components
    ADD CONSTRAINT service_components_pkey PRIMARY KEY (service_component_id);


--
-- Name: services services_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.services
    ADD CONSTRAINT services_pkey PRIMARY KEY (service_id);


--
-- Name: services services_service_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.services
    ADD CONSTRAINT services_service_code_key UNIQUE (service_code);


--
-- Name: component_dependencies uq_component_dependency; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.component_dependencies
    ADD CONSTRAINT uq_component_dependency UNIQUE (source_component_id, dependent_component_id);


--
-- Name: incident_affected_components uq_incident_affected_component; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incident_affected_components
    ADD CONSTRAINT uq_incident_affected_component UNIQUE (incident_id, component_id);


--
-- Name: incident_affected_services uq_incident_affected_service; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incident_affected_services
    ADD CONSTRAINT uq_incident_affected_service UNIQUE (incident_id, service_id);


--
-- Name: service_components uq_service_component; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_components
    ADD CONSTRAINT uq_service_component UNIQUE (service_id, component_id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (user_id);


--
-- Name: idx_affected_components_incident; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_affected_components_incident ON public.incident_affected_components USING btree (incident_id);


--
-- Name: idx_affected_services_incident; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_affected_services_incident ON public.incident_affected_services USING btree (incident_id);


--
-- Name: idx_alarms_component; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_alarms_component ON public.alarms USING btree (component_id);


--
-- Name: idx_alarms_occurred_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_alarms_occurred_at ON public.alarms USING btree (occurred_at DESC);


--
-- Name: idx_alarms_severity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_alarms_severity ON public.alarms USING btree (severity);


--
-- Name: idx_alarms_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_alarms_status ON public.alarms USING btree (alarm_status);


--
-- Name: idx_audit_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_created_at ON public.audit_logs USING btree (created_at DESC);


--
-- Name: idx_audit_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_user ON public.audit_logs USING btree (user_id);


--
-- Name: idx_components_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_components_status ON public.components USING btree (status);


--
-- Name: idx_components_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_components_type ON public.components USING btree (component_type);


--
-- Name: idx_dependency_dependent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dependency_dependent ON public.component_dependencies USING btree (dependent_component_id);


--
-- Name: idx_dependency_source; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dependency_source ON public.component_dependencies USING btree (source_component_id);


--
-- Name: idx_incidents_severity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_severity ON public.incidents USING btree (severity);


--
-- Name: idx_incidents_started_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_started_at ON public.incidents USING btree (started_at DESC);


--
-- Name: idx_incidents_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_status ON public.incidents USING btree (incident_status);


--
-- Name: idx_restoration_incident; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_restoration_incident ON public.restoration_history USING btree (incident_id);


--
-- Name: idx_service_components_component; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_service_components_component ON public.service_components USING btree (component_id);


--
-- Name: idx_users_role_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_role_id ON public.users USING btree (role_id);


--
-- Name: incident_affected_components fk_affected_component_component; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incident_affected_components
    ADD CONSTRAINT fk_affected_component_component FOREIGN KEY (component_id) REFERENCES public.components(component_id);


--
-- Name: incident_affected_components fk_affected_component_incident; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incident_affected_components
    ADD CONSTRAINT fk_affected_component_incident FOREIGN KEY (incident_id) REFERENCES public.incidents(incident_id) ON DELETE CASCADE;


--
-- Name: incident_affected_services fk_affected_service_incident; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incident_affected_services
    ADD CONSTRAINT fk_affected_service_incident FOREIGN KEY (incident_id) REFERENCES public.incidents(incident_id) ON DELETE CASCADE;


--
-- Name: incident_affected_services fk_affected_service_service; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incident_affected_services
    ADD CONSTRAINT fk_affected_service_service FOREIGN KEY (service_id) REFERENCES public.services(service_id);


--
-- Name: alarms fk_alarm_component; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alarms
    ADD CONSTRAINT fk_alarm_component FOREIGN KEY (component_id) REFERENCES public.components(component_id);


--
-- Name: audit_logs fk_audit_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE SET NULL;


--
-- Name: component_dependencies fk_dependency_dependent; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.component_dependencies
    ADD CONSTRAINT fk_dependency_dependent FOREIGN KEY (dependent_component_id) REFERENCES public.components(component_id);


--
-- Name: component_dependencies fk_dependency_source; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.component_dependencies
    ADD CONSTRAINT fk_dependency_source FOREIGN KEY (source_component_id) REFERENCES public.components(component_id);


--
-- Name: failure_events fk_failure_component; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.failure_events
    ADD CONSTRAINT fk_failure_component FOREIGN KEY (failed_component_id) REFERENCES public.components(component_id);


--
-- Name: failure_events fk_failure_event_alarm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.failure_events
    ADD CONSTRAINT fk_failure_event_alarm FOREIGN KEY (alarm_id) REFERENCES public.alarms(alarm_id);


--
-- Name: failure_events fk_failure_event_incident; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.failure_events
    ADD CONSTRAINT fk_failure_event_incident FOREIGN KEY (incident_id) REFERENCES public.incidents(incident_id);


--
-- Name: incident_alarms fk_incident_alarm_alarm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incident_alarms
    ADD CONSTRAINT fk_incident_alarm_alarm FOREIGN KEY (alarm_id) REFERENCES public.alarms(alarm_id) ON DELETE CASCADE;


--
-- Name: incident_alarms fk_incident_alarm_incident; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incident_alarms
    ADD CONSTRAINT fk_incident_alarm_incident FOREIGN KEY (incident_id) REFERENCES public.incidents(incident_id) ON DELETE CASCADE;


--
-- Name: incidents fk_incident_creator; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incidents
    ADD CONSTRAINT fk_incident_creator FOREIGN KEY (created_by) REFERENCES public.users(user_id);


--
-- Name: incidents fk_incident_root_component; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incidents
    ADD CONSTRAINT fk_incident_root_component FOREIGN KEY (root_component_id) REFERENCES public.components(component_id);


--
-- Name: restoration_history fk_restoration_component; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.restoration_history
    ADD CONSTRAINT fk_restoration_component FOREIGN KEY (component_id) REFERENCES public.components(component_id);


--
-- Name: restoration_history fk_restoration_incident; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.restoration_history
    ADD CONSTRAINT fk_restoration_incident FOREIGN KEY (incident_id) REFERENCES public.incidents(incident_id) ON DELETE CASCADE;


--
-- Name: restoration_history fk_restoration_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.restoration_history
    ADD CONSTRAINT fk_restoration_user FOREIGN KEY (performed_by) REFERENCES public.users(user_id);


--
-- Name: restoration_history fk_restoration_verified_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.restoration_history
    ADD CONSTRAINT fk_restoration_verified_by FOREIGN KEY (verified_by) REFERENCES public.users(user_id);


--
-- Name: service_components fk_service_component_component; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_components
    ADD CONSTRAINT fk_service_component_component FOREIGN KEY (component_id) REFERENCES public.components(component_id);


--
-- Name: service_components fk_service_component_service; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_components
    ADD CONSTRAINT fk_service_component_service FOREIGN KEY (service_id) REFERENCES public.services(service_id) ON DELETE CASCADE;


--
-- Name: users fk_users_role; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES public.roles(role_id);


--
-- PostgreSQL database dump complete
--

COMMIT;

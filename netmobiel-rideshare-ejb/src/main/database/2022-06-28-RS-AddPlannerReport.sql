-- RS: Add the PlannerReport

create sequence planner_report_id_seq start 50 increment 1;
ALTER SEQUENCE public.planner_report_id_seq
    OWNER TO rideshare;
create sequence plan_request_id_seq start 50 increment 1;
ALTER SEQUENCE public.plan_request_id_seq
    OWNER TO rideshare;

CREATE TABLE public.plan_request
(
    id bigint NOT NULL,
    creation_time timestamp without time zone NOT NULL,
    earliest_departure_time timestamp without time zone,
    from_label character varying(256) COLLATE pg_catalog."default",
    from_point geometry NOT NULL,
    latest_arrival_time timestamp without time zone,
    max_walk_distance integer,
    max_results integer,
    nr_seats integer,
    request_duration bigint,
    request_time timestamp without time zone NOT NULL,
    requestor bigint NOT NULL,
    to_label character varying(256) COLLATE pg_catalog."default",
    to_point geometry NOT NULL,
    travel_time timestamp without time zone,
    use_as_arrival_time boolean,
    CONSTRAINT plan_request_pkey PRIMARY KEY (id),
    CONSTRAINT plan_request_requestor_fk FOREIGN KEY (requestor)
        REFERENCES public.rs_user (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.plan_request
    OWNER to rideshare
;


CREATE TABLE public.planner_report
(
    id bigint NOT NULL,
    error_text character varying(512) COLLATE pg_catalog."default",
    error_vendor_code character varying(64) COLLATE pg_catalog."default",
    execution_time bigint,
    from_label character varying(256) COLLATE pg_catalog."default",
    from_point geometry NOT NULL,
    lenient_search boolean,
    max_detour_meters integer,
    max_detour_seconds integer,
    max_results integer,
    nr_results integer,
    plan_request bigint NOT NULL,
    rejected boolean,
    rejection_reason character varying(256) COLLATE pg_catalog."default",
    request_geometry geometry NOT NULL,
    status_code integer,
    to_label character varying(256) COLLATE pg_catalog."default",
    to_point geometry NOT NULL,
    tool_type character varying(3) COLLATE pg_catalog."default" NOT NULL,
    travel_time timestamp without time zone,
    use_as_arrival_time boolean,
    CONSTRAINT planner_report_pkey PRIMARY KEY (id),
    CONSTRAINT planner_report_plan_request_fk FOREIGN KEY (plan_request)
        REFERENCES public.plan_request (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.planner_report
    OWNER to rideshare
;    
    
CREATE TABLE public.report_via
(
    report_id bigint NOT NULL,
    label character varying(256) COLLATE pg_catalog."default",
    point geometry,
    CONSTRAINT via_report_fk FOREIGN KEY (report_id)
        REFERENCES public.planner_report (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.report_via
    OWNER to rideshare
;


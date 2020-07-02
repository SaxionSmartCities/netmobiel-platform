-- Add TripPlan data structure in database

create sequence itinerary_id_seq start 50 increment 1;
ALTER SEQUENCE public.itinerary_id_seq
    OWNER TO planner;
create sequence planner_report_id_seq start 50 increment 1;
ALTER SEQUENCE public.planner_report_id_seq
    OWNER TO planner;
create sequence trip_plan_id_seq start 50 increment 1;
ALTER SEQUENCE public.trip_plan_id_seq
    OWNER TO planner;

CREATE TABLE public.trip_plan
(
    id bigint NOT NULL,
    creation_time timestamp without time zone NOT NULL,
    earliest_departure_time timestamp without time zone,
    first_leg_rs boolean,
    from_label character varying(256) COLLATE pg_catalog."default",
    from_point geometry NOT NULL,
    last_leg_rs boolean,
    latest_arrival_time timestamp without time zone,
    max_walk_distance integer,
    nr_seats integer,
    plan_type character varying(3) COLLATE pg_catalog."default",
    request_duration bigint,
    request_time timestamp without time zone NOT NULL,
    to_label character varying(256) COLLATE pg_catalog."default",
    to_point geometry NOT NULL,
    travel_time timestamp without time zone NOT NULL,
    use_as_arrival_time boolean,
    traveller bigint NOT NULL,
    CONSTRAINT trip_plan_pkey PRIMARY KEY (id),
    CONSTRAINT trip_plan_traveller_fk FOREIGN KEY (traveller)
        REFERENCES public.pl_user (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.trip_plan
    OWNER to planner
;

CREATE TABLE public.itinerary
(
    id bigint NOT NULL,
    arrival_time timestamp without time zone NOT NULL,
    departure_time timestamp without time zone NOT NULL,
    duration integer,
    score double precision,
    transfers integer,
    transit_time integer,
    waiting_time integer,
    walk_distance integer,
    walk_time integer,
    trip_plan bigint,
    itinerary_ix integer,
    CONSTRAINT itinerary_pkey PRIMARY KEY (id),
    CONSTRAINT itinerary_trip_plan_fk FOREIGN KEY (trip_plan)
        REFERENCES public.trip_plan (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;
ALTER TABLE public.itinerary
    OWNER to planner
;


CREATE TABLE public.plan_traverse_mode
(
    plan_id bigint NOT NULL,
    traverse_mode character varying(2) COLLATE pg_catalog."default",
    CONSTRAINT traverse_mode_trip_plan_fk FOREIGN KEY (plan_id)
        REFERENCES public.trip_plan (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.plan_traverse_mode
    OWNER to planner
;

CREATE TABLE public.planner_report
(
    id bigint NOT NULL,
    creation_time timestamp without time zone NOT NULL,
    earliest_departure_time timestamp without time zone,
    error_text character varying(512) COLLATE pg_catalog."default",
    error_vendor_code character varying(64) COLLATE pg_catalog."default",
    execution_time bigint,
    from_label character varying(256) COLLATE pg_catalog."default",
    from_point geometry NOT NULL,
    latest_arrival_time timestamp without time zone,
    lenient_search boolean,
    max_detour_meters integer,
    max_detour_seconds integer,
    max_results integer,
    max_walk_distance integer,
    nr_itineraries integer,
    nr_seats integer,
    rejected boolean,
    rejection_reason character varying(256) COLLATE pg_catalog."default",
    request_geometry geometry NOT NULL,
    request_time timestamp without time zone NOT NULL,
    start_position integer,
    status_code integer,
    to_label character varying(256) COLLATE pg_catalog."default",
    to_point geometry NOT NULL,
    tool_type character varying(3) COLLATE pg_catalog."default" NOT NULL,
    travel_time timestamp without time zone NOT NULL,
    use_as_arrival_time boolean,
    plan bigint NOT NULL,
    CONSTRAINT planner_report_pkey PRIMARY KEY (id),
    CONSTRAINT planner_report_plan_fk FOREIGN KEY (plan)
        REFERENCES public.trip_plan (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.planner_report
    OWNER to planner
;    
    
CREATE TABLE public.report_traverse_mode
(
    report_id bigint NOT NULL,
    traverse_mode character varying(2) COLLATE pg_catalog."default",
    CONSTRAINT traverse_mode_report_fk FOREIGN KEY (report_id)
        REFERENCES public.planner_report (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.report_traverse_mode
    OWNER to planner
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
    OWNER to planner
;

alter table public.leg 
	ADD COLUMN report bigint,
	ADD COLUMN itinerary bigint;
;
alter table public.leg add constraint leg_report_fk foreign key (report) references public.planner_report;
alter table public.leg add constraint leg_itinerary_fk foreign key (itinerary) references public.itinerary;

alter table public.trip add column itinerary bigint;
alter table public.trip add constraint trip_itinerary_fk foreign key (itinerary) references public.itinerary;
alter table public.trip add constraint trip_itinerary_uc unique (itinerary);

-- Split the itinerary from the trip by duplication, there is a one-to-one relation between trip and itineray.
-- Simply assign the trip id to the itinerary id.
-- Also update the itinerary sequence. 

INSERT INTO itinerary (id, arrival_time, departure_time, duration, score, 
	transfers, transit_time, waiting_time, walk_distance, walk_time, trip_plan, itinerary_ix)
	SELECT id, arrival_time, departure_time, duration, 0, 
	transfers, transit_time, waiting_time, walk_distance, walk_time, null, 0 from trip;

SELECT setval('itinerary_id_seq', (SELECT MAX(id) from public.itinerary), TRUE);

UPDATE leg SET itinerary = trip;
-- Trip id and itinerary id are the same according the insert above.
UPDATE trip SET itinerary = id;

ALTER TABLE public.stop
	DROP CONSTRAINT stop_trip_fk
;
ALTER TABLE public.stop
	RENAME COLUMN trip TO itinerary
;
ALTER TABLE public.stop ADD CONSTRAINT stop_itinerary_fk FOREIGN KEY (itinerary) references public.itinerary;

-- Decouple the legs from the trip
ALTER TABLE public.leg
	DROP CONSTRAINT leg_trip_fk,
	DROP COLUMN trip
;

-- Drop the itinerary columhns from the trip
ALTER TABLE public.trip
	DROP COLUMN arrival_time,
    DROP COLUMN departure_time,
    DROP COLUMN duration,
    DROP COLUMN transfers,
    DROP COLUMN transit_time,
    DROP COLUMN waiting_time,
    DROP COLUMN walk_distance,
    DROP COLUMN walk_time
;
	
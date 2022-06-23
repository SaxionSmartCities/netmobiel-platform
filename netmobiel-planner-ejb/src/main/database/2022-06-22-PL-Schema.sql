--
-- PostgreSQL database dump
--

-- Dumped from database version 10.18
-- Dumped by pg_dump version 10.18

-- Started on 2022-06-23 09:30:48

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 10 (class 2615 OID 64102)
-- Name: topology; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA topology;


ALTER SCHEMA topology OWNER TO postgres;

--
-- TOC entry 4486 (class 0 OID 0)
-- Dependencies: 10
-- Name: SCHEMA topology; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON SCHEMA topology IS 'PostGIS Topology schema';


--
-- TOC entry 1 (class 3079 OID 12924)
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- TOC entry 4487 (class 0 OID 0)
-- Dependencies: 1
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


--
-- TOC entry 2 (class 3079 OID 62603)
-- Name: postgis; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;


--
-- TOC entry 4488 (class 0 OID 0)
-- Dependencies: 2
-- Name: EXTENSION postgis; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION postgis IS 'PostGIS geometry, geography, and raster spatial types and functions';


--
-- TOC entry 3 (class 3079 OID 64103)
-- Name: postgis_topology; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS postgis_topology WITH SCHEMA topology;


--
-- TOC entry 4489 (class 0 OID 0)
-- Dependencies: 3
-- Name: EXTENSION postgis_topology; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION postgis_topology IS 'PostGIS topology spatial types and functions';


SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 220 (class 1259 OID 83390)
-- Name: guide_step; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.guide_step (
    leg_id bigint NOT NULL,
    absolute_direction character varying(2),
    is_area boolean,
    is_bogus_name boolean,
    distance integer,
    exit character varying(16),
    latitude double precision,
    longitude double precision,
    name character varying(128),
    relative_direction character varying(3),
    stay_on boolean,
    step_ix integer NOT NULL
);


ALTER TABLE public.guide_step OWNER TO planner;

--
-- TOC entry 239 (class 1259 OID 199417)
-- Name: itinerary; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.itinerary (
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
    fare_credits integer
);


ALTER TABLE public.itinerary OWNER TO planner;

--
-- TOC entry 235 (class 1259 OID 199393)
-- Name: itinerary_id_seq; Type: SEQUENCE; Schema: public; Owner: planner
--

CREATE SEQUENCE public.itinerary_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.itinerary_id_seq OWNER TO planner;

--
-- TOC entry 221 (class 1259 OID 83393)
-- Name: leg; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.leg (
    id bigint NOT NULL,
    agency_id character varying(32),
    agency_name character varying(32),
    agency_time_zone_offset integer,
    distance integer,
    driver_id character varying(64),
    driver_name character varying(64),
    duration integer,
    headsign character varying(48),
    leg_geometry public.geometry,
    route_id character varying(32),
    route_long_name character varying(96),
    route_short_name character varying(32),
    route_type integer,
    state character varying(3),
    traverse_mode character varying(2),
    trip_id character varying(32),
    vehicle_id character varying(32),
    vehicle_license_plate character varying(16),
    vehicle_name character varying(64),
    from_stop bigint NOT NULL,
    to_stop bigint NOT NULL,
    leg_ix integer,
    booking_id character varying(32),
    booking_required boolean,
    report bigint,
    itinerary bigint,
    fare_credits integer,
    confirmation_req boolean DEFAULT false NOT NULL,
    confirmed boolean,
    confirmation_prov_req boolean DEFAULT false NOT NULL,
    confirmed_prov boolean,
    payment_state character varying(1),
    payment_id character varying(32),
    conf_reason character varying(3),
    conf_reason_prov character varying(3),
    cancelled_by_provider boolean,
    shout_out_ref character varying(32),
    booking_confirmed boolean
);


ALTER TABLE public.leg OWNER TO planner;

--
-- TOC entry 222 (class 1259 OID 83399)
-- Name: leg_id_seq; Type: SEQUENCE; Schema: public; Owner: planner
--

CREATE SEQUENCE public.leg_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.leg_id_seq OWNER TO planner;

--
-- TOC entry 223 (class 1259 OID 83401)
-- Name: otp_cluster; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.otp_cluster (
    id character varying(64) NOT NULL,
    gtfs_id character varying(64),
    label character varying(128),
    point public.geometry,
    nr_routes integer,
    nr_stops integer,
    transportation_types integer,
    stale boolean DEFAULT false NOT NULL
);


ALTER TABLE public.otp_cluster OWNER TO planner;

--
-- TOC entry 234 (class 1259 OID 164163)
-- Name: otp_cluster_export; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.otp_cluster_export (
    id character varying(64),
    gtfs_id character varying(64),
    label character varying(128),
    point public.geometry,
    nr_routes integer,
    nr_stops integer,
    transportation_types integer
);


ALTER TABLE public.otp_cluster_export OWNER TO postgres;

--
-- TOC entry 224 (class 1259 OID 83407)
-- Name: otp_route; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.otp_route (
    id character varying(64) NOT NULL,
    gtfs_id character varying(64),
    long_name character varying(96),
    short_name character varying(32),
    ov_type integer NOT NULL,
    stale boolean DEFAULT false NOT NULL
);


ALTER TABLE public.otp_route OWNER TO planner;

--
-- TOC entry 225 (class 1259 OID 83410)
-- Name: otp_route_stop; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.otp_route_stop (
    route_id character varying(64) NOT NULL,
    stop_id character varying(64) NOT NULL
);


ALTER TABLE public.otp_route_stop OWNER TO planner;

--
-- TOC entry 226 (class 1259 OID 83413)
-- Name: otp_stop; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.otp_stop (
    id character varying(64) NOT NULL,
    gtfs_id character varying(64),
    label character varying(128),
    point public.geometry,
    nr_routes integer,
    platform_code character varying(32),
    transportation_types integer,
    cluster character varying(64),
    stale boolean DEFAULT false NOT NULL
);


ALTER TABLE public.otp_stop OWNER TO planner;

--
-- TOC entry 227 (class 1259 OID 83419)
-- Name: otp_transfer; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.otp_transfer (
    from_stop character varying(255) NOT NULL,
    to_stop character varying(255) NOT NULL,
    distance integer NOT NULL
);


ALTER TABLE public.otp_transfer OWNER TO planner;

--
-- TOC entry 228 (class 1259 OID 83425)
-- Name: pl_user; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.pl_user (
    id bigint NOT NULL,
    family_name character varying(64),
    given_name character varying(32),
    managed_identity character varying(36) NOT NULL,
    email character varying(64)
);


ALTER TABLE public.pl_user OWNER TO planner;

--
-- TOC entry 240 (class 1259 OID 199427)
-- Name: plan_traverse_mode; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.plan_traverse_mode (
    plan_id bigint NOT NULL,
    traverse_mode character varying(2)
);


ALTER TABLE public.plan_traverse_mode OWNER TO planner;

--
-- TOC entry 241 (class 1259 OID 199435)
-- Name: planner_report; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.planner_report (
    id bigint NOT NULL,
    creation_time timestamp without time zone NOT NULL,
    earliest_departure_time timestamp without time zone,
    error_text character varying(512),
    error_vendor_code character varying(64),
    execution_time bigint NOT NULL,
    from_label character varying(256),
    from_point public.geometry NOT NULL,
    latest_arrival_time timestamp without time zone,
    lenient_search boolean,
    max_detour_meters integer,
    max_detour_seconds integer,
    max_results integer,
    max_walk_distance integer,
    nr_itineraries integer,
    nr_seats integer,
    rejected boolean,
    rejection_reason character varying(256),
    request_geometry public.geometry NOT NULL,
    request_time timestamp without time zone NOT NULL,
    start_position integer,
    status_code integer,
    to_label character varying(256),
    to_point public.geometry NOT NULL,
    tool_type character varying(3) NOT NULL,
    travel_time timestamp without time zone NOT NULL,
    use_as_arrival_time boolean,
    plan bigint NOT NULL
);


ALTER TABLE public.planner_report OWNER TO planner;

--
-- TOC entry 236 (class 1259 OID 199395)
-- Name: planner_report_id_seq; Type: SEQUENCE; Schema: public; Owner: planner
--

CREATE SEQUENCE public.planner_report_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.planner_report_id_seq OWNER TO planner;

--
-- TOC entry 242 (class 1259 OID 199448)
-- Name: report_traverse_mode; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.report_traverse_mode (
    report_id bigint NOT NULL,
    traverse_mode character varying(2)
);


ALTER TABLE public.report_traverse_mode OWNER TO planner;

--
-- TOC entry 243 (class 1259 OID 199456)
-- Name: report_via; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.report_via (
    report_id bigint NOT NULL,
    label character varying(256),
    point public.geometry
);


ALTER TABLE public.report_via OWNER TO planner;

--
-- TOC entry 229 (class 1259 OID 83428)
-- Name: stop; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.stop (
    id bigint NOT NULL,
    arrival_time timestamp without time zone,
    departure_time timestamp without time zone,
    label character varying(256),
    point public.geometry,
    platform_code character varying(32),
    stop_code character varying(32),
    stop_id character varying(32),
    itinerary bigint NOT NULL,
    stop_ix integer
);


ALTER TABLE public.stop OWNER TO planner;

--
-- TOC entry 230 (class 1259 OID 83434)
-- Name: stop_id_seq; Type: SEQUENCE; Schema: public; Owner: planner
--

CREATE SEQUENCE public.stop_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.stop_id_seq OWNER TO planner;

--
-- TOC entry 231 (class 1259 OID 83436)
-- Name: trip; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.trip (
    id bigint NOT NULL,
    cancel_reason character varying(256),
    deleted boolean,
    from_label character varying(256),
    from_point public.geometry,
    state character varying(3),
    to_label character varying(256),
    to_point public.geometry,
    traveller bigint NOT NULL,
    nr_seats integer DEFAULT 1 NOT NULL,
    itinerary bigint,
    arrival_time_is_pinned boolean DEFAULT false,
    cancelled_by_provider boolean,
    departure_postal_code character varying(6),
    arrival_postal_code character varying(6),
    organizer integer NOT NULL,
    creation_time timestamp without time zone NOT NULL,
    reminder_count integer DEFAULT 0 NOT NULL,
    validation_exp_time timestamp without time zone,
    validation_rem_time timestamp without time zone,
    version integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.trip OWNER TO planner;

--
-- TOC entry 232 (class 1259 OID 83442)
-- Name: trip_id_seq; Type: SEQUENCE; Schema: public; Owner: planner
--

CREATE SEQUENCE public.trip_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.trip_id_seq OWNER TO planner;

--
-- TOC entry 238 (class 1259 OID 199404)
-- Name: trip_plan; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.trip_plan (
    id bigint NOT NULL,
    creation_time timestamp without time zone NOT NULL,
    earliest_departure_time timestamp without time zone,
    first_leg_rs boolean,
    from_label character varying(256),
    from_point public.geometry NOT NULL,
    last_leg_rs boolean,
    latest_arrival_time timestamp without time zone,
    max_walk_distance integer,
    nr_seats integer,
    plan_type character varying(3) NOT NULL,
    request_duration bigint,
    request_time timestamp without time zone NOT NULL,
    to_label character varying(256),
    to_point public.geometry NOT NULL,
    travel_time timestamp without time zone NOT NULL,
    use_as_arrival_time boolean,
    traveller bigint NOT NULL,
    requestor integer NOT NULL,
    geodesic_distance integer,
    reference_itinerary bigint,
    plan_state character varying(2) NOT NULL,
    version integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.trip_plan OWNER TO planner;

--
-- TOC entry 237 (class 1259 OID 199397)
-- Name: trip_plan_id_seq; Type: SEQUENCE; Schema: public; Owner: planner
--

CREATE SEQUENCE public.trip_plan_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.trip_plan_id_seq OWNER TO planner;

--
-- TOC entry 233 (class 1259 OID 83444)
-- Name: user_id_seq; Type: SEQUENCE; Schema: public; Owner: planner
--

CREATE SEQUENCE public.user_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.user_id_seq OWNER TO planner;

--
-- TOC entry 4312 (class 2606 OID 83447)
-- Name: pl_user cs_managed_identity_unique; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.pl_user
    ADD CONSTRAINT cs_managed_identity_unique UNIQUE (managed_identity);


--
-- TOC entry 4293 (class 2606 OID 83449)
-- Name: guide_step guide_step_pkey; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.guide_step
    ADD CONSTRAINT guide_step_pkey PRIMARY KEY (leg_id, step_ix);


--
-- TOC entry 4326 (class 2606 OID 199421)
-- Name: itinerary itinerary_pkey; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.itinerary
    ADD CONSTRAINT itinerary_pkey PRIMARY KEY (id);


--
-- TOC entry 4295 (class 2606 OID 83451)
-- Name: leg leg_pkey; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.leg
    ADD CONSTRAINT leg_pkey PRIMARY KEY (id);


--
-- TOC entry 4297 (class 2606 OID 83453)
-- Name: otp_cluster otp_cluster_gtfs_id_uc; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_cluster
    ADD CONSTRAINT otp_cluster_gtfs_id_uc UNIQUE (gtfs_id);


--
-- TOC entry 4299 (class 2606 OID 83455)
-- Name: otp_cluster otp_cluster_pkey; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_cluster
    ADD CONSTRAINT otp_cluster_pkey PRIMARY KEY (id);


--
-- TOC entry 4301 (class 2606 OID 83457)
-- Name: otp_route otp_route_gtfs_id_uc; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_route
    ADD CONSTRAINT otp_route_gtfs_id_uc UNIQUE (gtfs_id);


--
-- TOC entry 4303 (class 2606 OID 83459)
-- Name: otp_route otp_route_pkey; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_route
    ADD CONSTRAINT otp_route_pkey PRIMARY KEY (id);


--
-- TOC entry 4305 (class 2606 OID 83461)
-- Name: otp_stop otp_stop_gtfs_id_uc; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_stop
    ADD CONSTRAINT otp_stop_gtfs_id_uc UNIQUE (gtfs_id);


--
-- TOC entry 4307 (class 2606 OID 83463)
-- Name: otp_stop otp_stop_pkey; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_stop
    ADD CONSTRAINT otp_stop_pkey PRIMARY KEY (id);


--
-- TOC entry 4310 (class 2606 OID 83465)
-- Name: otp_transfer otp_transfer_pkey; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_transfer
    ADD CONSTRAINT otp_transfer_pkey PRIMARY KEY (from_stop, to_stop);


--
-- TOC entry 4314 (class 2606 OID 83467)
-- Name: pl_user pl_user_pkey; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.pl_user
    ADD CONSTRAINT pl_user_pkey PRIMARY KEY (id);


--
-- TOC entry 4328 (class 2606 OID 199442)
-- Name: planner_report planner_report_pkey; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.planner_report
    ADD CONSTRAINT planner_report_pkey PRIMARY KEY (id);


--
-- TOC entry 4316 (class 2606 OID 83469)
-- Name: stop stop_pkey; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.stop
    ADD CONSTRAINT stop_pkey PRIMARY KEY (id);


--
-- TOC entry 4318 (class 2606 OID 199483)
-- Name: trip trip_itinerary_uc; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.trip
    ADD CONSTRAINT trip_itinerary_uc UNIQUE (itinerary);


--
-- TOC entry 4320 (class 2606 OID 83471)
-- Name: trip trip_pkey; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.trip
    ADD CONSTRAINT trip_pkey PRIMARY KEY (id);


--
-- TOC entry 4322 (class 2606 OID 199411)
-- Name: trip_plan trip_plan_pkey; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.trip_plan
    ADD CONSTRAINT trip_plan_pkey PRIMARY KEY (id);


--
-- TOC entry 4324 (class 2606 OID 371410)
-- Name: trip_plan trip_plan_reference_itinerary_uc; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.trip_plan
    ADD CONSTRAINT trip_plan_reference_itinerary_uc UNIQUE (reference_itinerary);


--
-- TOC entry 4308 (class 1259 OID 83472)
-- Name: stop_cluster_ix; Type: INDEX; Schema: public; Owner: planner
--

CREATE INDEX stop_cluster_ix ON public.otp_stop USING btree (cluster);


--
-- TOC entry 4346 (class 2606 OID 199422)
-- Name: itinerary itinerary_trip_plan_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.itinerary
    ADD CONSTRAINT itinerary_trip_plan_fk FOREIGN KEY (trip_plan) REFERENCES public.trip_plan(id);


--
-- TOC entry 4330 (class 2606 OID 83473)
-- Name: leg leg_from_stop_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.leg
    ADD CONSTRAINT leg_from_stop_fk FOREIGN KEY (from_stop) REFERENCES public.stop(id);


--
-- TOC entry 4333 (class 2606 OID 199472)
-- Name: leg leg_itinerary_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.leg
    ADD CONSTRAINT leg_itinerary_fk FOREIGN KEY (itinerary) REFERENCES public.itinerary(id);


--
-- TOC entry 4332 (class 2606 OID 199467)
-- Name: leg leg_report_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.leg
    ADD CONSTRAINT leg_report_fk FOREIGN KEY (report) REFERENCES public.planner_report(id);


--
-- TOC entry 4331 (class 2606 OID 83478)
-- Name: leg leg_to_stop_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.leg
    ADD CONSTRAINT leg_to_stop_fk FOREIGN KEY (to_stop) REFERENCES public.stop(id);


--
-- TOC entry 4334 (class 2606 OID 292716)
-- Name: otp_route_stop otp_route_stop_route_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_route_stop
    ADD CONSTRAINT otp_route_stop_route_fk FOREIGN KEY (route_id) REFERENCES public.otp_route(id) ON DELETE CASCADE;


--
-- TOC entry 4335 (class 2606 OID 292721)
-- Name: otp_route_stop otp_route_stop_stop_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_route_stop
    ADD CONSTRAINT otp_route_stop_stop_fk FOREIGN KEY (stop_id) REFERENCES public.otp_stop(id) ON DELETE CASCADE;


--
-- TOC entry 4336 (class 2606 OID 83498)
-- Name: otp_stop otp_stop_cluster_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_stop
    ADD CONSTRAINT otp_stop_cluster_fk FOREIGN KEY (cluster) REFERENCES public.otp_cluster(id);


--
-- TOC entry 4337 (class 2606 OID 292726)
-- Name: otp_transfer otp_transfer_from_stop_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_transfer
    ADD CONSTRAINT otp_transfer_from_stop_fk FOREIGN KEY (from_stop) REFERENCES public.otp_stop(id) ON DELETE CASCADE;


--
-- TOC entry 4338 (class 2606 OID 292731)
-- Name: otp_transfer otp_transfer_to_stop_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_transfer
    ADD CONSTRAINT otp_transfer_to_stop_fk FOREIGN KEY (to_stop) REFERENCES public.otp_stop(id) ON DELETE CASCADE;


--
-- TOC entry 4348 (class 2606 OID 199443)
-- Name: planner_report planner_report_plan_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.planner_report
    ADD CONSTRAINT planner_report_plan_fk FOREIGN KEY (plan) REFERENCES public.trip_plan(id);


--
-- TOC entry 4329 (class 2606 OID 83513)
-- Name: guide_step step_leg_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.guide_step
    ADD CONSTRAINT step_leg_fk FOREIGN KEY (leg_id) REFERENCES public.leg(id);


--
-- TOC entry 4339 (class 2606 OID 199484)
-- Name: stop stop_itinerary_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.stop
    ADD CONSTRAINT stop_itinerary_fk FOREIGN KEY (itinerary) REFERENCES public.itinerary(id);


--
-- TOC entry 4349 (class 2606 OID 199451)
-- Name: report_traverse_mode traverse_mode_report_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.report_traverse_mode
    ADD CONSTRAINT traverse_mode_report_fk FOREIGN KEY (report_id) REFERENCES public.planner_report(id) ON DELETE CASCADE;


--
-- TOC entry 4347 (class 2606 OID 199430)
-- Name: plan_traverse_mode traverse_mode_trip_plan_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.plan_traverse_mode
    ADD CONSTRAINT traverse_mode_trip_plan_fk FOREIGN KEY (plan_id) REFERENCES public.trip_plan(id) ON DELETE CASCADE;


--
-- TOC entry 4341 (class 2606 OID 199477)
-- Name: trip trip_itinerary_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.trip
    ADD CONSTRAINT trip_itinerary_fk FOREIGN KEY (itinerary) REFERENCES public.itinerary(id);


--
-- TOC entry 4342 (class 2606 OID 325569)
-- Name: trip trip_organizer_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.trip
    ADD CONSTRAINT trip_organizer_fk FOREIGN KEY (organizer) REFERENCES public.pl_user(id);


--
-- TOC entry 4345 (class 2606 OID 371411)
-- Name: trip_plan trip_plan_reference_itinerary_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.trip_plan
    ADD CONSTRAINT trip_plan_reference_itinerary_fk FOREIGN KEY (reference_itinerary) REFERENCES public.itinerary(id);


--
-- TOC entry 4344 (class 2606 OID 325564)
-- Name: trip_plan trip_plan_requestor_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.trip_plan
    ADD CONSTRAINT trip_plan_requestor_fk FOREIGN KEY (requestor) REFERENCES public.pl_user(id);


--
-- TOC entry 4343 (class 2606 OID 199412)
-- Name: trip_plan trip_plan_traveller_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.trip_plan
    ADD CONSTRAINT trip_plan_traveller_fk FOREIGN KEY (traveller) REFERENCES public.pl_user(id);


--
-- TOC entry 4340 (class 2606 OID 83523)
-- Name: trip trip_traveller_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.trip
    ADD CONSTRAINT trip_traveller_fk FOREIGN KEY (traveller) REFERENCES public.pl_user(id);


--
-- TOC entry 4350 (class 2606 OID 199462)
-- Name: report_via via_report_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.report_via
    ADD CONSTRAINT via_report_fk FOREIGN KEY (report_id) REFERENCES public.planner_report(id) ON DELETE CASCADE;


-- Completed on 2022-06-23 09:30:49

--
-- PostgreSQL database dump complete
--


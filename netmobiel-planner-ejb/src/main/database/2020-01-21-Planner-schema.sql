--
-- PostgreSQL database dump
--

-- Dumped from database version 10.10
-- Dumped by pg_dump version 10.10

-- Started on 2020-01-21 13:58:25

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

SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 224 (class 1259 OID 83258)
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
-- TOC entry 225 (class 1259 OID 83263)
-- Name: leg; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.leg (
    id bigint NOT NULL,
    agency_id character varying(32),
    agency_name character varying(32),
    agency_time_zone_offset integer,
    distance integer,
    driver_id character varying(32),
    driver_name character varying(64),
    duration integer,
    headsign character varying(32),
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
    trip bigint NOT NULL,
    leg_ix integer
);


ALTER TABLE public.leg OWNER TO planner;

--
-- TOC entry 220 (class 1259 OID 83250)
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
-- TOC entry 226 (class 1259 OID 83271)
-- Name: otp_cluster; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.otp_cluster (
    id character varying(64) NOT NULL,
    gtfs_id character varying(64),
    label character varying(128),
    point public.geometry,
    nr_routes integer,
    nr_stops integer,
    transportation_types integer
);


ALTER TABLE public.otp_cluster OWNER TO planner;

--
-- TOC entry 227 (class 1259 OID 83279)
-- Name: otp_route; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.otp_route (
    id character varying(64) NOT NULL,
    gtfs_id character varying(64),
    long_name character varying(96),
    short_name character varying(32),
    ov_type integer NOT NULL
);


ALTER TABLE public.otp_route OWNER TO planner;

--
-- TOC entry 228 (class 1259 OID 83284)
-- Name: otp_route_stop; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.otp_route_stop (
    route_id character varying(64) NOT NULL,
    stop_id character varying(64) NOT NULL
);


ALTER TABLE public.otp_route_stop OWNER TO planner;

--
-- TOC entry 229 (class 1259 OID 83287)
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
    cluster character varying(64)
);


ALTER TABLE public.otp_stop OWNER TO planner;

--
-- TOC entry 230 (class 1259 OID 83295)
-- Name: otp_transfer; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.otp_transfer (
    from_stop character varying(255) NOT NULL,
    to_stop character varying(255) NOT NULL,
    distance integer NOT NULL
);


ALTER TABLE public.otp_transfer OWNER TO planner;

--
-- TOC entry 231 (class 1259 OID 83303)
-- Name: pl_user; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.pl_user (
    id bigint NOT NULL,
    email character varying(64),
    family_name character varying(64),
    given_name character varying(32),
    managed_identity character varying(36) NOT NULL
);


ALTER TABLE public.pl_user OWNER TO planner;

--
-- TOC entry 232 (class 1259 OID 83308)
-- Name: stop; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.stop (
    id bigint NOT NULL,
    arrival_time timestamp without time zone,
    departure_time timestamp without time zone,
    label character varying(128),
    point public.geometry,
    platform_code character varying(32),
    stop_code character varying(32),
    stop_id character varying(32),
    trip bigint NOT NULL,
    stop_ix integer
);


ALTER TABLE public.stop OWNER TO planner;

--
-- TOC entry 221 (class 1259 OID 83252)
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
-- TOC entry 233 (class 1259 OID 83316)
-- Name: trip; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.trip (
    id bigint NOT NULL,
    arrival_time timestamp without time zone NOT NULL,
    departure_time timestamp without time zone NOT NULL,
    duration integer,
    transfers integer,
    transit_time integer,
    waiting_time integer,
    walk_distance integer,
    walk_time integer,
    cancel_reason character varying(256),
    deleted boolean,
    from_label character varying(255),
    from_point public.geometry,
    state character varying(3),
    to_label character varying(255),
    to_point public.geometry,
    traveller bigint NOT NULL
);


ALTER TABLE public.trip OWNER TO planner;

--
-- TOC entry 222 (class 1259 OID 83254)
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
-- TOC entry 223 (class 1259 OID 83256)
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
-- TOC entry 4253 (class 2606 OID 83332)
-- Name: pl_user cs_managed_identity_unique; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.pl_user
    ADD CONSTRAINT cs_managed_identity_unique UNIQUE (managed_identity);


--
-- TOC entry 4234 (class 2606 OID 83262)
-- Name: guide_step guide_step_pkey; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.guide_step
    ADD CONSTRAINT guide_step_pkey PRIMARY KEY (leg_id, step_ix);


--
-- TOC entry 4236 (class 2606 OID 83270)
-- Name: leg leg_pkey; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.leg
    ADD CONSTRAINT leg_pkey PRIMARY KEY (id);


--
-- TOC entry 4238 (class 2606 OID 83325)
-- Name: otp_cluster otp_cluster_gtfs_id_uc; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_cluster
    ADD CONSTRAINT otp_cluster_gtfs_id_uc UNIQUE (gtfs_id);


--
-- TOC entry 4240 (class 2606 OID 83278)
-- Name: otp_cluster otp_cluster_pkey; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_cluster
    ADD CONSTRAINT otp_cluster_pkey PRIMARY KEY (id);


--
-- TOC entry 4242 (class 2606 OID 83327)
-- Name: otp_route otp_route_gtfs_id_uc; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_route
    ADD CONSTRAINT otp_route_gtfs_id_uc UNIQUE (gtfs_id);


--
-- TOC entry 4244 (class 2606 OID 83283)
-- Name: otp_route otp_route_pkey; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_route
    ADD CONSTRAINT otp_route_pkey PRIMARY KEY (id);


--
-- TOC entry 4246 (class 2606 OID 83330)
-- Name: otp_stop otp_stop_gtfs_id_uc; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_stop
    ADD CONSTRAINT otp_stop_gtfs_id_uc UNIQUE (gtfs_id);


--
-- TOC entry 4248 (class 2606 OID 83294)
-- Name: otp_stop otp_stop_pkey; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_stop
    ADD CONSTRAINT otp_stop_pkey PRIMARY KEY (id);


--
-- TOC entry 4251 (class 2606 OID 83302)
-- Name: otp_transfer otp_transfer_pkey; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_transfer
    ADD CONSTRAINT otp_transfer_pkey PRIMARY KEY (from_stop, to_stop);


--
-- TOC entry 4255 (class 2606 OID 83307)
-- Name: pl_user pl_user_pkey; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.pl_user
    ADD CONSTRAINT pl_user_pkey PRIMARY KEY (id);


--
-- TOC entry 4257 (class 2606 OID 83315)
-- Name: stop stop_pkey; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.stop
    ADD CONSTRAINT stop_pkey PRIMARY KEY (id);


--
-- TOC entry 4259 (class 2606 OID 83323)
-- Name: trip trip_pkey; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.trip
    ADD CONSTRAINT trip_pkey PRIMARY KEY (id);


--
-- TOC entry 4249 (class 1259 OID 83328)
-- Name: stop_cluster_ix; Type: INDEX; Schema: public; Owner: planner
--

CREATE INDEX stop_cluster_ix ON public.otp_stop USING btree (cluster);


--
-- TOC entry 4261 (class 2606 OID 83338)
-- Name: leg leg_from_stop_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.leg
    ADD CONSTRAINT leg_from_stop_fk FOREIGN KEY (from_stop) REFERENCES public.stop(id);


--
-- TOC entry 4262 (class 2606 OID 83343)
-- Name: leg leg_to_stop_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.leg
    ADD CONSTRAINT leg_to_stop_fk FOREIGN KEY (to_stop) REFERENCES public.stop(id);


--
-- TOC entry 4263 (class 2606 OID 83348)
-- Name: leg leg_trip_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.leg
    ADD CONSTRAINT leg_trip_fk FOREIGN KEY (trip) REFERENCES public.trip(id);


--
-- TOC entry 4265 (class 2606 OID 83358)
-- Name: otp_route_stop otp_route_stop_route_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_route_stop
    ADD CONSTRAINT otp_route_stop_route_fk FOREIGN KEY (route_id) REFERENCES public.otp_route(id);


--
-- TOC entry 4264 (class 2606 OID 83353)
-- Name: otp_route_stop otp_route_stop_stop_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_route_stop
    ADD CONSTRAINT otp_route_stop_stop_fk FOREIGN KEY (stop_id) REFERENCES public.otp_stop(id) ON DELETE CASCADE;


--
-- TOC entry 4266 (class 2606 OID 83363)
-- Name: otp_stop otp_stop_cluster_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_stop
    ADD CONSTRAINT otp_stop_cluster_fk FOREIGN KEY (cluster) REFERENCES public.otp_cluster(id);


--
-- TOC entry 4267 (class 2606 OID 83368)
-- Name: otp_transfer otp_transfer_from_stop_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_transfer
    ADD CONSTRAINT otp_transfer_from_stop_fk FOREIGN KEY (from_stop) REFERENCES public.otp_stop(id);


--
-- TOC entry 4268 (class 2606 OID 83373)
-- Name: otp_transfer otp_transfer_to_stop_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_transfer
    ADD CONSTRAINT otp_transfer_to_stop_fk FOREIGN KEY (to_stop) REFERENCES public.otp_stop(id);


--
-- TOC entry 4260 (class 2606 OID 83333)
-- Name: guide_step step_leg_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.guide_step
    ADD CONSTRAINT step_leg_fk FOREIGN KEY (leg_id) REFERENCES public.leg(id);


--
-- TOC entry 4269 (class 2606 OID 83378)
-- Name: stop stop_trip_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.stop
    ADD CONSTRAINT stop_trip_fk FOREIGN KEY (trip) REFERENCES public.trip(id);


--
-- TOC entry 4270 (class 2606 OID 83383)
-- Name: trip trip_traveller_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.trip
    ADD CONSTRAINT trip_traveller_fk FOREIGN KEY (traveller) REFERENCES public.pl_user(id);


-- Completed on 2020-01-21 13:58:26

--
-- PostgreSQL database dump complete
--


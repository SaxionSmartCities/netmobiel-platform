--
-- PostgreSQL database dump
--

-- Dumped from database version 10.10
-- Dumped by pg_dump version 10.10

-- Started on 2020-06-03 08:37:32

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
-- TOC entry 7 (class 2615 OID 104662)
-- Name: topology; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA topology;


ALTER SCHEMA topology OWNER TO postgres;

--
-- TOC entry 4414 (class 0 OID 0)
-- Dependencies: 7
-- Name: SCHEMA topology; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON SCHEMA topology IS 'PostGIS Topology schema';


--
-- TOC entry 1 (class 3079 OID 12924)
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- TOC entry 4415 (class 0 OID 0)
-- Dependencies: 1
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


--
-- TOC entry 2 (class 3079 OID 103163)
-- Name: postgis; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;


--
-- TOC entry 4416 (class 0 OID 0)
-- Dependencies: 2
-- Name: EXTENSION postgis; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION postgis IS 'PostGIS geometry, geography, and raster spatial types and functions';


--
-- TOC entry 3 (class 3079 OID 104663)
-- Name: postgis_topology; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS postgis_topology WITH SCHEMA topology;


--
-- TOC entry 4417 (class 0 OID 0)
-- Dependencies: 3
-- Name: EXTENSION postgis_topology; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION postgis_topology IS 'PostGIS topology spatial types and functions';


SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 227 (class 1259 OID 160946)
-- Name: booked_legs; Type: TABLE; Schema: public; Owner: rideshare
--

CREATE TABLE public.booked_legs (
    booking bigint NOT NULL,
    leg bigint NOT NULL
);


ALTER TABLE public.booked_legs OWNER TO rideshare;

--
-- TOC entry 228 (class 1259 OID 160949)
-- Name: booking; Type: TABLE; Schema: public; Owner: rideshare
--

CREATE TABLE public.booking (
    id bigint NOT NULL,
    arrival_time timestamp without time zone NOT NULL,
    cancel_reason character varying(256),
    cancelled_by_driver boolean,
    departure_time timestamp without time zone NOT NULL,
    to_label character varying(128),
    to_point public.geometry,
    nr_seats integer,
    from_label character varying(128),
    from_point public.geometry,
    state character varying(3),
    passenger bigint NOT NULL,
    ride bigint NOT NULL,
    CONSTRAINT booking_nr_seats_check CHECK ((nr_seats <= 99))
);


ALTER TABLE public.booking OWNER TO rideshare;

--
-- TOC entry 220 (class 1259 OID 160932)
-- Name: booking_id_seq; Type: SEQUENCE; Schema: public; Owner: rideshare
--

CREATE SEQUENCE public.booking_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.booking_id_seq OWNER TO rideshare;

--
-- TOC entry 229 (class 1259 OID 160958)
-- Name: car; Type: TABLE; Schema: public; Owner: rideshare
--

CREATE TABLE public.car (
    id bigint NOT NULL,
    brand character varying(32) NOT NULL,
    co2_emission integer,
    color character varying(16) NOT NULL,
    color2 character varying(16),
    deleted boolean,
    license_plate character varying(16) NOT NULL,
    model character varying(64) NOT NULL,
    nr_doors integer,
    nr_seats integer,
    registration_country character varying(3) NOT NULL,
    registration_year integer,
    type character varying(3) NOT NULL,
    type_registration_id character varying(40),
    driver bigint NOT NULL,
    CONSTRAINT car_nr_doors_check CHECK (((nr_doors <= 6) AND (nr_doors >= 0))),
    CONSTRAINT car_nr_seats_check CHECK (((nr_seats <= 99) AND (nr_seats >= 0))),
    CONSTRAINT car_registration_year_check CHECK (((registration_year <= 2100) AND (registration_year >= 1900)))
);


ALTER TABLE public.car OWNER TO rideshare;

--
-- TOC entry 221 (class 1259 OID 160934)
-- Name: car_id_seq; Type: SEQUENCE; Schema: public; Owner: rideshare
--

CREATE SEQUENCE public.car_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.car_id_seq OWNER TO rideshare;

--
-- TOC entry 230 (class 1259 OID 160977)
-- Name: leg; Type: TABLE; Schema: public; Owner: rideshare
--

CREATE TABLE public.leg (
    id bigint NOT NULL,
    distance integer,
    duration integer,
    leg_geometry public.geometry,
    leg_ix integer,
    from_stop bigint NOT NULL,
    ride bigint NOT NULL,
    to_stop bigint NOT NULL
);


ALTER TABLE public.leg OWNER TO rideshare;

--
-- TOC entry 222 (class 1259 OID 160936)
-- Name: leg_id_seq; Type: SEQUENCE; Schema: public; Owner: rideshare
--

CREATE SEQUENCE public.leg_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.leg_id_seq OWNER TO rideshare;

--
-- TOC entry 231 (class 1259 OID 160985)
-- Name: ride; Type: TABLE; Schema: public; Owner: rideshare
--

CREATE TABLE public.ride (
    id bigint NOT NULL,
    co2_emission integer,
    arrival_time timestamp without time zone NOT NULL,
    arrival_time_pinned boolean,
    carthesian_bearing integer,
    carthesian_distance integer,
    departure_time timestamp without time zone NOT NULL,
    distance integer,
    from_label character varying(128),
    from_point public.geometry,
    max_detour_meters integer,
    max_detour_seconds integer,
    nr_seats_available integer,
    remarks character varying(256),
    share_eligibility public.geometry,
    to_label character varying(128),
    to_point public.geometry,
    cancel_reason character varying(256),
    deleted boolean,
    car bigint NOT NULL,
    driver bigint NOT NULL,
    ride_template bigint,
    CONSTRAINT ride_nr_seats_available_check CHECK ((nr_seats_available <= 99))
);


ALTER TABLE public.ride OWNER TO rideshare;

--
-- TOC entry 223 (class 1259 OID 160938)
-- Name: ride_id_seq; Type: SEQUENCE; Schema: public; Owner: rideshare
--

CREATE SEQUENCE public.ride_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ride_id_seq OWNER TO rideshare;

--
-- TOC entry 232 (class 1259 OID 160994)
-- Name: ride_template; Type: TABLE; Schema: public; Owner: rideshare
--

CREATE TABLE public.ride_template (
    id bigint NOT NULL,
    co2_emission integer,
    arrival_time timestamp without time zone NOT NULL,
    arrival_time_pinned boolean,
    carthesian_bearing integer,
    carthesian_distance integer,
    departure_time timestamp without time zone NOT NULL,
    distance integer,
    from_label character varying(128),
    from_point public.geometry,
    max_detour_meters integer,
    max_detour_seconds integer,
    nr_seats_available integer,
    remarks character varying(256),
    share_eligibility public.geometry,
    to_label character varying(128),
    to_point public.geometry,
    leg_geometry public.geometry,
    recurrence_dsow smallint,
    recurrence_horizon timestamp without time zone,
    recurrence_interval integer NOT NULL,
    recurrence_time_zone character varying(32),
    recurrence_unit character varying(1) NOT NULL,
    car bigint NOT NULL,
    driver bigint NOT NULL,
    CONSTRAINT ride_template_nr_seats_available_check CHECK ((nr_seats_available <= 99))
);


ALTER TABLE public.ride_template OWNER TO rideshare;

--
-- TOC entry 224 (class 1259 OID 160940)
-- Name: ride_template_id_seq; Type: SEQUENCE; Schema: public; Owner: rideshare
--

CREATE SEQUENCE public.ride_template_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ride_template_id_seq OWNER TO rideshare;

--
-- TOC entry 233 (class 1259 OID 161003)
-- Name: rs_user; Type: TABLE; Schema: public; Owner: rideshare
--

CREATE TABLE public.rs_user (
    id bigint NOT NULL,
    email character varying(64),
    family_name character varying(64),
    gender character varying(1),
    given_name character varying(32),
    managed_identity character varying(36) NOT NULL,
    year_of_birth character varying(4)
);


ALTER TABLE public.rs_user OWNER TO rideshare;

--
-- TOC entry 234 (class 1259 OID 161008)
-- Name: stop; Type: TABLE; Schema: public; Owner: rideshare
--

CREATE TABLE public.stop (
    id bigint NOT NULL,
    arrival_time timestamp without time zone,
    departure_time timestamp without time zone,
    label character varying(128),
    point public.geometry,
    ride bigint NOT NULL,
    stop_ix integer
);


ALTER TABLE public.stop OWNER TO rideshare;

--
-- TOC entry 225 (class 1259 OID 160942)
-- Name: stop_id_seq; Type: SEQUENCE; Schema: public; Owner: rideshare
--

CREATE SEQUENCE public.stop_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.stop_id_seq OWNER TO rideshare;

--
-- TOC entry 226 (class 1259 OID 160944)
-- Name: user_id_seq; Type: SEQUENCE; Schema: public; Owner: rideshare
--

CREATE SEQUENCE public.user_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.user_id_seq OWNER TO rideshare;

--
-- TOC entry 4248 (class 2606 OID 160957)
-- Name: booking booking_pkey; Type: CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.booking
    ADD CONSTRAINT booking_pkey PRIMARY KEY (id);


--
-- TOC entry 4250 (class 2606 OID 160965)
-- Name: car car_pkey; Type: CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.car
    ADD CONSTRAINT car_pkey PRIMARY KEY (id);


--
-- TOC entry 4252 (class 2606 OID 161017)
-- Name: car car_uc; Type: CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.car
    ADD CONSTRAINT car_uc UNIQUE (driver, registration_country, license_plate);


--
-- TOC entry 4260 (class 2606 OID 161019)
-- Name: rs_user cs_managed_identity_unique; Type: CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.rs_user
    ADD CONSTRAINT cs_managed_identity_unique UNIQUE (managed_identity);


--
-- TOC entry 4254 (class 2606 OID 160984)
-- Name: leg leg_pkey; Type: CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.leg
    ADD CONSTRAINT leg_pkey PRIMARY KEY (id);


--
-- TOC entry 4256 (class 2606 OID 160993)
-- Name: ride ride_pkey; Type: CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.ride
    ADD CONSTRAINT ride_pkey PRIMARY KEY (id);


--
-- TOC entry 4258 (class 2606 OID 161002)
-- Name: ride_template ride_template_pkey; Type: CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.ride_template
    ADD CONSTRAINT ride_template_pkey PRIMARY KEY (id);


--
-- TOC entry 4262 (class 2606 OID 161007)
-- Name: rs_user rs_user_pkey; Type: CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.rs_user
    ADD CONSTRAINT rs_user_pkey PRIMARY KEY (id);


--
-- TOC entry 4264 (class 2606 OID 161015)
-- Name: stop stop_pkey; Type: CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.stop
    ADD CONSTRAINT stop_pkey PRIMARY KEY (id);


--
-- TOC entry 4266 (class 2606 OID 161025)
-- Name: booked_legs booked_legs_booking_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.booked_legs
    ADD CONSTRAINT booked_legs_booking_fk FOREIGN KEY (booking) REFERENCES public.leg(id);


--
-- TOC entry 4265 (class 2606 OID 161020)
-- Name: booked_legs booked_legs_leg_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.booked_legs
    ADD CONSTRAINT booked_legs_leg_fk FOREIGN KEY (leg) REFERENCES public.booking(id);


--
-- TOC entry 4267 (class 2606 OID 161030)
-- Name: booking booking_passenger_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.booking
    ADD CONSTRAINT booking_passenger_fk FOREIGN KEY (passenger) REFERENCES public.rs_user(id);


--
-- TOC entry 4268 (class 2606 OID 161035)
-- Name: booking booking_ride_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.booking
    ADD CONSTRAINT booking_ride_fk FOREIGN KEY (ride) REFERENCES public.ride(id);


--
-- TOC entry 4269 (class 2606 OID 161040)
-- Name: car car_driver_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.car
    ADD CONSTRAINT car_driver_fk FOREIGN KEY (driver) REFERENCES public.rs_user(id);


--
-- TOC entry 4272 (class 2606 OID 161045)
-- Name: leg leg_from_stop_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.leg
    ADD CONSTRAINT leg_from_stop_fk FOREIGN KEY (from_stop) REFERENCES public.stop(id);


--
-- TOC entry 4270 (class 2606 OID 161050)
-- Name: leg leg_ride_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.leg
    ADD CONSTRAINT leg_ride_fk FOREIGN KEY (ride) REFERENCES public.ride(id);


--
-- TOC entry 4271 (class 2606 OID 161055)
-- Name: leg leg_to_stop_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.leg
    ADD CONSTRAINT leg_to_stop_fk FOREIGN KEY (to_stop) REFERENCES public.stop(id);


--
-- TOC entry 4273 (class 2606 OID 161060)
-- Name: ride ride_base_car_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.ride
    ADD CONSTRAINT ride_base_car_fk FOREIGN KEY (car) REFERENCES public.car(id);


--
-- TOC entry 4276 (class 2606 OID 161075)
-- Name: ride_template ride_base_car_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.ride_template
    ADD CONSTRAINT ride_base_car_fk FOREIGN KEY (car) REFERENCES public.car(id);


--
-- TOC entry 4274 (class 2606 OID 161065)
-- Name: ride ride_base_driver_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.ride
    ADD CONSTRAINT ride_base_driver_fk FOREIGN KEY (driver) REFERENCES public.rs_user(id);


--
-- TOC entry 4277 (class 2606 OID 161080)
-- Name: ride_template ride_base_driver_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.ride_template
    ADD CONSTRAINT ride_base_driver_fk FOREIGN KEY (driver) REFERENCES public.rs_user(id);


--
-- TOC entry 4275 (class 2606 OID 161070)
-- Name: ride ride_ride_template_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.ride
    ADD CONSTRAINT ride_ride_template_fk FOREIGN KEY (ride_template) REFERENCES public.ride_template(id);


--
-- TOC entry 4278 (class 2606 OID 161085)
-- Name: stop stop_ride_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.stop
    ADD CONSTRAINT stop_ride_fk FOREIGN KEY (ride) REFERENCES public.ride(id);


-- Completed on 2020-06-03 08:37:33

--
-- PostgreSQL database dump complete
--


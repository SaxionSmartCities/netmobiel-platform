--
-- PostgreSQL database dump
--

-- Dumped from database version 10.18
-- Dumped by pg_dump version 10.18

-- Started on 2022-06-23 09:32:02

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
-- TOC entry 7 (class 2615 OID 39077)
-- Name: topology; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA topology;


ALTER SCHEMA topology OWNER TO postgres;

--
-- TOC entry 4423 (class 0 OID 0)
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
-- TOC entry 4424 (class 0 OID 0)
-- Dependencies: 1
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


--
-- TOC entry 2 (class 3079 OID 37578)
-- Name: postgis; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;


--
-- TOC entry 4425 (class 0 OID 0)
-- Dependencies: 2
-- Name: EXTENSION postgis; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION postgis IS 'PostGIS geometry, geography, and raster spatial types and functions';


--
-- TOC entry 3 (class 3079 OID 39078)
-- Name: postgis_topology; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS postgis_topology WITH SCHEMA topology;


--
-- TOC entry 4426 (class 0 OID 0)
-- Dependencies: 3
-- Name: EXTENSION postgis_topology; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION postgis_topology IS 'PostGIS topology spatial types and functions';


SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 236 (class 1259 OID 183805)
-- Name: booked_legs; Type: TABLE; Schema: public; Owner: rideshare
--

CREATE TABLE public.booked_legs (
    leg bigint NOT NULL,
    booking bigint NOT NULL
);


ALTER TABLE public.booked_legs OWNER TO rideshare;

--
-- TOC entry 235 (class 1259 OID 183613)
-- Name: booking; Type: TABLE; Schema: public; Owner: rideshare
--

CREATE TABLE public.booking (
    id bigint NOT NULL,
    arrival_time timestamp without time zone NOT NULL,
    cancel_reason character varying(256),
    cancelled_by_driver boolean,
    departure_time timestamp without time zone NOT NULL,
    to_label character varying(256),
    to_point public.geometry NOT NULL,
    nr_seats integer,
    passenger_trip_ref character varying(32),
    from_label character varying(256),
    from_point public.geometry NOT NULL,
    state character varying(3),
    passenger bigint NOT NULL,
    ride bigint NOT NULL,
    passenger_trip_plan_ref character varying(32),
    confirmed boolean,
    fare_credits integer,
    conf_reason character varying(3),
    payment_state character varying(1),
    payment_id character varying(32),
    confirmed_by_passenger boolean,
    conf_reason_by_passenger character varying(3),
    CONSTRAINT booking_nr_seats_check CHECK ((nr_seats <= 99))
);


ALTER TABLE public.booking OWNER TO rideshare;

--
-- TOC entry 221 (class 1259 OID 62403)
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
-- TOC entry 226 (class 1259 OID 62419)
-- Name: car; Type: TABLE; Schema: public; Owner: rideshare
--

CREATE TABLE public.car (
    id bigint NOT NULL,
    brand character varying(32) NOT NULL,
    co2_emission integer,
    color character varying(16) NOT NULL,
    color2 character varying(16),
    license_plate character varying(16) NOT NULL,
    model character varying(32) NOT NULL,
    nr_doors integer,
    nr_seats integer,
    registration_country character varying(3) NOT NULL,
    registration_year integer,
    type character varying(3) NOT NULL,
    type_registration_id character varying(40),
    driver bigint NOT NULL,
    deleted boolean,
    license_plate_raw character varying(12) NOT NULL,
    CONSTRAINT car_nr_doors_check CHECK (((nr_doors <= 6) AND (nr_doors >= 0))),
    CONSTRAINT car_nr_seats_check CHECK (((nr_seats <= 99) AND (nr_seats >= 0))),
    CONSTRAINT car_registration_year_check CHECK (((registration_year <= 2100) AND (registration_year >= 1900)))
);


ALTER TABLE public.car OWNER TO rideshare;

--
-- TOC entry 222 (class 1259 OID 62405)
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
-- TOC entry 220 (class 1259 OID 49344)
-- Name: hibernate_sequence; Type: SEQUENCE; Schema: public; Owner: rideshare
--

CREATE SEQUENCE public.hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.hibernate_sequence OWNER TO rideshare;

--
-- TOC entry 233 (class 1259 OID 153213)
-- Name: leg; Type: TABLE; Schema: public; Owner: rideshare
--

CREATE TABLE public.leg (
    id bigint NOT NULL,
    distance integer,
    duration integer,
    leg_geometry public.geometry,
    from_stop bigint NOT NULL,
    to_stop bigint NOT NULL,
    ride bigint NOT NULL,
    leg_ix integer
);


ALTER TABLE public.leg OWNER TO rideshare;

--
-- TOC entry 234 (class 1259 OID 153236)
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
-- TOC entry 230 (class 1259 OID 64372)
-- Name: recurrence_id_seq; Type: SEQUENCE; Schema: public; Owner: rideshare
--

CREATE SEQUENCE public.recurrence_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.recurrence_id_seq OWNER TO rideshare;

--
-- TOC entry 227 (class 1259 OID 62459)
-- Name: ride; Type: TABLE; Schema: public; Owner: rideshare
--

CREATE TABLE public.ride (
    id bigint NOT NULL,
    deleted boolean,
    departure_time timestamp without time zone NOT NULL,
    arrival_time timestamp without time zone NOT NULL,
    ride_template bigint,
    carthesian_bearing integer,
    carthesian_distance integer,
    co2_emission integer,
    distance integer,
    max_detour_meters integer NOT NULL,
    max_detour_seconds integer,
    nr_seats_available integer,
    remarks character varying(256),
    share_eligibility public.geometry,
    driver bigint NOT NULL,
    car bigint NOT NULL,
    cancel_reason character varying(256),
    from_label character varying(256),
    from_point public.geometry NOT NULL,
    to_label character varying(256),
    to_point public.geometry NOT NULL,
    arrival_time_pinned boolean DEFAULT false,
    state character varying(3) NOT NULL,
    version integer DEFAULT 0 NOT NULL,
    departure_postal_code character varying(6),
    arrival_postal_code character varying(6),
    able_to_assist boolean DEFAULT true NOT NULL,
    reminder_count integer DEFAULT 0 NOT NULL,
    validation_exp_time timestamp without time zone,
    validation_rem_time timestamp without time zone
);


ALTER TABLE public.ride OWNER TO rideshare;

--
-- TOC entry 223 (class 1259 OID 62407)
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
-- TOC entry 231 (class 1259 OID 64394)
-- Name: ride_template; Type: TABLE; Schema: public; Owner: rideshare
--

CREATE TABLE public.ride_template (
    id bigint NOT NULL,
    carthesian_bearing integer,
    carthesian_distance integer,
    co2_emission integer,
    distance integer,
    max_detour_meters integer NOT NULL,
    max_detour_seconds integer,
    nr_seats_available integer,
    recurrence_dsow smallint,
    recurrence_horizon timestamp without time zone,
    recurrence_interval integer NOT NULL,
    recurrence_unit character varying(1) NOT NULL,
    remarks character varying(256),
    share_eligibility public.geometry,
    car bigint NOT NULL,
    driver bigint NOT NULL,
    departure_time timestamp without time zone NOT NULL,
    arrival_time timestamp without time zone NOT NULL,
    leg_geometry public.geometry,
    from_label character varying(256),
    from_point public.geometry NOT NULL,
    to_label character varying(256),
    to_point public.geometry NOT NULL,
    recurrence_time_zone character varying(32),
    arrival_time_pinned boolean DEFAULT false,
    departure_postal_code character varying(6),
    arrival_postal_code character varying(6),
    able_to_assist boolean DEFAULT true NOT NULL,
    CONSTRAINT ride_template_nr_seats_available_check CHECK ((nr_seats_available <= 99))
);


ALTER TABLE public.ride_template OWNER TO rideshare;

--
-- TOC entry 232 (class 1259 OID 64403)
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
-- TOC entry 228 (class 1259 OID 62468)
-- Name: rs_user; Type: TABLE; Schema: public; Owner: rideshare
--

CREATE TABLE public.rs_user (
    id bigint NOT NULL,
    email character varying(64),
    family_name character varying(64),
    given_name character varying(32),
    managed_identity character varying(36) NOT NULL
);


ALTER TABLE public.rs_user OWNER TO rideshare;

--
-- TOC entry 229 (class 1259 OID 62473)
-- Name: stop; Type: TABLE; Schema: public; Owner: rideshare
--

CREATE TABLE public.stop (
    id bigint NOT NULL,
    label character varying(256),
    point public.geometry NOT NULL,
    ride bigint NOT NULL,
    stop_ix integer,
    departure_time timestamp without time zone,
    arrival_time timestamp without time zone
);


ALTER TABLE public.stop OWNER TO rideshare;

--
-- TOC entry 224 (class 1259 OID 62409)
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
-- TOC entry 225 (class 1259 OID 62411)
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
-- TOC entry 4273 (class 2606 OID 183621)
-- Name: booking booking_pkey; Type: CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.booking
    ADD CONSTRAINT booking_pkey PRIMARY KEY (id);


--
-- TOC entry 4257 (class 2606 OID 62426)
-- Name: car car_pkey; Type: CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.car
    ADD CONSTRAINT car_pkey PRIMARY KEY (id);


--
-- TOC entry 4259 (class 2606 OID 62482)
-- Name: car car_uc; Type: CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.car
    ADD CONSTRAINT car_uc UNIQUE (driver, registration_country, license_plate);


--
-- TOC entry 4263 (class 2606 OID 64431)
-- Name: rs_user cs_managed_identity_unique; Type: CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.rs_user
    ADD CONSTRAINT cs_managed_identity_unique UNIQUE (managed_identity);


--
-- TOC entry 4271 (class 2606 OID 153220)
-- Name: leg leg_pkey; Type: CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.leg
    ADD CONSTRAINT leg_pkey PRIMARY KEY (id);


--
-- TOC entry 4261 (class 2606 OID 62467)
-- Name: ride ride_pkey; Type: CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.ride
    ADD CONSTRAINT ride_pkey PRIMARY KEY (id);


--
-- TOC entry 4269 (class 2606 OID 64402)
-- Name: ride_template ride_template_pkey; Type: CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.ride_template
    ADD CONSTRAINT ride_template_pkey PRIMARY KEY (id);


--
-- TOC entry 4265 (class 2606 OID 62472)
-- Name: rs_user rs_user_pkey; Type: CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.rs_user
    ADD CONSTRAINT rs_user_pkey PRIMARY KEY (id);


--
-- TOC entry 4267 (class 2606 OID 62480)
-- Name: stop stop_pkey; Type: CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.stop
    ADD CONSTRAINT stop_pkey PRIMARY KEY (id);


--
-- TOC entry 4286 (class 2606 OID 183808)
-- Name: booked_legs booked_legs_booking_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.booked_legs
    ADD CONSTRAINT booked_legs_booking_fk FOREIGN KEY (booking) REFERENCES public.booking(id);


--
-- TOC entry 4287 (class 2606 OID 183813)
-- Name: booked_legs booked_legs_leg_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.booked_legs
    ADD CONSTRAINT booked_legs_leg_fk FOREIGN KEY (leg) REFERENCES public.leg(id);


--
-- TOC entry 4284 (class 2606 OID 183622)
-- Name: booking booking_passenger_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.booking
    ADD CONSTRAINT booking_passenger_fk FOREIGN KEY (passenger) REFERENCES public.rs_user(id);


--
-- TOC entry 4285 (class 2606 OID 183627)
-- Name: booking booking_ride_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.booking
    ADD CONSTRAINT booking_ride_fk FOREIGN KEY (ride) REFERENCES public.ride(id);


--
-- TOC entry 4274 (class 2606 OID 62512)
-- Name: car car_driver_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.car
    ADD CONSTRAINT car_driver_fk FOREIGN KEY (driver) REFERENCES public.rs_user(id);


--
-- TOC entry 4281 (class 2606 OID 153221)
-- Name: leg leg_from_stop_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.leg
    ADD CONSTRAINT leg_from_stop_fk FOREIGN KEY (from_stop) REFERENCES public.stop(id);


--
-- TOC entry 4282 (class 2606 OID 153226)
-- Name: leg leg_ride_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.leg
    ADD CONSTRAINT leg_ride_fk FOREIGN KEY (ride) REFERENCES public.ride(id);


--
-- TOC entry 4283 (class 2606 OID 153231)
-- Name: leg leg_to_stop_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.leg
    ADD CONSTRAINT leg_to_stop_fk FOREIGN KEY (to_stop) REFERENCES public.stop(id);


--
-- TOC entry 4276 (class 2606 OID 153196)
-- Name: ride ride_car_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.ride
    ADD CONSTRAINT ride_car_fk FOREIGN KEY (car) REFERENCES public.car(id);


--
-- TOC entry 4277 (class 2606 OID 153201)
-- Name: ride ride_driver_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.ride
    ADD CONSTRAINT ride_driver_fk FOREIGN KEY (driver) REFERENCES public.rs_user(id);


--
-- TOC entry 4275 (class 2606 OID 64405)
-- Name: ride ride_ride_template_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.ride
    ADD CONSTRAINT ride_ride_template_fk FOREIGN KEY (ride_template) REFERENCES public.ride_template(id);


--
-- TOC entry 4279 (class 2606 OID 64410)
-- Name: ride_template ride_template_car_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.ride_template
    ADD CONSTRAINT ride_template_car_fk FOREIGN KEY (car) REFERENCES public.car(id);


--
-- TOC entry 4280 (class 2606 OID 64415)
-- Name: ride_template ride_template_driver_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.ride_template
    ADD CONSTRAINT ride_template_driver_fk FOREIGN KEY (driver) REFERENCES public.rs_user(id);


--
-- TOC entry 4278 (class 2606 OID 62562)
-- Name: stop stop_ride_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.stop
    ADD CONSTRAINT stop_ride_fk FOREIGN KEY (ride) REFERENCES public.ride(id);


-- Completed on 2022-06-23 09:32:03

--
-- PostgreSQL database dump complete
--


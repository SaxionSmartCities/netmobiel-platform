--
-- PostgreSQL database dump
--

-- Dumped from database version 10.10
-- Dumped by pg_dump version 10.10

-- Started on 2019-11-08 09:04:44

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
-- TOC entry 2 (class 3079 OID 37578)
-- Name: postgis; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;


--
-- TOC entry 4390 (class 0 OID 0)
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
-- TOC entry 4391 (class 0 OID 0)
-- Dependencies: 3
-- Name: EXTENSION postgis_topology; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION postgis_topology IS 'PostGIS topology spatial types and functions';


SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 226 (class 1259 OID 62413)
-- Name: booking; Type: TABLE; Schema: public; Owner: rideshare
--

CREATE TABLE public.booking (
    id bigint NOT NULL,
    cancel_reason character varying(256),
    cancelled_by_driver boolean,
    nr_seats integer,
    state character varying(3),
    drop_off bigint NOT NULL,
    passenger bigint NOT NULL,
    pickup bigint NOT NULL,
    ride bigint NOT NULL,
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
-- TOC entry 227 (class 1259 OID 62419)
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
-- TOC entry 228 (class 1259 OID 62459)
-- Name: ride; Type: TABLE; Schema: public; Owner: rideshare
--

CREATE TABLE public.ride (
    id bigint NOT NULL,
    carthesian_bearing integer,
    carthesian_distance integer,
    deleted boolean,
    departure_time timestamp without time zone NOT NULL,
    estimated_co2_emission integer,
    estimated_distance integer,
    estimated_driving_time integer,
    max_detour_meters integer,
    max_detour_seconds integer,
    nr_seats_available integer,
    remarks character varying(256),
    share_eligibility public.geometry,
    car bigint NOT NULL,
    driver bigint NOT NULL,
    from_place bigint NOT NULL,
    to_place bigint NOT NULL,
    estimated_arrival_time timestamp without time zone NOT NULL,
    CONSTRAINT ride_nr_seats_available_check CHECK ((nr_seats_available <= 99))
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
-- TOC entry 229 (class 1259 OID 62468)
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
-- TOC entry 230 (class 1259 OID 62473)
-- Name: stop; Type: TABLE; Schema: public; Owner: rideshare
--

CREATE TABLE public.stop (
    id bigint NOT NULL,
    label character varying(255),
    point public.geometry,
    ride bigint,
    stops_order integer
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
-- TOC entry 4230 (class 2606 OID 62418)
-- Name: booking booking_pkey; Type: CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.booking
    ADD CONSTRAINT booking_pkey PRIMARY KEY (id);


--
-- TOC entry 4232 (class 2606 OID 62426)
-- Name: car car_pkey; Type: CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.car
    ADD CONSTRAINT car_pkey PRIMARY KEY (id);


--
-- TOC entry 4234 (class 2606 OID 62482)
-- Name: car car_uc; Type: CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.car
    ADD CONSTRAINT car_uc UNIQUE (driver, registration_country, license_plate);


--
-- TOC entry 4236 (class 2606 OID 62467)
-- Name: ride ride_pkey; Type: CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.ride
    ADD CONSTRAINT ride_pkey PRIMARY KEY (id);


--
-- TOC entry 4238 (class 2606 OID 62472)
-- Name: rs_user rs_user_pkey; Type: CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.rs_user
    ADD CONSTRAINT rs_user_pkey PRIMARY KEY (id);


--
-- TOC entry 4242 (class 2606 OID 62480)
-- Name: stop stop_pkey; Type: CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.stop
    ADD CONSTRAINT stop_pkey PRIMARY KEY (id);


--
-- TOC entry 4240 (class 2606 OID 62491)
-- Name: rs_user uk_5cok4010603wyyathlyr6yfj7; Type: CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.rs_user
    ADD CONSTRAINT uk_5cok4010603wyyathlyr6yfj7 UNIQUE (managed_identity);


--
-- TOC entry 4243 (class 2606 OID 62492)
-- Name: booking booking_drop_off_stop_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.booking
    ADD CONSTRAINT booking_drop_off_stop_fk FOREIGN KEY (drop_off) REFERENCES public.stop(id);


--
-- TOC entry 4244 (class 2606 OID 62497)
-- Name: booking booking_passenger_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.booking
    ADD CONSTRAINT booking_passenger_fk FOREIGN KEY (passenger) REFERENCES public.rs_user(id);


--
-- TOC entry 4245 (class 2606 OID 62502)
-- Name: booking booking_pickup_stop_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.booking
    ADD CONSTRAINT booking_pickup_stop_fk FOREIGN KEY (pickup) REFERENCES public.stop(id);


--
-- TOC entry 4246 (class 2606 OID 62507)
-- Name: booking booking_ride_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.booking
    ADD CONSTRAINT booking_ride_fk FOREIGN KEY (ride) REFERENCES public.ride(id);


--
-- TOC entry 4247 (class 2606 OID 62512)
-- Name: car car_driver_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.car
    ADD CONSTRAINT car_driver_fk FOREIGN KEY (driver) REFERENCES public.rs_user(id);


--
-- TOC entry 4248 (class 2606 OID 62542)
-- Name: ride ride_car_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.ride
    ADD CONSTRAINT ride_car_fk FOREIGN KEY (car) REFERENCES public.car(id);


--
-- TOC entry 4249 (class 2606 OID 62547)
-- Name: ride ride_driver_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.ride
    ADD CONSTRAINT ride_driver_fk FOREIGN KEY (driver) REFERENCES public.rs_user(id);


--
-- TOC entry 4250 (class 2606 OID 62552)
-- Name: ride ride_from_stop_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.ride
    ADD CONSTRAINT ride_from_stop_fk FOREIGN KEY (from_place) REFERENCES public.stop(id);


--
-- TOC entry 4251 (class 2606 OID 62557)
-- Name: ride ride_to_stop_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.ride
    ADD CONSTRAINT ride_to_stop_fk FOREIGN KEY (to_place) REFERENCES public.stop(id);


--
-- TOC entry 4252 (class 2606 OID 62562)
-- Name: stop stop_ride_fk; Type: FK CONSTRAINT; Schema: public; Owner: rideshare
--

ALTER TABLE ONLY public.stop
    ADD CONSTRAINT stop_ride_fk FOREIGN KEY (ride) REFERENCES public.ride(id);


-- Completed on 2019-11-08 09:04:45

--
-- PostgreSQL database dump complete
--


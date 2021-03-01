--
-- PostgreSQL database dump
--

-- Dumped from database version 10.14
-- Dumped by pg_dump version 10.14

-- Started on 2021-02-26 14:44:07

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
-- TOC entry 224 (class 1259 OID 308399)
-- Name: compliment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.compliment (
    id bigint NOT NULL,
    compliment character varying(2) NOT NULL,
    published timestamp without time zone NOT NULL,
    receiver bigint NOT NULL,
    sender bigint NOT NULL
);


--
-- TOC entry 220 (class 1259 OID 308391)
-- Name: compliment_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.compliment_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 225 (class 1259 OID 308404)
-- Name: passenger_luggage; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.passenger_luggage (
    profile bigint NOT NULL,
    luggage character varying(2)
);


--
-- TOC entry 226 (class 1259 OID 308407)
-- Name: place; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.place (
    id bigint NOT NULL,
    country_code character varying(3),
    house_nr character varying(8),
    locality character varying(64),
    label character varying(256),
    point public.geometry,
    postal_code character varying(8),
    street character varying(64),
    profile bigint
);


--
-- TOC entry 221 (class 1259 OID 308393)
-- Name: place_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.place_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 227 (class 1259 OID 308415)
-- Name: preferred_traverse_mode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.preferred_traverse_mode (
    profile bigint NOT NULL,
    traverse_mode character varying(2)
);


--
-- TOC entry 228 (class 1259 OID 308418)
-- Name: profile; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.profile (
    id bigint NOT NULL,
    email character varying(64) NOT NULL,
    family_name character varying(64),
    given_name character varying(32),
    managed_identity character varying(36) NOT NULL,
    consent_accepted_terms boolean,
    consent_older_than_sixteen boolean,
    date_of_birth date,
    fcm_token character varying(512),
    home_country_code character varying(3),
    home_house_nr character varying(8),
    home_locality character varying(64),
    home_label character varying(256),
    home_point public.geometry,
    home_postal_code character varying(8),
    home_street character varying(64),
    image_path character varying(128),
    opt_messages boolean NOT NULL,
    opt_shoutouts boolean NOT NULL,
    opt_trip_confirmations boolean NOT NULL,
    opt_trip_reminders boolean NOT NULL,
    opt_trip_updates boolean NOT NULL,
    phone_number character varying(16),
    user_role character varying(2) NOT NULL
);


--
-- TOC entry 222 (class 1259 OID 308395)
-- Name: profile_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.profile_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 229 (class 1259 OID 308426)
-- Name: review; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.review (
    id bigint NOT NULL,
    published timestamp without time zone NOT NULL,
    review character varying(256) NOT NULL,
    receiver bigint NOT NULL,
    sender bigint NOT NULL
);


--
-- TOC entry 223 (class 1259 OID 308397)
-- Name: review_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.review_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 230 (class 1259 OID 308431)
-- Name: ridehare_luggage; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ridehare_luggage (
    profile bigint NOT NULL,
    luggage character varying(2)
);


--
-- TOC entry 231 (class 1259 OID 308434)
-- Name: rideshare_preferences; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rideshare_preferences (
    id bigint NOT NULL,
    defaultcarref character varying(32),
    max_minutes_detour integer NOT NULL,
    max_passengers integer NOT NULL,
    CONSTRAINT rideshare_preferences_max_passengers_check CHECK (((max_passengers <= 8) AND (max_passengers >= 1)))
);


--
-- TOC entry 232 (class 1259 OID 308440)
-- Name: search_preferences; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.search_preferences (
    id bigint NOT NULL,
    allow_first_leg_rideshare boolean NOT NULL,
    allow_last_leg_rideshare boolean NOT NULL,
    allow_transfers boolean NOT NULL,
    max_transfer_time integer NOT NULL,
    number_of_passengers integer NOT NULL,
    CONSTRAINT search_preferences_number_of_passengers_check CHECK (((number_of_passengers <= 8) AND (number_of_passengers >= 1)))
);


--
-- TOC entry 4228 (class 2606 OID 308403)
-- Name: compliment compliment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compliment
    ADD CONSTRAINT compliment_pkey PRIMARY KEY (id);


--
-- TOC entry 4232 (class 2606 OID 308447)
-- Name: profile cs_managed_identity_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.profile
    ADD CONSTRAINT cs_managed_identity_unique UNIQUE (managed_identity);

ALTER TABLE ONLY public.profile
    ADD CONSTRAINT cs_email_unique UNIQUE (email);

--
-- TOC entry 4230 (class 2606 OID 308414)
-- Name: place place_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.place
    ADD CONSTRAINT place_pkey PRIMARY KEY (id);


--
-- TOC entry 4234 (class 2606 OID 308425)
-- Name: profile profile_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.profile
    ADD CONSTRAINT profile_pkey PRIMARY KEY (id);


--
-- TOC entry 4236 (class 2606 OID 308430)
-- Name: review review_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.review
    ADD CONSTRAINT review_pkey PRIMARY KEY (id);


--
-- TOC entry 4238 (class 2606 OID 308439)
-- Name: rideshare_preferences rideshare_preferences_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rideshare_preferences
    ADD CONSTRAINT rideshare_preferences_pkey PRIMARY KEY (id);


--
-- TOC entry 4240 (class 2606 OID 308445)
-- Name: search_preferences search_preferences_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.search_preferences
    ADD CONSTRAINT search_preferences_pkey PRIMARY KEY (id);


--
-- TOC entry 4241 (class 2606 OID 308448)
-- Name: compliment compliment_receiver_profile_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compliment
    ADD CONSTRAINT compliment_receiver_profile_fk FOREIGN KEY (receiver) REFERENCES public.profile(id);


--
-- TOC entry 4242 (class 2606 OID 308453)
-- Name: compliment compliment_sender_profile_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compliment
    ADD CONSTRAINT compliment_sender_profile_fk FOREIGN KEY (sender) REFERENCES public.profile(id);


--
-- TOC entry 4247 (class 2606 OID 308478)
-- Name: search_preferences fkn3simblon7opqbkk89mabkaqj; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.search_preferences
    ADD CONSTRAINT fkn3simblon7opqbkk89mabkaqj FOREIGN KEY (id) REFERENCES public.profile(id);


--
-- TOC entry 4246 (class 2606 OID 308473)
-- Name: rideshare_preferences fkrlbivlpiqbvs6qtvf4n1jsf4x; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rideshare_preferences
    ADD CONSTRAINT fkrlbivlpiqbvs6qtvf4n1jsf4x FOREIGN KEY (id) REFERENCES public.profile(id);


--
-- TOC entry 4243 (class 2606 OID 308458)
-- Name: place place_profile_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.place
    ADD CONSTRAINT place_profile_fk FOREIGN KEY (profile) REFERENCES public.profile(id);


--
-- TOC entry 4244 (class 2606 OID 308463)
-- Name: review review_receiver_profile_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.review
    ADD CONSTRAINT review_receiver_profile_fk FOREIGN KEY (receiver) REFERENCES public.profile(id);


--
-- TOC entry 4245 (class 2606 OID 308468)
-- Name: review review_sender_profile_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.review
    ADD CONSTRAINT review_sender_profile_fk FOREIGN KEY (sender) REFERENCES public.profile(id);


-- Completed on 2021-02-26 14:44:08

--
-- PostgreSQL database dump complete
--


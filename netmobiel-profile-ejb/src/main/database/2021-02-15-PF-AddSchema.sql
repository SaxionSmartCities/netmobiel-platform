--
-- PostgreSQL database dump
--

-- Dumped from database version 10.14
-- Dumped by pg_dump version 10.14

-- Started on 2021-02-15 15:37:16

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

SET default_with_oids = false;

--
-- TOC entry 224 (class 1259 OID 306796)
-- Name: address; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.address (
    id bigint NOT NULL,
    country_code character varying(2),
    house_number character varying(8),
    locality character varying(64),
    label character varying(256),
    point public.geometry,
    postal_code character varying(8),
    street character varying(64),
    profile bigint
);


--
-- TOC entry 220 (class 1259 OID 306788)
-- Name: address_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.address_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 225 (class 1259 OID 306804)
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
-- TOC entry 221 (class 1259 OID 306790)
-- Name: compliment_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.compliment_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 226 (class 1259 OID 306809)
-- Name: passenger_luggage; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.passenger_luggage (
    profile bigint NOT NULL,
    luggage character varying(2)
);


--
-- TOC entry 227 (class 1259 OID 306812)
-- Name: preferred_traverse_mode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.preferred_traverse_mode (
    profile bigint NOT NULL,
    traverse_mode character varying(2)
);


--
-- TOC entry 228 (class 1259 OID 306815)
-- Name: profile; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.profile (
    id bigint NOT NULL,
    email character varying(64),
    family_name character varying(64),
    given_name character varying(32),
    managed_identity character varying(36) NOT NULL,
    accepted_terms boolean,
    older_than_sixteen boolean,
    date_of_birth date,
    fcm_token character varying(512),
    image_path character varying(128),
    messages boolean NOT NULL,
    shoutouts boolean NOT NULL,
    trip_confirmations boolean NOT NULL,
    trip_reminders boolean NOT NULL,
    trip_updates boolean NOT NULL,
    phone_number character varying(16),
    user_role character varying(2) NOT NULL,
    home_address bigint
);


--
-- TOC entry 222 (class 1259 OID 306792)
-- Name: profile_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.profile_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 229 (class 1259 OID 306823)
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
-- TOC entry 223 (class 1259 OID 306794)
-- Name: review_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.review_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 230 (class 1259 OID 306828)
-- Name: ridehare_luggage; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ridehare_luggage (
    profile bigint NOT NULL,
    luggage character varying(2)
);


--
-- TOC entry 231 (class 1259 OID 306831)
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
-- TOC entry 232 (class 1259 OID 306837)
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
-- TOC entry 4228 (class 2606 OID 306803)
-- Name: address address_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.address
    ADD CONSTRAINT address_pkey PRIMARY KEY (id);


--
-- TOC entry 4230 (class 2606 OID 306808)
-- Name: compliment compliment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compliment
    ADD CONSTRAINT compliment_pkey PRIMARY KEY (id);


--
-- TOC entry 4232 (class 2606 OID 306844)
-- Name: profile cs_managed_identity_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.profile
    ADD CONSTRAINT cs_managed_identity_unique UNIQUE (managed_identity);


--
-- TOC entry 4234 (class 2606 OID 306822)
-- Name: profile profile_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.profile
    ADD CONSTRAINT profile_pkey PRIMARY KEY (id);


--
-- TOC entry 4236 (class 2606 OID 306827)
-- Name: review review_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.review
    ADD CONSTRAINT review_pkey PRIMARY KEY (id);


--
-- TOC entry 4238 (class 2606 OID 306836)
-- Name: rideshare_preferences rideshare_preferences_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rideshare_preferences
    ADD CONSTRAINT rideshare_preferences_pkey PRIMARY KEY (id);


--
-- TOC entry 4240 (class 2606 OID 306842)
-- Name: search_preferences search_preferences_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.search_preferences
    ADD CONSTRAINT search_preferences_pkey PRIMARY KEY (id);


--
-- TOC entry 4241 (class 2606 OID 306845)
-- Name: address address_profile_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.address
    ADD CONSTRAINT address_profile_fk FOREIGN KEY (profile) REFERENCES public.profile(id);


--
-- TOC entry 4242 (class 2606 OID 306850)
-- Name: compliment compliment_receiver_profile_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compliment
    ADD CONSTRAINT compliment_receiver_profile_fk FOREIGN KEY (receiver) REFERENCES public.profile(id);


--
-- TOC entry 4243 (class 2606 OID 306855)
-- Name: compliment compliment_sender_profile_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compliment
    ADD CONSTRAINT compliment_sender_profile_fk FOREIGN KEY (sender) REFERENCES public.profile(id);


--
-- TOC entry 4248 (class 2606 OID 306880)
-- Name: search_preferences fkn3simblon7opqbkk89mabkaqj; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.search_preferences
    ADD CONSTRAINT fkn3simblon7opqbkk89mabkaqj FOREIGN KEY (id) REFERENCES public.profile(id);


--
-- TOC entry 4247 (class 2606 OID 306875)
-- Name: rideshare_preferences fkrlbivlpiqbvs6qtvf4n1jsf4x; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rideshare_preferences
    ADD CONSTRAINT fkrlbivlpiqbvs6qtvf4n1jsf4x FOREIGN KEY (id) REFERENCES public.profile(id);


--
-- TOC entry 4244 (class 2606 OID 306860)
-- Name: profile profile_home_address_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.profile
    ADD CONSTRAINT profile_home_address_fk FOREIGN KEY (home_address) REFERENCES public.address(id) ON DELETE SET NULL;


--
-- TOC entry 4245 (class 2606 OID 306865)
-- Name: review review_receiver_profile_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.review
    ADD CONSTRAINT review_receiver_profile_fk FOREIGN KEY (receiver) REFERENCES public.profile(id);


--
-- TOC entry 4246 (class 2606 OID 306870)
-- Name: review review_sender_profile_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.review
    ADD CONSTRAINT review_sender_profile_fk FOREIGN KEY (sender) REFERENCES public.profile(id);


-- Completed on 2021-02-15 15:37:16

--
-- PostgreSQL database dump complete
--


--
-- PostgreSQL database dump
--

-- Dumped from database version 10.18
-- Dumped by pg_dump version 10.18

-- Started on 2022-06-23 09:31:31

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
-- TOC entry 10 (class 2615 OID 295649)
-- Name: topology; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA topology;


ALTER SCHEMA topology OWNER TO postgres;

--
-- TOC entry 4472 (class 0 OID 0)
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
-- TOC entry 4473 (class 0 OID 0)
-- Dependencies: 1
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


--
-- TOC entry 2 (class 3079 OID 294150)
-- Name: postgis; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;


--
-- TOC entry 4474 (class 0 OID 0)
-- Dependencies: 2
-- Name: EXTENSION postgis; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION postgis IS 'PostGIS geometry, geography, and raster spatial types and functions';


--
-- TOC entry 3 (class 3079 OID 295650)
-- Name: postgis_topology; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS postgis_topology WITH SCHEMA topology;


--
-- TOC entry 4475 (class 0 OID 0)
-- Dependencies: 3
-- Name: EXTENSION postgis_topology; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION postgis_topology IS 'PostGIS topology spatial types and functions';


SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 238 (class 1259 OID 498665)
-- Name: compliment; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.compliment (
    compliment_set bigint NOT NULL,
    compliment character varying(2) NOT NULL
);


ALTER TABLE public.compliment OWNER TO postgres;

--
-- TOC entry 220 (class 1259 OID 306894)
-- Name: compliment_set; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.compliment_set (
    id bigint NOT NULL,
    published timestamp without time zone NOT NULL,
    receiver bigint NOT NULL,
    sender bigint NOT NULL,
    context character varying(32) NOT NULL
);


ALTER TABLE public.compliment_set OWNER TO postgres;

--
-- TOC entry 221 (class 1259 OID 306897)
-- Name: compliment_set_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.compliment_set_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.compliment_set_id_seq OWNER TO postgres;

--
-- TOC entry 233 (class 1259 OID 310110)
-- Name: delegation; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.delegation (
    id bigint NOT NULL,
    activation_time timestamp without time zone,
    revocation_time timestamp without time zone,
    submission_time timestamp without time zone NOT NULL,
    activation_code character varying(32),
    delegate bigint NOT NULL,
    delegator bigint NOT NULL,
    activation_code_sent_time timestamp without time zone,
    transfer_from bigint,
    sms_id character varying(32),
    activation_code_ttl integer NOT NULL
);


ALTER TABLE public.delegation OWNER TO postgres;

--
-- TOC entry 234 (class 1259 OID 310125)
-- Name: delegation_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.delegation_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.delegation_id_seq OWNER TO postgres;

--
-- TOC entry 242 (class 1259 OID 647696)
-- Name: page_visit; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.page_visit (
    id bigint NOT NULL,
    user_session bigint NOT NULL,
    path character varying(128) NOT NULL,
    visit_time timestamp without time zone NOT NULL,
    on_behalf_of integer
);


ALTER TABLE public.page_visit OWNER TO postgres;

--
-- TOC entry 243 (class 1259 OID 647706)
-- Name: page_visit_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.page_visit_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.page_visit_id_seq OWNER TO postgres;

--
-- TOC entry 222 (class 1259 OID 306899)
-- Name: passenger_luggage; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.passenger_luggage (
    prefs bigint NOT NULL,
    luggage character varying(2)
);


ALTER TABLE public.passenger_luggage OWNER TO postgres;

--
-- TOC entry 230 (class 1259 OID 308484)
-- Name: place; Type: TABLE; Schema: public; Owner: postgres
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
    profile bigint,
    reference character varying(256),
    state_code character varying(3),
    category character varying(32),
    name character varying(64)
);


ALTER TABLE public.place OWNER TO postgres;

--
-- TOC entry 231 (class 1259 OID 308490)
-- Name: place_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.place_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.place_id_seq OWNER TO postgres;

--
-- TOC entry 223 (class 1259 OID 306902)
-- Name: preferred_traverse_mode; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.preferred_traverse_mode (
    prefs bigint NOT NULL,
    traverse_mode character varying(2)
);


ALTER TABLE public.preferred_traverse_mode OWNER TO postgres;

--
-- TOC entry 232 (class 1259 OID 308492)
-- Name: profile; Type: TABLE; Schema: public; Owner: postgres
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
    user_role character varying(2) NOT NULL,
    home_state_code character varying(3),
    created_by bigint,
    creation_time timestamp without time zone NOT NULL,
    consent_safety_guidelines boolean NOT NULL,
    acting_role character varying(2) NOT NULL
);


ALTER TABLE public.profile OWNER TO postgres;

--
-- TOC entry 224 (class 1259 OID 306911)
-- Name: profile_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.profile_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.profile_id_seq OWNER TO postgres;

--
-- TOC entry 225 (class 1259 OID 306913)
-- Name: review; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.review (
    id bigint NOT NULL,
    published timestamp without time zone NOT NULL,
    review character varying(256) NOT NULL,
    receiver bigint NOT NULL,
    sender bigint NOT NULL,
    context character varying(32) NOT NULL
);


ALTER TABLE public.review OWNER TO postgres;

--
-- TOC entry 226 (class 1259 OID 306916)
-- Name: review_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.review_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.review_id_seq OWNER TO postgres;

--
-- TOC entry 227 (class 1259 OID 306918)
-- Name: rideshare_luggage; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.rideshare_luggage (
    prefs bigint NOT NULL,
    luggage character varying(2)
);


ALTER TABLE public.rideshare_luggage OWNER TO postgres;

--
-- TOC entry 228 (class 1259 OID 306921)
-- Name: rideshare_preferences; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.rideshare_preferences (
    id bigint NOT NULL,
    default_car_ref character varying(32),
    max_distance_detour integer NOT NULL,
    max_passengers integer NOT NULL,
    max_time_detour integer,
    able_to_assist boolean DEFAULT true NOT NULL,
    CONSTRAINT rideshare_preferences_max_passengers_check CHECK (((max_passengers <= 8) AND (max_passengers >= 1)))
);


ALTER TABLE public.rideshare_preferences OWNER TO postgres;

--
-- TOC entry 229 (class 1259 OID 306925)
-- Name: search_preferences; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.search_preferences (
    id bigint NOT NULL,
    allow_first_leg_rideshare boolean NOT NULL,
    allow_last_leg_rideshare boolean NOT NULL,
    max_walk_distance integer NOT NULL,
    number_of_passengers integer NOT NULL,
    max_transfers integer,
    needs_assistance boolean DEFAULT false NOT NULL,
    CONSTRAINT search_preferences_number_of_passengers_check CHECK (((number_of_passengers <= 8) AND (number_of_passengers >= 1)))
);


ALTER TABLE public.search_preferences OWNER TO postgres;

--
-- TOC entry 235 (class 1259 OID 371242)
-- Name: survey; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.survey (
    id bigint NOT NULL,
    survey_id character varying(32) NOT NULL,
    display_name character varying(64) NOT NULL,
    remarks character varying(256),
    take_delay_hours integer DEFAULT 0 NOT NULL,
    take_interval_hours integer,
    start_time timestamp without time zone,
    end_time timestamp without time zone,
    incentive_code character varying(16)
);


ALTER TABLE public.survey OWNER TO postgres;

--
-- TOC entry 237 (class 1259 OID 371267)
-- Name: survey_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.survey_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.survey_id_seq OWNER TO postgres;

--
-- TOC entry 236 (class 1259 OID 371250)
-- Name: survey_interaction; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.survey_interaction (
    survey bigint NOT NULL,
    profile bigint NOT NULL,
    trigger_time timestamp without time zone NOT NULL,
    invitation_time timestamp without time zone NOT NULL,
    invitation_count integer DEFAULT 0 NOT NULL,
    redirect_time timestamp without time zone,
    redirect_count integer DEFAULT 0 NOT NULL,
    submit_time timestamp without time zone,
    id bigint NOT NULL
);


ALTER TABLE public.survey_interaction OWNER TO postgres;

--
-- TOC entry 239 (class 1259 OID 573531)
-- Name: survey_interaction_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.survey_interaction_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.survey_interaction_seq OWNER TO postgres;

--
-- TOC entry 240 (class 1259 OID 647674)
-- Name: user_session; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_session (
    id bigint NOT NULL,
    real_user bigint NOT NULL,
    session_id character varying(48) NOT NULL,
    ip_address character varying(32) NOT NULL,
    user_agent character varying(512) NOT NULL,
    session_start timestamp without time zone NOT NULL,
    session_end timestamp without time zone
);


ALTER TABLE public.user_session OWNER TO postgres;

--
-- TOC entry 241 (class 1259 OID 647689)
-- Name: user_session_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.user_session_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.user_session_id_seq OWNER TO postgres;

--
-- TOC entry 4281 (class 2606 OID 306932)
-- Name: compliment_set compliment_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.compliment_set
    ADD CONSTRAINT compliment_pkey PRIMARY KEY (id);


--
-- TOC entry 4283 (class 2606 OID 498664)
-- Name: compliment_set cs_compliment_set_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.compliment_set
    ADD CONSTRAINT cs_compliment_set_unique UNIQUE (receiver, context);


--
-- TOC entry 4311 (class 2606 OID 498669)
-- Name: compliment cs_compliment_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.compliment
    ADD CONSTRAINT cs_compliment_unique UNIQUE (compliment_set, compliment);


--
-- TOC entry 4295 (class 2606 OID 308583)
-- Name: profile cs_email_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.profile
    ADD CONSTRAINT cs_email_unique UNIQUE (email);


--
-- TOC entry 4297 (class 2606 OID 308499)
-- Name: profile cs_managed_identity_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.profile
    ADD CONSTRAINT cs_managed_identity_unique UNIQUE (managed_identity);


--
-- TOC entry 4285 (class 2606 OID 498662)
-- Name: review cs_review_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.review
    ADD CONSTRAINT cs_review_unique UNIQUE (receiver, context);


--
-- TOC entry 4307 (class 2606 OID 573536)
-- Name: survey_interaction cs_survey_interaction_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.survey_interaction
    ADD CONSTRAINT cs_survey_interaction_unique UNIQUE (profile, survey);


--
-- TOC entry 4301 (class 2606 OID 310114)
-- Name: delegation delegation_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delegation
    ADD CONSTRAINT delegation_pkey PRIMARY KEY (id);


--
-- TOC entry 4317 (class 2606 OID 647700)
-- Name: page_visit page_visit_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.page_visit
    ADD CONSTRAINT page_visit_pkey PRIMARY KEY (id);


--
-- TOC entry 4293 (class 2606 OID 308501)
-- Name: place place_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.place
    ADD CONSTRAINT place_pkey PRIMARY KEY (id);


--
-- TOC entry 4299 (class 2606 OID 308503)
-- Name: profile profile_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.profile
    ADD CONSTRAINT profile_pkey PRIMARY KEY (id);


--
-- TOC entry 4287 (class 2606 OID 306938)
-- Name: review review_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.review
    ADD CONSTRAINT review_pkey PRIMARY KEY (id);


--
-- TOC entry 4289 (class 2606 OID 306940)
-- Name: rideshare_preferences rideshare_preferences_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rideshare_preferences
    ADD CONSTRAINT rideshare_preferences_pkey PRIMARY KEY (id);


--
-- TOC entry 4291 (class 2606 OID 306942)
-- Name: search_preferences search_preferences_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.search_preferences
    ADD CONSTRAINT search_preferences_pkey PRIMARY KEY (id);


--
-- TOC entry 4309 (class 2606 OID 573534)
-- Name: survey_interaction survey_interaction_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.survey_interaction
    ADD CONSTRAINT survey_interaction_pkey PRIMARY KEY (id);


--
-- TOC entry 4303 (class 2606 OID 371247)
-- Name: survey survey_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.survey
    ADD CONSTRAINT survey_pkey PRIMARY KEY (id);


--
-- TOC entry 4313 (class 2606 OID 647683)
-- Name: user_session uc_session_id; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_session
    ADD CONSTRAINT uc_session_id UNIQUE (session_id);


--
-- TOC entry 4305 (class 2606 OID 371249)
-- Name: survey uc_survey_id; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.survey
    ADD CONSTRAINT uc_survey_id UNIQUE (survey_id);


--
-- TOC entry 4315 (class 2606 OID 647681)
-- Name: user_session user_session_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_session
    ADD CONSTRAINT user_session_pkey PRIMARY KEY (id);


--
-- TOC entry 4333 (class 2606 OID 498670)
-- Name: compliment compliment_compliment_set_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.compliment
    ADD CONSTRAINT compliment_compliment_set_fk FOREIGN KEY (compliment_set) REFERENCES public.compliment_set(id);


--
-- TOC entry 4318 (class 2606 OID 308540)
-- Name: compliment_set compliment_receiver_profile_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.compliment_set
    ADD CONSTRAINT compliment_receiver_profile_fk FOREIGN KEY (receiver) REFERENCES public.profile(id);


--
-- TOC entry 4319 (class 2606 OID 308545)
-- Name: compliment_set compliment_sender_profile_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.compliment_set
    ADD CONSTRAINT compliment_sender_profile_fk FOREIGN KEY (sender) REFERENCES public.profile(id);


--
-- TOC entry 4329 (class 2606 OID 310115)
-- Name: delegation delegation_delegate_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delegation
    ADD CONSTRAINT delegation_delegate_fk FOREIGN KEY (delegate) REFERENCES public.profile(id);


--
-- TOC entry 4330 (class 2606 OID 310120)
-- Name: delegation delegation_delegator_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delegation
    ADD CONSTRAINT delegation_delegator_fk FOREIGN KEY (delegator) REFERENCES public.profile(id);


--
-- TOC entry 4336 (class 2606 OID 647766)
-- Name: page_visit page_visit_on_behalf_of_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.page_visit
    ADD CONSTRAINT page_visit_on_behalf_of_fk FOREIGN KEY (on_behalf_of) REFERENCES public.profile(id) ON DELETE CASCADE;


--
-- TOC entry 4335 (class 2606 OID 647701)
-- Name: page_visit page_visit_user_session_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.page_visit
    ADD CONSTRAINT page_visit_user_session_fk FOREIGN KEY (user_session) REFERENCES public.user_session(id) ON DELETE CASCADE;


--
-- TOC entry 4320 (class 2606 OID 319811)
-- Name: passenger_luggage passenger_luggage_prefs_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.passenger_luggage
    ADD CONSTRAINT passenger_luggage_prefs_fk FOREIGN KEY (prefs) REFERENCES public.search_preferences(id);


--
-- TOC entry 4327 (class 2606 OID 308560)
-- Name: place place_profile_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.place
    ADD CONSTRAINT place_profile_fk FOREIGN KEY (profile) REFERENCES public.profile(id);


--
-- TOC entry 4321 (class 2606 OID 319826)
-- Name: preferred_traverse_mode preferred_traverse_mode_prefs_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.preferred_traverse_mode
    ADD CONSTRAINT preferred_traverse_mode_prefs_fk FOREIGN KEY (prefs) REFERENCES public.search_preferences(id);


--
-- TOC entry 4328 (class 2606 OID 325574)
-- Name: profile profile_created_by_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.profile
    ADD CONSTRAINT profile_created_by_fk FOREIGN KEY (created_by) REFERENCES public.profile(id);


--
-- TOC entry 4322 (class 2606 OID 308565)
-- Name: review review_receiver_profile_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.review
    ADD CONSTRAINT review_receiver_profile_fk FOREIGN KEY (receiver) REFERENCES public.profile(id);


--
-- TOC entry 4323 (class 2606 OID 308570)
-- Name: review review_sender_profile_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.review
    ADD CONSTRAINT review_sender_profile_fk FOREIGN KEY (sender) REFERENCES public.profile(id);


--
-- TOC entry 4324 (class 2606 OID 319821)
-- Name: rideshare_luggage rideshare_luggage_prefs_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rideshare_luggage
    ADD CONSTRAINT rideshare_luggage_prefs_fk FOREIGN KEY (prefs) REFERENCES public.rideshare_preferences(id);


--
-- TOC entry 4325 (class 2606 OID 321228)
-- Name: rideshare_preferences rideshare_prefs_profile_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rideshare_preferences
    ADD CONSTRAINT rideshare_prefs_profile_fk FOREIGN KEY (id) REFERENCES public.profile(id);


--
-- TOC entry 4326 (class 2606 OID 321233)
-- Name: search_preferences search_prefs_profile_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.search_preferences
    ADD CONSTRAINT search_prefs_profile_fk FOREIGN KEY (id) REFERENCES public.profile(id);


--
-- TOC entry 4331 (class 2606 OID 371257)
-- Name: survey_interaction survey_interaction_profile_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.survey_interaction
    ADD CONSTRAINT survey_interaction_profile_fk FOREIGN KEY (profile) REFERENCES public.profile(id);


--
-- TOC entry 4332 (class 2606 OID 371262)
-- Name: survey_interaction survey_interaction_survey_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.survey_interaction
    ADD CONSTRAINT survey_interaction_survey_fk FOREIGN KEY (survey) REFERENCES public.survey(id);


--
-- TOC entry 4334 (class 2606 OID 647771)
-- Name: user_session user_session_real_user_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_session
    ADD CONSTRAINT user_session_real_user_fk FOREIGN KEY (real_user) REFERENCES public.profile(id) ON DELETE CASCADE;


--
-- TOC entry 4476 (class 0 OID 0)
-- Dependencies: 238
-- Name: TABLE compliment; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.compliment TO profilesvc;


--
-- TOC entry 4477 (class 0 OID 0)
-- Dependencies: 220
-- Name: TABLE compliment_set; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.compliment_set TO profilesvc;


--
-- TOC entry 4478 (class 0 OID 0)
-- Dependencies: 221
-- Name: SEQUENCE compliment_set_id_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.compliment_set_id_seq TO profilesvc;


--
-- TOC entry 4479 (class 0 OID 0)
-- Dependencies: 233
-- Name: TABLE delegation; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.delegation TO profilesvc;


--
-- TOC entry 4480 (class 0 OID 0)
-- Dependencies: 234
-- Name: SEQUENCE delegation_id_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.delegation_id_seq TO profilesvc;


--
-- TOC entry 4481 (class 0 OID 0)
-- Dependencies: 202
-- Name: TABLE geography_columns; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.geography_columns TO profilesvc;


--
-- TOC entry 4482 (class 0 OID 0)
-- Dependencies: 203
-- Name: TABLE geometry_columns; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.geometry_columns TO profilesvc;


--
-- TOC entry 4483 (class 0 OID 0)
-- Dependencies: 242
-- Name: TABLE page_visit; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.page_visit TO profilesvc;


--
-- TOC entry 4484 (class 0 OID 0)
-- Dependencies: 243
-- Name: SEQUENCE page_visit_id_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.page_visit_id_seq TO profilesvc;


--
-- TOC entry 4485 (class 0 OID 0)
-- Dependencies: 222
-- Name: TABLE passenger_luggage; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.passenger_luggage TO profilesvc;


--
-- TOC entry 4486 (class 0 OID 0)
-- Dependencies: 230
-- Name: TABLE place; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.place TO profilesvc;


--
-- TOC entry 4487 (class 0 OID 0)
-- Dependencies: 231
-- Name: SEQUENCE place_id_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.place_id_seq TO profilesvc;


--
-- TOC entry 4488 (class 0 OID 0)
-- Dependencies: 223
-- Name: TABLE preferred_traverse_mode; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.preferred_traverse_mode TO profilesvc;


--
-- TOC entry 4489 (class 0 OID 0)
-- Dependencies: 232
-- Name: TABLE profile; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.profile TO profilesvc;


--
-- TOC entry 4490 (class 0 OID 0)
-- Dependencies: 224
-- Name: SEQUENCE profile_id_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.profile_id_seq TO profilesvc;


--
-- TOC entry 4491 (class 0 OID 0)
-- Dependencies: 212
-- Name: TABLE raster_columns; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.raster_columns TO profilesvc;


--
-- TOC entry 4492 (class 0 OID 0)
-- Dependencies: 213
-- Name: TABLE raster_overviews; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.raster_overviews TO profilesvc;


--
-- TOC entry 4493 (class 0 OID 0)
-- Dependencies: 225
-- Name: TABLE review; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.review TO profilesvc;


--
-- TOC entry 4494 (class 0 OID 0)
-- Dependencies: 226
-- Name: SEQUENCE review_id_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.review_id_seq TO profilesvc;


--
-- TOC entry 4495 (class 0 OID 0)
-- Dependencies: 227
-- Name: TABLE rideshare_luggage; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.rideshare_luggage TO profilesvc;


--
-- TOC entry 4496 (class 0 OID 0)
-- Dependencies: 228
-- Name: TABLE rideshare_preferences; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.rideshare_preferences TO profilesvc;


--
-- TOC entry 4497 (class 0 OID 0)
-- Dependencies: 229
-- Name: TABLE search_preferences; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.search_preferences TO profilesvc;


--
-- TOC entry 4498 (class 0 OID 0)
-- Dependencies: 200
-- Name: TABLE spatial_ref_sys; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.spatial_ref_sys TO profilesvc;


--
-- TOC entry 4499 (class 0 OID 0)
-- Dependencies: 235
-- Name: TABLE survey; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.survey TO profilesvc;


--
-- TOC entry 4500 (class 0 OID 0)
-- Dependencies: 237
-- Name: SEQUENCE survey_id_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.survey_id_seq TO profilesvc;


--
-- TOC entry 4501 (class 0 OID 0)
-- Dependencies: 236
-- Name: TABLE survey_interaction; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.survey_interaction TO profilesvc;


--
-- TOC entry 4502 (class 0 OID 0)
-- Dependencies: 239
-- Name: SEQUENCE survey_interaction_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.survey_interaction_seq TO profilesvc;


--
-- TOC entry 4503 (class 0 OID 0)
-- Dependencies: 240
-- Name: TABLE user_session; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.user_session TO profilesvc;


--
-- TOC entry 4504 (class 0 OID 0)
-- Dependencies: 241
-- Name: SEQUENCE user_session_id_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.user_session_id_seq TO profilesvc;


--
-- TOC entry 3236 (class 826 OID 295800)
-- Name: DEFAULT PRIVILEGES FOR SEQUENCES; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public REVOKE ALL ON SEQUENCES  FROM postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON SEQUENCES  TO profilesvc;


--
-- TOC entry 3235 (class 826 OID 295799)
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public REVOKE ALL ON TABLES  FROM postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT SELECT,INSERT,DELETE,UPDATE ON TABLES  TO profilesvc;


-- Completed on 2022-06-23 09:31:32

--
-- PostgreSQL database dump complete
--


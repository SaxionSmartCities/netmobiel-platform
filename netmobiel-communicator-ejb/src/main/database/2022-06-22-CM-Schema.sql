--
-- PostgreSQL database dump
--

-- Dumped from database version 10.18
-- Dumped by pg_dump version 10.18

-- Started on 2022-06-23 09:29:03

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
-- TOC entry 1 (class 3079 OID 12924)
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- TOC entry 2843 (class 0 OID 0)
-- Dependencies: 1
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 196 (class 1259 OID 87276)
-- Name: cm_user; Type: TABLE; Schema: public; Owner: communicator
--

CREATE TABLE public.cm_user (
    id bigint NOT NULL,
    family_name character varying(64),
    given_name character varying(32),
    managed_identity character varying(36) NOT NULL,
    email character varying(64),
    fcm_token character varying(512),
    fcm_token_timestamp timestamp without time zone,
    phone_number character varying(16),
    country_code character varying(3)
);


ALTER TABLE public.cm_user OWNER TO communicator;

--
-- TOC entry 202 (class 1259 OID 478501)
-- Name: conversation; Type: TABLE; Schema: public; Owner: communicator
--

CREATE TABLE public.conversation (
    id bigint NOT NULL,
    topic character varying(256) NOT NULL,
    created_time timestamp without time zone,
    archived_time timestamp without time zone,
    owner bigint NOT NULL,
    owner_role character varying(2) NOT NULL,
    initial_context character varying(64) NOT NULL
);


ALTER TABLE public.conversation OWNER TO communicator;

--
-- TOC entry 204 (class 1259 OID 481547)
-- Name: conversation_context; Type: TABLE; Schema: public; Owner: communicator
--

CREATE TABLE public.conversation_context (
    conversation bigint NOT NULL,
    context character varying(64)
);


ALTER TABLE public.conversation_context OWNER TO communicator;

--
-- TOC entry 203 (class 1259 OID 478513)
-- Name: conversation_id_seq; Type: SEQUENCE; Schema: public; Owner: communicator
--

CREATE SEQUENCE public.conversation_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.conversation_id_seq OWNER TO communicator;

--
-- TOC entry 197 (class 1259 OID 87279)
-- Name: envelope; Type: TABLE; Schema: public; Owner: communicator
--

CREATE TABLE public.envelope (
    id bigint NOT NULL,
    ack_time timestamp without time zone,
    message bigint NOT NULL,
    push_time timestamp without time zone,
    context character varying(32) NOT NULL,
    conversation bigint NOT NULL,
    sender boolean DEFAULT false NOT NULL
);


ALTER TABLE public.envelope OWNER TO communicator;

--
-- TOC entry 198 (class 1259 OID 87282)
-- Name: envelope_id_seq; Type: SEQUENCE; Schema: public; Owner: communicator
--

CREATE SEQUENCE public.envelope_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.envelope_id_seq OWNER TO communicator;

--
-- TOC entry 199 (class 1259 OID 87284)
-- Name: message; Type: TABLE; Schema: public; Owner: communicator
--

CREATE TABLE public.message (
    id bigint NOT NULL,
    body character varying(1024) NOT NULL,
    context character varying(32) NOT NULL,
    created_time timestamp without time zone NOT NULL,
    delivery_mode character varying(2) NOT NULL
);


ALTER TABLE public.message OWNER TO communicator;

--
-- TOC entry 200 (class 1259 OID 87290)
-- Name: message_id_seq; Type: SEQUENCE; Schema: public; Owner: communicator
--

CREATE SEQUENCE public.message_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.message_id_seq OWNER TO communicator;

--
-- TOC entry 201 (class 1259 OID 87292)
-- Name: user_id_seq; Type: SEQUENCE; Schema: public; Owner: communicator
--

CREATE SEQUENCE public.user_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.user_id_seq OWNER TO communicator;

--
-- TOC entry 2698 (class 2606 OID 87295)
-- Name: cm_user cm_user_pkey; Type: CONSTRAINT; Schema: public; Owner: communicator
--

ALTER TABLE ONLY public.cm_user
    ADD CONSTRAINT cm_user_pkey PRIMARY KEY (id);


--
-- TOC entry 2708 (class 2606 OID 478507)
-- Name: conversation conversation_pkey; Type: CONSTRAINT; Schema: public; Owner: communicator
--

ALTER TABLE ONLY public.conversation
    ADD CONSTRAINT conversation_pkey PRIMARY KEY (id);


--
-- TOC entry 2700 (class 2606 OID 87297)
-- Name: cm_user cs_managed_identity_unique; Type: CONSTRAINT; Schema: public; Owner: communicator
--

ALTER TABLE ONLY public.cm_user
    ADD CONSTRAINT cs_managed_identity_unique UNIQUE (managed_identity);


--
-- TOC entry 2710 (class 2606 OID 647600)
-- Name: conversation cs_unique_conversation; Type: CONSTRAINT; Schema: public; Owner: communicator
--

ALTER TABLE ONLY public.conversation
    ADD CONSTRAINT cs_unique_conversation UNIQUE (initial_context, owner);


--
-- TOC entry 2702 (class 2606 OID 506680)
-- Name: envelope cs_unique_conversation_message; Type: CONSTRAINT; Schema: public; Owner: communicator
--

ALTER TABLE ONLY public.envelope
    ADD CONSTRAINT cs_unique_conversation_message UNIQUE (conversation, message);


--
-- TOC entry 2704 (class 2606 OID 87301)
-- Name: envelope envelope_pkey; Type: CONSTRAINT; Schema: public; Owner: communicator
--

ALTER TABLE ONLY public.envelope
    ADD CONSTRAINT envelope_pkey PRIMARY KEY (id);


--
-- TOC entry 2706 (class 2606 OID 87303)
-- Name: message message_pkey; Type: CONSTRAINT; Schema: public; Owner: communicator
--

ALTER TABLE ONLY public.message
    ADD CONSTRAINT message_pkey PRIMARY KEY (id);


--
-- TOC entry 2714 (class 2606 OID 481550)
-- Name: conversation_context conversation_context_conversation_fk; Type: FK CONSTRAINT; Schema: public; Owner: communicator
--

ALTER TABLE ONLY public.conversation_context
    ADD CONSTRAINT conversation_context_conversation_fk FOREIGN KEY (conversation) REFERENCES public.conversation(id) ON DELETE CASCADE;


--
-- TOC entry 2713 (class 2606 OID 478508)
-- Name: conversation conversation_owner_fk; Type: FK CONSTRAINT; Schema: public; Owner: communicator
--

ALTER TABLE ONLY public.conversation
    ADD CONSTRAINT conversation_owner_fk FOREIGN KEY (owner) REFERENCES public.cm_user(id);


--
-- TOC entry 2712 (class 2606 OID 478520)
-- Name: envelope envelope_conversation_fk; Type: FK CONSTRAINT; Schema: public; Owner: communicator
--

ALTER TABLE ONLY public.envelope
    ADD CONSTRAINT envelope_conversation_fk FOREIGN KEY (conversation) REFERENCES public.conversation(id);


--
-- TOC entry 2711 (class 2606 OID 470309)
-- Name: envelope envelope_message_fk; Type: FK CONSTRAINT; Schema: public; Owner: communicator
--

ALTER TABLE ONLY public.envelope
    ADD CONSTRAINT envelope_message_fk FOREIGN KEY (message) REFERENCES public.message(id) ON DELETE CASCADE;


--
-- TOC entry 1693 (class 826 OID 86859)
-- Name: DEFAULT PRIVILEGES FOR SEQUENCES; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public REVOKE ALL ON SEQUENCES  FROM postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON SEQUENCES  TO communicator;


--
-- TOC entry 1694 (class 826 OID 86860)
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public REVOKE ALL ON TABLES  FROM postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT SELECT,INSERT,DELETE,UPDATE ON TABLES  TO communicator;


-- Completed on 2022-06-23 09:29:04

--
-- PostgreSQL database dump complete
--


--
-- PostgreSQL database dump
--

-- Dumped from database version 10.10
-- Dumped by pg_dump version 10.10

-- Started on 2020-03-03 13:59:40

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
-- TOC entry 2826 (class 0 OID 0)
-- Dependencies: 1
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 199 (class 1259 OID 87239)
-- Name: cm_user; Type: TABLE; Schema: public; Owner: communicator
--

CREATE TABLE public.cm_user (
    id bigint NOT NULL,
    family_name character varying(64),
    given_name character varying(32),
    managed_identity character varying(36) NOT NULL
);


ALTER TABLE public.cm_user OWNER TO communicator;

--
-- TOC entry 200 (class 1259 OID 87244)
-- Name: envelope; Type: TABLE; Schema: public; Owner: communicator
--

CREATE TABLE public.envelope (
    id bigint NOT NULL,
    ack_time timestamp without time zone,
    message bigint NOT NULL,
    recipient bigint NOT NULL
);


ALTER TABLE public.envelope OWNER TO communicator;

--
-- TOC entry 196 (class 1259 OID 87233)
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
-- TOC entry 201 (class 1259 OID 87249)
-- Name: message; Type: TABLE; Schema: public; Owner: communicator
--

CREATE TABLE public.message (
    id bigint NOT NULL,
    body character varying(1024),
    context character varying(32) NOT NULL,
    created_time timestamp without time zone NOT NULL,
    delivery_mode character varying(2) NOT NULL,
    subject character varying(128) NOT NULL,
    sender bigint NOT NULL
);


ALTER TABLE public.message OWNER TO communicator;

--
-- TOC entry 197 (class 1259 OID 87235)
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
-- TOC entry 198 (class 1259 OID 87237)
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
-- TOC entry 2686 (class 2606 OID 87243)
-- Name: cm_user cm_user_pkey; Type: CONSTRAINT; Schema: public; Owner: communicator
--

ALTER TABLE ONLY public.cm_user
    ADD CONSTRAINT cm_user_pkey PRIMARY KEY (id);


--
-- TOC entry 2688 (class 2606 OID 87258)
-- Name: cm_user cs_managed_identity_unique; Type: CONSTRAINT; Schema: public; Owner: communicator
--

ALTER TABLE ONLY public.cm_user
    ADD CONSTRAINT cs_managed_identity_unique UNIQUE (managed_identity);


--
-- TOC entry 2690 (class 2606 OID 87260)
-- Name: envelope cs_unique_message_recipient; Type: CONSTRAINT; Schema: public; Owner: communicator
--

ALTER TABLE ONLY public.envelope
    ADD CONSTRAINT cs_unique_message_recipient UNIQUE (recipient, message);


--
-- TOC entry 2692 (class 2606 OID 87248)
-- Name: envelope envelope_pkey; Type: CONSTRAINT; Schema: public; Owner: communicator
--

ALTER TABLE ONLY public.envelope
    ADD CONSTRAINT envelope_pkey PRIMARY KEY (id);


--
-- TOC entry 2694 (class 2606 OID 87256)
-- Name: message message_pkey; Type: CONSTRAINT; Schema: public; Owner: communicator
--

ALTER TABLE ONLY public.message
    ADD CONSTRAINT message_pkey PRIMARY KEY (id);


--
-- TOC entry 2695 (class 2606 OID 87261)
-- Name: envelope envelope_message_fk; Type: FK CONSTRAINT; Schema: public; Owner: communicator
--

ALTER TABLE ONLY public.envelope
    ADD CONSTRAINT envelope_message_fk FOREIGN KEY (message) REFERENCES public.message(id);


--
-- TOC entry 2696 (class 2606 OID 87266)
-- Name: envelope message_recipient_fk; Type: FK CONSTRAINT; Schema: public; Owner: communicator
--

ALTER TABLE ONLY public.envelope
    ADD CONSTRAINT message_recipient_fk FOREIGN KEY (recipient) REFERENCES public.cm_user(id);


--
-- TOC entry 2697 (class 2606 OID 87271)
-- Name: message message_sender_fk; Type: FK CONSTRAINT; Schema: public; Owner: communicator
--

ALTER TABLE ONLY public.message
    ADD CONSTRAINT message_sender_fk FOREIGN KEY (sender) REFERENCES public.cm_user(id);


--
-- TOC entry 1683 (class 826 OID 85923)
-- Name: DEFAULT PRIVILEGES FOR SEQUENCES; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public REVOKE ALL ON SEQUENCES  FROM postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON SEQUENCES  TO communicator;


--
-- TOC entry 1682 (class 826 OID 85922)
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public REVOKE ALL ON TABLES  FROM postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT SELECT,INSERT,DELETE,UPDATE ON TABLES  TO communicator;


-- Completed on 2020-03-03 13:59:40

--
-- PostgreSQL database dump complete
--


--
-- PostgreSQL database dump
--

-- Dumped from database version 10.10
-- Dumped by pg_dump version 10.10

-- Started on 2020-02-27 12:10:47

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
-- TOC entry 2814 (class 0 OID 0)
-- Dependencies: 1
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 198 (class 1259 OID 86814)
-- Name: envelope; Type: TABLE; Schema: public; Owner: communicator
--

CREATE TABLE public.envelope (
    id bigint NOT NULL,
    ack_time timestamp without time zone,
    recipient character varying(64) NOT NULL,
    message bigint NOT NULL
);


ALTER TABLE public.envelope OWNER TO communicator;

--
-- TOC entry 196 (class 1259 OID 86810)
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
-- TOC entry 199 (class 1259 OID 86819)
-- Name: message; Type: TABLE; Schema: public; Owner: communicator
--

CREATE TABLE public.message (
    id bigint NOT NULL,
    body character varying(1024),
    context character varying(32) NOT NULL,
    created_time timestamp without time zone NOT NULL,
    delivery_mode character varying(2) NOT NULL,
    sender character varying(64) NOT NULL,
    subject character varying(128) NOT NULL
);

ALTER TABLE public.message OWNER TO communicator;

--
-- TOC entry 197 (class 1259 OID 86812)
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
-- TOC entry 2680 (class 2606 OID 86828)
-- Name: envelope cs_unique_message_recipient; Type: CONSTRAINT; Schema: public; Owner: communicator
--

ALTER TABLE ONLY public.envelope
    ADD CONSTRAINT cs_unique_message_recipient UNIQUE (recipient, message);


--
-- TOC entry 2682 (class 2606 OID 86818)
-- Name: envelope envelope_pkey; Type: CONSTRAINT; Schema: public; Owner: communicator
--

ALTER TABLE ONLY public.envelope
    ADD CONSTRAINT envelope_pkey PRIMARY KEY (id);


--
-- TOC entry 2684 (class 2606 OID 86826)
-- Name: message message_pkey; Type: CONSTRAINT; Schema: public; Owner: communicator
--

ALTER TABLE ONLY public.message
    ADD CONSTRAINT message_pkey PRIMARY KEY (id);


--
-- TOC entry 2685 (class 2606 OID 86829)
-- Name: envelope envelope_message_fk; Type: FK CONSTRAINT; Schema: public; Owner: communicator
--

ALTER TABLE ONLY public.envelope
    ADD CONSTRAINT envelope_message_fk FOREIGN KEY (message) REFERENCES public.message(id);


--
-- TOC entry 1677 (class 826 OID 85923)
-- Name: DEFAULT PRIVILEGES FOR SEQUENCES; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public REVOKE ALL ON SEQUENCES  FROM postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON SEQUENCES  TO communicator;


--
-- TOC entry 1676 (class 826 OID 85922)
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public REVOKE ALL ON TABLES  FROM postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT SELECT,INSERT,DELETE,UPDATE ON TABLES  TO communicator;


-- Completed on 2020-02-27 12:10:47

--
-- PostgreSQL database dump complete
--


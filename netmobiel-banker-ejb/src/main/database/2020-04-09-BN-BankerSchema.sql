--
-- PostgreSQL database dump
--

-- Dumped from database version 10.10
-- Dumped by pg_dump version 10.10

-- Started on 2020-04-09 13:50:01

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
-- TOC entry 2857 (class 0 OID 0)
-- Dependencies: 1
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 202 (class 1259 OID 100838)
-- Name: account; Type: TABLE; Schema: public; Owner: banker
--

CREATE TABLE public.account (
    id bigint NOT NULL,
    account_type character varying(1) NOT NULL,
    closed_time timestamp without time zone,
    created_time timestamp without time zone NOT NULL,
    reference character varying(32) NOT NULL,
    holder bigint NOT NULL
);


ALTER TABLE public.account OWNER TO banker;

--
-- TOC entry 196 (class 1259 OID 100826)
-- Name: account_seq; Type: SEQUENCE; Schema: public; Owner: banker
--

CREATE SEQUENCE public.account_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.account_seq OWNER TO banker;

--
-- TOC entry 203 (class 1259 OID 100843)
-- Name: accounting_entry; Type: TABLE; Schema: public; Owner: banker
--

CREATE TABLE public.accounting_entry (
    id bigint NOT NULL,
    amount integer NOT NULL,
    entry_type character varying(1) NOT NULL,
    account bigint NOT NULL,
    transaction bigint NOT NULL
);


ALTER TABLE public.accounting_entry OWNER TO banker;

--
-- TOC entry 197 (class 1259 OID 100828)
-- Name: accounting_entry_seq; Type: SEQUENCE; Schema: public; Owner: banker
--

CREATE SEQUENCE public.accounting_entry_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.accounting_entry_seq OWNER TO banker;

--
-- TOC entry 204 (class 1259 OID 100848)
-- Name: accounting_transaction; Type: TABLE; Schema: public; Owner: banker
--

CREATE TABLE public.accounting_transaction (
    id bigint NOT NULL,
    accounting_time timestamp without time zone NOT NULL,
    description character varying(256) NOT NULL,
    transaction_time timestamp without time zone NOT NULL,
    ledger bigint NOT NULL
);


ALTER TABLE public.accounting_transaction OWNER TO banker;

--
-- TOC entry 198 (class 1259 OID 100830)
-- Name: accounting_transaction_seq; Type: SEQUENCE; Schema: public; Owner: banker
--

CREATE SEQUENCE public.accounting_transaction_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.accounting_transaction_seq OWNER TO banker;

--
-- TOC entry 205 (class 1259 OID 100853)
-- Name: balance; Type: TABLE; Schema: public; Owner: banker
--

CREATE TABLE public.balance (
    id bigint NOT NULL,
    end_amount integer NOT NULL,
    modified_time timestamp without time zone NOT NULL,
    start_amount integer NOT NULL,
    version integer,
    account bigint NOT NULL,
    ledger bigint NOT NULL
);


ALTER TABLE public.balance OWNER TO banker;

--
-- TOC entry 199 (class 1259 OID 100832)
-- Name: balance_seq; Type: SEQUENCE; Schema: public; Owner: banker
--

CREATE SEQUENCE public.balance_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.balance_seq OWNER TO banker;

--
-- TOC entry 206 (class 1259 OID 100858)
-- Name: bn_user; Type: TABLE; Schema: public; Owner: banker
--

CREATE TABLE public.bn_user (
    id bigint NOT NULL,
    family_name character varying(64),
    given_name character varying(32),
    managed_identity character varying(36) NOT NULL
);


ALTER TABLE public.bn_user OWNER TO banker;

--
-- TOC entry 207 (class 1259 OID 100863)
-- Name: ledger; Type: TABLE; Schema: public; Owner: banker
--

CREATE TABLE public.ledger (
    id bigint NOT NULL,
    end_period timestamp without time zone,
    name character varying(32) NOT NULL,
    start_period timestamp without time zone NOT NULL
);


ALTER TABLE public.ledger OWNER TO banker;

--
-- TOC entry 200 (class 1259 OID 100834)
-- Name: ledger_seq; Type: SEQUENCE; Schema: public; Owner: banker
--

CREATE SEQUENCE public.ledger_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ledger_seq OWNER TO banker;

--
-- TOC entry 201 (class 1259 OID 100836)
-- Name: user_id_seq; Type: SEQUENCE; Schema: public; Owner: banker
--

CREATE SEQUENCE public.user_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.user_id_seq OWNER TO banker;

--
-- TOC entry 2701 (class 2606 OID 100842)
-- Name: account account_pkey; Type: CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.account
    ADD CONSTRAINT account_pkey PRIMARY KEY (id);


--
-- TOC entry 2705 (class 2606 OID 100847)
-- Name: accounting_entry accounting_entry_pkey; Type: CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.accounting_entry
    ADD CONSTRAINT accounting_entry_pkey PRIMARY KEY (id);


--
-- TOC entry 2709 (class 2606 OID 100852)
-- Name: accounting_transaction accounting_transaction_pkey; Type: CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.accounting_transaction
    ADD CONSTRAINT accounting_transaction_pkey PRIMARY KEY (id);


--
-- TOC entry 2711 (class 2606 OID 100857)
-- Name: balance balance_pkey; Type: CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.balance
    ADD CONSTRAINT balance_pkey PRIMARY KEY (id);


--
-- TOC entry 2715 (class 2606 OID 100862)
-- Name: bn_user bn_user_pkey; Type: CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.bn_user
    ADD CONSTRAINT bn_user_pkey PRIMARY KEY (id);


--
-- TOC entry 2703 (class 2606 OID 100869)
-- Name: account cs_account_unique; Type: CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.account
    ADD CONSTRAINT cs_account_unique UNIQUE (reference);


--
-- TOC entry 2713 (class 2606 OID 100873)
-- Name: balance cs_balance_unique; Type: CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.balance
    ADD CONSTRAINT cs_balance_unique UNIQUE (account, ledger);


--
-- TOC entry 2719 (class 2606 OID 100877)
-- Name: ledger cs_ledger_unique; Type: CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.ledger
    ADD CONSTRAINT cs_ledger_unique UNIQUE (name);


--
-- TOC entry 2717 (class 2606 OID 100875)
-- Name: bn_user cs_managed_identity_unique; Type: CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.bn_user
    ADD CONSTRAINT cs_managed_identity_unique UNIQUE (managed_identity);


--
-- TOC entry 2707 (class 2606 OID 100871)
-- Name: accounting_entry cs_transaction_account_unique; Type: CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.accounting_entry
    ADD CONSTRAINT cs_transaction_account_unique UNIQUE (account, transaction);


--
-- TOC entry 2721 (class 2606 OID 100867)
-- Name: ledger ledger_pkey; Type: CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.ledger
    ADD CONSTRAINT ledger_pkey PRIMARY KEY (id);


--
-- TOC entry 2722 (class 2606 OID 100878)
-- Name: account account_user_fk; Type: FK CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.account
    ADD CONSTRAINT account_user_fk FOREIGN KEY (holder) REFERENCES public.bn_user(id);


--
-- TOC entry 2723 (class 2606 OID 100883)
-- Name: accounting_entry accounting_entry_account_fk; Type: FK CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.accounting_entry
    ADD CONSTRAINT accounting_entry_account_fk FOREIGN KEY (account) REFERENCES public.account(id);


--
-- TOC entry 2724 (class 2606 OID 100888)
-- Name: accounting_entry accounting_entry_transaction_fk; Type: FK CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.accounting_entry
    ADD CONSTRAINT accounting_entry_transaction_fk FOREIGN KEY (transaction) REFERENCES public.accounting_transaction(id);


--
-- TOC entry 2725 (class 2606 OID 100893)
-- Name: accounting_transaction accounting_transaction_ledger_fk; Type: FK CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.accounting_transaction
    ADD CONSTRAINT accounting_transaction_ledger_fk FOREIGN KEY (ledger) REFERENCES public.ledger(id);


--
-- TOC entry 2726 (class 2606 OID 100898)
-- Name: balance balance_account_fk; Type: FK CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.balance
    ADD CONSTRAINT balance_account_fk FOREIGN KEY (account) REFERENCES public.account(id);


--
-- TOC entry 2727 (class 2606 OID 100903)
-- Name: balance balance_ledger_fk; Type: FK CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.balance
    ADD CONSTRAINT balance_ledger_fk FOREIGN KEY (ledger) REFERENCES public.ledger(id);


-- Completed on 2020-04-09 13:50:01

--
-- PostgreSQL database dump complete
--


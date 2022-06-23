--
-- PostgreSQL database dump
--

-- Dumped from database version 10.18
-- Dumped by pg_dump version 10.18

-- Started on 2022-06-23 09:13:46

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
-- TOC entry 4499 (class 0 OID 0)
-- Dependencies: 4498
-- Name: DATABASE banker_dev; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON DATABASE banker_dev IS 'Credit service database';


--
-- TOC entry 10 (class 2615 OID 220610)
-- Name: topology; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA topology;


ALTER SCHEMA topology OWNER TO postgres;

--
-- TOC entry 4501 (class 0 OID 0)
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
-- TOC entry 4502 (class 0 OID 0)
-- Dependencies: 1
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


--
-- TOC entry 2 (class 3079 OID 219111)
-- Name: postgis; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;


--
-- TOC entry 4503 (class 0 OID 0)
-- Dependencies: 2
-- Name: EXTENSION postgis; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION postgis IS 'PostGIS geometry, geography, and raster spatial types and functions';


--
-- TOC entry 3 (class 3079 OID 220611)
-- Name: postgis_topology; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS postgis_topology WITH SCHEMA topology;


--
-- TOC entry 4504 (class 0 OID 0)
-- Dependencies: 3
-- Name: EXTENSION postgis_topology; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION postgis_topology IS 'PostGIS topology spatial types and functions';


SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 199 (class 1259 OID 101569)
-- Name: account; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.account (
    id bigint NOT NULL,
    account_type character varying(1) NOT NULL,
    closed_time timestamp without time zone,
    created_time timestamp without time zone NOT NULL,
    ncan character varying(32) NOT NULL,
    name character varying(96) NOT NULL,
    iban character varying(48),
    iban_holder character varying(96),
    purpose character varying(1) NOT NULL
);


ALTER TABLE public.account OWNER TO postgres;

--
-- TOC entry 200 (class 1259 OID 101572)
-- Name: account_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.account_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.account_seq OWNER TO postgres;

--
-- TOC entry 201 (class 1259 OID 101574)
-- Name: accounting_entry; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.accounting_entry (
    id bigint NOT NULL,
    amount integer NOT NULL,
    entry_type character varying(1) NOT NULL,
    account bigint NOT NULL,
    transaction bigint NOT NULL,
    counterparty bigint NOT NULL,
    purpose character varying(2) NOT NULL
);


ALTER TABLE public.accounting_entry OWNER TO postgres;

--
-- TOC entry 202 (class 1259 OID 101577)
-- Name: accounting_entry_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.accounting_entry_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.accounting_entry_seq OWNER TO postgres;

--
-- TOC entry 203 (class 1259 OID 101579)
-- Name: accounting_transaction; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.accounting_transaction (
    id bigint NOT NULL,
    accounting_time timestamp without time zone NOT NULL,
    description character varying(256) NOT NULL,
    transaction_time timestamp without time zone NOT NULL,
    ledger bigint NOT NULL,
    context character varying(64),
    is_rollback boolean DEFAULT false,
    head bigint
);


ALTER TABLE public.accounting_transaction OWNER TO postgres;

--
-- TOC entry 204 (class 1259 OID 101582)
-- Name: accounting_transaction_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.accounting_transaction_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.accounting_transaction_seq OWNER TO postgres;

--
-- TOC entry 205 (class 1259 OID 101584)
-- Name: balance; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.balance OWNER TO postgres;

--
-- TOC entry 206 (class 1259 OID 101587)
-- Name: balance_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.balance_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.balance_seq OWNER TO postgres;

--
-- TOC entry 207 (class 1259 OID 101589)
-- Name: bn_user; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.bn_user (
    id bigint NOT NULL,
    family_name character varying(64),
    given_name character varying(32),
    managed_identity character varying(36) NOT NULL,
    email character varying(64),
    personal_account bigint,
    premium_account bigint
);


ALTER TABLE public.bn_user OWNER TO postgres;

--
-- TOC entry 234 (class 1259 OID 220752)
-- Name: charity; Type: TABLE; Schema: public; Owner: banker
--

CREATE TABLE public.charity (
    id bigint NOT NULL,
    description character varying(256),
    image_url character varying(256),
    goal_amount integer NOT NULL,
    donated_amount integer NOT NULL,
    account bigint NOT NULL,
    label character varying(128) NOT NULL,
    point public.geometry NOT NULL,
    campaign_start_time timestamp without time zone NOT NULL,
    campaign_end_time timestamp without time zone,
    name character varying(96) NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    version integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.charity OWNER TO banker;

--
-- TOC entry 235 (class 1259 OID 220758)
-- Name: charity_seq; Type: SEQUENCE; Schema: public; Owner: banker
--

CREATE SEQUENCE public.charity_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.charity_seq OWNER TO banker;

--
-- TOC entry 236 (class 1259 OID 220791)
-- Name: charity_user_role; Type: TABLE; Schema: public; Owner: banker
--

CREATE TABLE public.charity_user_role (
    bn_user bigint NOT NULL,
    charity bigint NOT NULL,
    created_time timestamp without time zone NOT NULL,
    modified_time timestamp without time zone NOT NULL,
    role character varying(1) NOT NULL
);


ALTER TABLE public.charity_user_role OWNER TO banker;

--
-- TOC entry 211 (class 1259 OID 209643)
-- Name: deposit_request; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.deposit_request (
    id bigint NOT NULL,
    amount_credits integer NOT NULL,
    amount_eurocents integer,
    completed_time timestamp without time zone,
    creation_time timestamp without time zone NOT NULL,
    description character varying(128) NOT NULL,
    expiration_time timestamp without time zone NOT NULL,
    merchant_order_id character varying(48),
    payment_link_id character varying(48),
    status character varying(1) NOT NULL,
    account bigint NOT NULL,
    transaction bigint
);


ALTER TABLE public.deposit_request OWNER TO postgres;

--
-- TOC entry 212 (class 1259 OID 209655)
-- Name: deposit_request_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.deposit_request_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.deposit_request_seq OWNER TO postgres;

--
-- TOC entry 237 (class 1259 OID 228938)
-- Name: donation; Type: TABLE; Schema: public; Owner: banker
--

CREATE TABLE public.donation (
    id bigint NOT NULL,
    description character varying(256),
    amount integer NOT NULL,
    donation_time timestamp without time zone NOT NULL,
    charity bigint NOT NULL,
    bn_user bigint NOT NULL,
    anonymous boolean NOT NULL
);


ALTER TABLE public.donation OWNER TO banker;

--
-- TOC entry 238 (class 1259 OID 228941)
-- Name: donation_seq; Type: SEQUENCE; Schema: public; Owner: banker
--

CREATE SEQUENCE public.donation_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.donation_seq OWNER TO banker;

--
-- TOC entry 243 (class 1259 OID 573537)
-- Name: incentive; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.incentive (
    id bigint NOT NULL,
    code character varying(16) NOT NULL,
    description character varying(256) NOT NULL,
    amount integer NOT NULL,
    category character varying(16) NOT NULL,
    relative boolean DEFAULT false NOT NULL,
    redemption boolean DEFAULT false NOT NULL,
    max_amount integer,
    disable_time timestamp without time zone,
    cta_enabled boolean DEFAULT false NOT NULL,
    cta_title character varying(128),
    cta_body character varying(256),
    cta_button_label character varying(48),
    cta_button_action character varying(32),
    cta_hide_beyond_reward_count integer,
    start_time timestamp without time zone,
    end_time timestamp without time zone
);


ALTER TABLE public.incentive OWNER TO postgres;

--
-- TOC entry 244 (class 1259 OID 573540)
-- Name: incentive_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.incentive_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.incentive_seq OWNER TO postgres;

--
-- TOC entry 208 (class 1259 OID 101592)
-- Name: ledger; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ledger (
    id bigint NOT NULL,
    end_period timestamp without time zone,
    name character varying(32) NOT NULL,
    start_period timestamp without time zone NOT NULL
);


ALTER TABLE public.ledger OWNER TO postgres;

--
-- TOC entry 209 (class 1259 OID 101595)
-- Name: ledger_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.ledger_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ledger_seq OWNER TO postgres;

--
-- TOC entry 239 (class 1259 OID 254928)
-- Name: payment_batch; Type: TABLE; Schema: public; Owner: banker
--

CREATE TABLE public.payment_batch (
    id bigint NOT NULL,
    creation_time timestamp without time zone NOT NULL,
    created_by bigint NOT NULL,
    modification_time timestamp without time zone NOT NULL,
    modified_by bigint NOT NULL,
    order_reference character varying(32),
    status character varying(1) NOT NULL,
    reason character varying(256),
    originator_iban character varying(48) NOT NULL,
    originator_iban_holder character varying(96) NOT NULL,
    originator_account bigint NOT NULL,
    version integer DEFAULT 0 NOT NULL,
    nr_requests integer DEFAULT 0 NOT NULL,
    amount_requested_eurocents integer DEFAULT 0 NOT NULL,
    amount_settled_eurocents integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.payment_batch OWNER TO banker;

--
-- TOC entry 240 (class 1259 OID 254943)
-- Name: payment_batch_seq; Type: SEQUENCE; Schema: public; Owner: banker
--

CREATE SEQUENCE public.payment_batch_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.payment_batch_seq OWNER TO banker;

--
-- TOC entry 245 (class 1259 OID 573548)
-- Name: reward; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.reward (
    id bigint NOT NULL,
    amount integer NOT NULL,
    reward_time timestamp without time zone NOT NULL,
    cancel_time timestamp without time zone,
    transaction integer,
    recipient integer NOT NULL,
    incentive integer NOT NULL,
    fact_context character varying(64),
    paid_out boolean DEFAULT false NOT NULL,
    version integer DEFAULT 1 NOT NULL
);


ALTER TABLE public.reward OWNER TO postgres;

--
-- TOC entry 246 (class 1259 OID 573551)
-- Name: reward_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.reward_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.reward_seq OWNER TO postgres;

--
-- TOC entry 210 (class 1259 OID 101597)
-- Name: user_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.user_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.user_id_seq OWNER TO postgres;

--
-- TOC entry 241 (class 1259 OID 254945)
-- Name: withdrawal_request; Type: TABLE; Schema: public; Owner: banker
--

CREATE TABLE public.withdrawal_request (
    id bigint NOT NULL,
    amount_credits integer NOT NULL,
    amount_eurocents integer NOT NULL,
    creation_time timestamp without time zone NOT NULL,
    created_by bigint NOT NULL,
    modification_time timestamp without time zone NOT NULL,
    modified_by bigint NOT NULL,
    description character varying(128) NOT NULL,
    order_reference character varying(32),
    status character varying(1) NOT NULL,
    account bigint NOT NULL,
    payment_batch bigint,
    transaction bigint,
    reason character varying(256),
    iban character varying(48) NOT NULL,
    iban_holder character varying(96) NOT NULL,
    version integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.withdrawal_request OWNER TO banker;

--
-- TOC entry 242 (class 1259 OID 254975)
-- Name: withdrawal_request_seq; Type: SEQUENCE; Schema: public; Owner: banker
--

CREATE SEQUENCE public.withdrawal_request_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.withdrawal_request_seq OWNER TO banker;

--
-- TOC entry 4291 (class 2606 OID 101600)
-- Name: account account_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account
    ADD CONSTRAINT account_pkey PRIMARY KEY (id);


--
-- TOC entry 4295 (class 2606 OID 101602)
-- Name: accounting_entry accounting_entry_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounting_entry
    ADD CONSTRAINT accounting_entry_pkey PRIMARY KEY (id);


--
-- TOC entry 4299 (class 2606 OID 101604)
-- Name: accounting_transaction accounting_transaction_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounting_transaction
    ADD CONSTRAINT accounting_transaction_pkey PRIMARY KEY (id);


--
-- TOC entry 4301 (class 2606 OID 101606)
-- Name: balance balance_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.balance
    ADD CONSTRAINT balance_pkey PRIMARY KEY (id);


--
-- TOC entry 4305 (class 2606 OID 101608)
-- Name: bn_user bn_user_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bn_user
    ADD CONSTRAINT bn_user_pkey PRIMARY KEY (id);


--
-- TOC entry 4321 (class 2606 OID 220761)
-- Name: charity charity_pkey; Type: CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.charity
    ADD CONSTRAINT charity_pkey PRIMARY KEY (id);


--
-- TOC entry 4323 (class 2606 OID 220795)
-- Name: charity_user_role charity_user_role_pkey; Type: CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.charity_user_role
    ADD CONSTRAINT charity_user_role_pkey PRIMARY KEY (bn_user, charity);


--
-- TOC entry 4293 (class 2606 OID 210186)
-- Name: account cs_account_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account
    ADD CONSTRAINT cs_account_unique UNIQUE (ncan);


--
-- TOC entry 4303 (class 2606 OID 101612)
-- Name: balance cs_balance_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.balance
    ADD CONSTRAINT cs_balance_unique UNIQUE (account, ledger);


--
-- TOC entry 4331 (class 2606 OID 573545)
-- Name: incentive cs_incentive_code_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incentive
    ADD CONSTRAINT cs_incentive_code_unique UNIQUE (code);


--
-- TOC entry 4313 (class 2606 OID 101614)
-- Name: ledger cs_ledger_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ledger
    ADD CONSTRAINT cs_ledger_unique UNIQUE (name);


--
-- TOC entry 4307 (class 2606 OID 101616)
-- Name: bn_user cs_managed_identity_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bn_user
    ADD CONSTRAINT cs_managed_identity_unique UNIQUE (managed_identity);


--
-- TOC entry 4309 (class 2606 OID 209637)
-- Name: bn_user cs_personal_account_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bn_user
    ADD CONSTRAINT cs_personal_account_unique UNIQUE (personal_account);


--
-- TOC entry 4311 (class 2606 OID 557141)
-- Name: bn_user cs_premium_account_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bn_user
    ADD CONSTRAINT cs_premium_account_unique UNIQUE (premium_account);


--
-- TOC entry 4335 (class 2606 OID 615684)
-- Name: reward cs_reward_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reward
    ADD CONSTRAINT cs_reward_unique UNIQUE (incentive, recipient, fact_context);


--
-- TOC entry 4297 (class 2606 OID 260263)
-- Name: accounting_entry cs_transaction_account_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounting_entry
    ADD CONSTRAINT cs_transaction_account_unique UNIQUE (account, transaction, counterparty);


--
-- TOC entry 4317 (class 2606 OID 209649)
-- Name: deposit_request deposit_request_payment_link_id_uc; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.deposit_request
    ADD CONSTRAINT deposit_request_payment_link_id_uc UNIQUE (payment_link_id);


--
-- TOC entry 4319 (class 2606 OID 209647)
-- Name: deposit_request deposit_request_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.deposit_request
    ADD CONSTRAINT deposit_request_pkey PRIMARY KEY (id);


--
-- TOC entry 4325 (class 2606 OID 228944)
-- Name: donation donation_pkey; Type: CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.donation
    ADD CONSTRAINT donation_pkey PRIMARY KEY (id);


--
-- TOC entry 4333 (class 2606 OID 573543)
-- Name: incentive incentive_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.incentive
    ADD CONSTRAINT incentive_pkey PRIMARY KEY (id);


--
-- TOC entry 4315 (class 2606 OID 101620)
-- Name: ledger ledger_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ledger
    ADD CONSTRAINT ledger_pkey PRIMARY KEY (id);


--
-- TOC entry 4327 (class 2606 OID 254932)
-- Name: payment_batch payment_batch_pkey; Type: CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.payment_batch
    ADD CONSTRAINT payment_batch_pkey PRIMARY KEY (id);


--
-- TOC entry 4337 (class 2606 OID 573554)
-- Name: reward reward_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reward
    ADD CONSTRAINT reward_pkey PRIMARY KEY (id);


--
-- TOC entry 4329 (class 2606 OID 254949)
-- Name: withdrawal_request withdrawal_request_pkey; Type: CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.withdrawal_request
    ADD CONSTRAINT withdrawal_request_pkey PRIMARY KEY (id);


--
-- TOC entry 4338 (class 2606 OID 101626)
-- Name: accounting_entry accounting_entry_account_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounting_entry
    ADD CONSTRAINT accounting_entry_account_fk FOREIGN KEY (account) REFERENCES public.account(id);


--
-- TOC entry 4340 (class 2606 OID 260257)
-- Name: accounting_entry accounting_entry_counterparty_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounting_entry
    ADD CONSTRAINT accounting_entry_counterparty_fk FOREIGN KEY (counterparty) REFERENCES public.account(id);


--
-- TOC entry 4339 (class 2606 OID 101631)
-- Name: accounting_entry accounting_entry_transaction_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounting_entry
    ADD CONSTRAINT accounting_entry_transaction_fk FOREIGN KEY (transaction) REFERENCES public.accounting_transaction(id);


--
-- TOC entry 4342 (class 2606 OID 555310)
-- Name: accounting_transaction accounting_transaction_head_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounting_transaction
    ADD CONSTRAINT accounting_transaction_head_fk FOREIGN KEY (head) REFERENCES public.accounting_transaction(id);


--
-- TOC entry 4341 (class 2606 OID 101636)
-- Name: accounting_transaction accounting_transaction_ledger_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounting_transaction
    ADD CONSTRAINT accounting_transaction_ledger_fk FOREIGN KEY (ledger) REFERENCES public.ledger(id);


--
-- TOC entry 4343 (class 2606 OID 101641)
-- Name: balance balance_account_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.balance
    ADD CONSTRAINT balance_account_fk FOREIGN KEY (account) REFERENCES public.account(id);


--
-- TOC entry 4344 (class 2606 OID 101646)
-- Name: balance balance_ledger_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.balance
    ADD CONSTRAINT balance_ledger_fk FOREIGN KEY (ledger) REFERENCES public.ledger(id);


--
-- TOC entry 4349 (class 2606 OID 220762)
-- Name: charity charity_account_fk; Type: FK CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.charity
    ADD CONSTRAINT charity_account_fk FOREIGN KEY (account) REFERENCES public.account(id);


--
-- TOC entry 4351 (class 2606 OID 220801)
-- Name: charity_user_role charity_user_role_charity_fk; Type: FK CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.charity_user_role
    ADD CONSTRAINT charity_user_role_charity_fk FOREIGN KEY (charity) REFERENCES public.charity(id);


--
-- TOC entry 4350 (class 2606 OID 220796)
-- Name: charity_user_role charity_user_role_user_fk; Type: FK CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.charity_user_role
    ADD CONSTRAINT charity_user_role_user_fk FOREIGN KEY (bn_user) REFERENCES public.bn_user(id);


--
-- TOC entry 4347 (class 2606 OID 209650)
-- Name: deposit_request deposit_request_account_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.deposit_request
    ADD CONSTRAINT deposit_request_account_fk FOREIGN KEY (account) REFERENCES public.account(id);


--
-- TOC entry 4348 (class 2606 OID 254977)
-- Name: deposit_request deposit_transaction_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.deposit_request
    ADD CONSTRAINT deposit_transaction_fk FOREIGN KEY (transaction) REFERENCES public.accounting_transaction(id);


--
-- TOC entry 4352 (class 2606 OID 228945)
-- Name: donation donation_charity_fk; Type: FK CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.donation
    ADD CONSTRAINT donation_charity_fk FOREIGN KEY (charity) REFERENCES public.charity(id);


--
-- TOC entry 4353 (class 2606 OID 228950)
-- Name: donation donation_user_fk; Type: FK CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.donation
    ADD CONSTRAINT donation_user_fk FOREIGN KEY (bn_user) REFERENCES public.bn_user(id);


--
-- TOC entry 4354 (class 2606 OID 254938)
-- Name: payment_batch payment_batch_created_by_fk; Type: FK CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.payment_batch
    ADD CONSTRAINT payment_batch_created_by_fk FOREIGN KEY (created_by) REFERENCES public.bn_user(id);


--
-- TOC entry 4355 (class 2606 OID 266498)
-- Name: payment_batch payment_batch_modified_by_fk; Type: FK CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.payment_batch
    ADD CONSTRAINT payment_batch_modified_by_fk FOREIGN KEY (modified_by) REFERENCES public.bn_user(id);


--
-- TOC entry 4356 (class 2606 OID 272359)
-- Name: payment_batch payment_batch_originator_account_fk; Type: FK CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.payment_batch
    ADD CONSTRAINT payment_batch_originator_account_fk FOREIGN KEY (originator_account) REFERENCES public.account(id);


--
-- TOC entry 4364 (class 2606 OID 573565)
-- Name: reward reward_incentive_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reward
    ADD CONSTRAINT reward_incentive_fk FOREIGN KEY (incentive) REFERENCES public.incentive(id);


--
-- TOC entry 4363 (class 2606 OID 573560)
-- Name: reward reward_recipient_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reward
    ADD CONSTRAINT reward_recipient_fk FOREIGN KEY (recipient) REFERENCES public.bn_user(id);


--
-- TOC entry 4362 (class 2606 OID 573555)
-- Name: reward reward_transaction_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reward
    ADD CONSTRAINT reward_transaction_fk FOREIGN KEY (transaction) REFERENCES public.accounting_transaction(id);


--
-- TOC entry 4345 (class 2606 OID 209638)
-- Name: bn_user user_personal_account_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bn_user
    ADD CONSTRAINT user_personal_account_fk FOREIGN KEY (personal_account) REFERENCES public.account(id);


--
-- TOC entry 4346 (class 2606 OID 557142)
-- Name: bn_user user_premium_account_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bn_user
    ADD CONSTRAINT user_premium_account_fk FOREIGN KEY (premium_account) REFERENCES public.account(id) ON DELETE SET NULL;


--
-- TOC entry 4357 (class 2606 OID 254950)
-- Name: withdrawal_request withdrawal_account_fk; Type: FK CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.withdrawal_request
    ADD CONSTRAINT withdrawal_account_fk FOREIGN KEY (account) REFERENCES public.account(id);


--
-- TOC entry 4359 (class 2606 OID 254965)
-- Name: withdrawal_request withdrawal_created_by_fk; Type: FK CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.withdrawal_request
    ADD CONSTRAINT withdrawal_created_by_fk FOREIGN KEY (created_by) REFERENCES public.bn_user(id);


--
-- TOC entry 4361 (class 2606 OID 266503)
-- Name: withdrawal_request withdrawal_modified_by_fk; Type: FK CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.withdrawal_request
    ADD CONSTRAINT withdrawal_modified_by_fk FOREIGN KEY (modified_by) REFERENCES public.bn_user(id);


--
-- TOC entry 4358 (class 2606 OID 254960)
-- Name: withdrawal_request withdrawal_payment_batch_fk; Type: FK CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.withdrawal_request
    ADD CONSTRAINT withdrawal_payment_batch_fk FOREIGN KEY (payment_batch) REFERENCES public.payment_batch(id);


--
-- TOC entry 4360 (class 2606 OID 254970)
-- Name: withdrawal_request withdrawal_transaction_fk; Type: FK CONSTRAINT; Schema: public; Owner: banker
--

ALTER TABLE ONLY public.withdrawal_request
    ADD CONSTRAINT withdrawal_transaction_fk FOREIGN KEY (transaction) REFERENCES public.accounting_transaction(id);


--
-- TOC entry 4505 (class 0 OID 0)
-- Dependencies: 199
-- Name: TABLE account; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.account TO banker;


--
-- TOC entry 4506 (class 0 OID 0)
-- Dependencies: 200
-- Name: SEQUENCE account_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.account_seq TO banker;


--
-- TOC entry 4507 (class 0 OID 0)
-- Dependencies: 201
-- Name: TABLE accounting_entry; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.accounting_entry TO banker;


--
-- TOC entry 4508 (class 0 OID 0)
-- Dependencies: 202
-- Name: SEQUENCE accounting_entry_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.accounting_entry_seq TO banker;


--
-- TOC entry 4509 (class 0 OID 0)
-- Dependencies: 203
-- Name: TABLE accounting_transaction; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.accounting_transaction TO banker;


--
-- TOC entry 4510 (class 0 OID 0)
-- Dependencies: 204
-- Name: SEQUENCE accounting_transaction_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.accounting_transaction_seq TO banker;


--
-- TOC entry 4511 (class 0 OID 0)
-- Dependencies: 205
-- Name: TABLE balance; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.balance TO banker;


--
-- TOC entry 4512 (class 0 OID 0)
-- Dependencies: 206
-- Name: SEQUENCE balance_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.balance_seq TO banker;


--
-- TOC entry 4513 (class 0 OID 0)
-- Dependencies: 207
-- Name: TABLE bn_user; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.bn_user TO banker;


--
-- TOC entry 4514 (class 0 OID 0)
-- Dependencies: 211
-- Name: TABLE deposit_request; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.deposit_request TO banker;


--
-- TOC entry 4515 (class 0 OID 0)
-- Dependencies: 212
-- Name: SEQUENCE deposit_request_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.deposit_request_seq TO banker;


--
-- TOC entry 4516 (class 0 OID 0)
-- Dependencies: 243
-- Name: TABLE incentive; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.incentive TO banker;


--
-- TOC entry 4517 (class 0 OID 0)
-- Dependencies: 244
-- Name: SEQUENCE incentive_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.incentive_seq TO banker;


--
-- TOC entry 4518 (class 0 OID 0)
-- Dependencies: 208
-- Name: TABLE ledger; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.ledger TO banker;


--
-- TOC entry 4519 (class 0 OID 0)
-- Dependencies: 209
-- Name: SEQUENCE ledger_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.ledger_seq TO banker;


--
-- TOC entry 4520 (class 0 OID 0)
-- Dependencies: 245
-- Name: TABLE reward; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.reward TO banker;


--
-- TOC entry 4521 (class 0 OID 0)
-- Dependencies: 246
-- Name: SEQUENCE reward_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.reward_seq TO banker;


--
-- TOC entry 4522 (class 0 OID 0)
-- Dependencies: 210
-- Name: SEQUENCE user_id_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.user_id_seq TO banker;


--
-- TOC entry 3240 (class 826 OID 101652)
-- Name: DEFAULT PRIVILEGES FOR SEQUENCES; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public REVOKE ALL ON SEQUENCES  FROM postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON SEQUENCES  TO banker;


--
-- TOC entry 3239 (class 826 OID 101651)
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public REVOKE ALL ON TABLES  FROM postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT SELECT,INSERT,DELETE,UPDATE ON TABLES  TO banker;


-- Completed on 2022-06-23 09:13:47

--
-- PostgreSQL database dump complete
--


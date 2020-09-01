--
-- PostgreSQL database dump
--

-- Dumped from database version 10.10
-- Dumped by pg_dump version 10.10

-- Started on 2020-09-01 14:47:30

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
-- TOC entry 196 (class 1259 OID 101569)
-- Name: account; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.account (
    id bigint NOT NULL,
    account_type character varying(1) NOT NULL,
    closed_time timestamp without time zone,
    created_time timestamp without time zone NOT NULL,
    ncan character varying(32) NOT NULL,
    name character varying(96) NOT NULL,
    actual_balance bigint
);


ALTER TABLE public.account OWNER TO postgres;

--
-- TOC entry 197 (class 1259 OID 101572)
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
-- TOC entry 198 (class 1259 OID 101574)
-- Name: accounting_entry; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.accounting_entry (
    id bigint NOT NULL,
    amount integer NOT NULL,
    entry_type character varying(1) NOT NULL,
    account bigint NOT NULL,
    transaction bigint NOT NULL,
    counterparty character varying(96)
);


ALTER TABLE public.accounting_entry OWNER TO postgres;

--
-- TOC entry 199 (class 1259 OID 101577)
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
-- TOC entry 200 (class 1259 OID 101579)
-- Name: accounting_transaction; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.accounting_transaction (
    id bigint NOT NULL,
    accounting_time timestamp without time zone NOT NULL,
    description character varying(256) NOT NULL,
    transaction_time timestamp without time zone NOT NULL,
    ledger bigint NOT NULL,
    context character varying(32),
    transaction_type character varying(2) NOT NULL
);


ALTER TABLE public.accounting_transaction OWNER TO postgres;

--
-- TOC entry 201 (class 1259 OID 101582)
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
-- TOC entry 202 (class 1259 OID 101584)
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
-- TOC entry 203 (class 1259 OID 101587)
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
-- TOC entry 204 (class 1259 OID 101589)
-- Name: bn_user; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.bn_user (
    id bigint NOT NULL,
    family_name character varying(64),
    given_name character varying(32),
    managed_identity character varying(36) NOT NULL,
    email character varying(64),
    personal_account bigint
);


ALTER TABLE public.bn_user OWNER TO postgres;

--
-- TOC entry 208 (class 1259 OID 209643)
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
    account bigint NOT NULL
);


ALTER TABLE public.deposit_request OWNER TO postgres;

--
-- TOC entry 209 (class 1259 OID 209655)
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
-- TOC entry 205 (class 1259 OID 101592)
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
-- TOC entry 206 (class 1259 OID 101595)
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
-- TOC entry 207 (class 1259 OID 101597)
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
-- TOC entry 2709 (class 2606 OID 101600)
-- Name: account account_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account
    ADD CONSTRAINT account_pkey PRIMARY KEY (id);


--
-- TOC entry 2715 (class 2606 OID 101602)
-- Name: accounting_entry accounting_entry_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounting_entry
    ADD CONSTRAINT accounting_entry_pkey PRIMARY KEY (id);


--
-- TOC entry 2719 (class 2606 OID 101604)
-- Name: accounting_transaction accounting_transaction_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounting_transaction
    ADD CONSTRAINT accounting_transaction_pkey PRIMARY KEY (id);


--
-- TOC entry 2721 (class 2606 OID 101606)
-- Name: balance balance_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.balance
    ADD CONSTRAINT balance_pkey PRIMARY KEY (id);


--
-- TOC entry 2725 (class 2606 OID 101608)
-- Name: bn_user bn_user_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bn_user
    ADD CONSTRAINT bn_user_pkey PRIMARY KEY (id);


--
-- TOC entry 2711 (class 2606 OID 210186)
-- Name: account cs_account_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account
    ADD CONSTRAINT cs_account_unique UNIQUE (ncan);


--
-- TOC entry 2713 (class 2606 OID 208990)
-- Name: account cs_actual_balance_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account
    ADD CONSTRAINT cs_actual_balance_unique UNIQUE (actual_balance);


--
-- TOC entry 2723 (class 2606 OID 101612)
-- Name: balance cs_balance_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.balance
    ADD CONSTRAINT cs_balance_unique UNIQUE (account, ledger);


--
-- TOC entry 2731 (class 2606 OID 101614)
-- Name: ledger cs_ledger_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ledger
    ADD CONSTRAINT cs_ledger_unique UNIQUE (name);


--
-- TOC entry 2727 (class 2606 OID 101616)
-- Name: bn_user cs_managed_identity_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bn_user
    ADD CONSTRAINT cs_managed_identity_unique UNIQUE (managed_identity);


--
-- TOC entry 2729 (class 2606 OID 209637)
-- Name: bn_user cs_personal_account_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bn_user
    ADD CONSTRAINT cs_personal_account_unique UNIQUE (personal_account);


--
-- TOC entry 2717 (class 2606 OID 101618)
-- Name: accounting_entry cs_transaction_account_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounting_entry
    ADD CONSTRAINT cs_transaction_account_unique UNIQUE (account, transaction);


--
-- TOC entry 2735 (class 2606 OID 209649)
-- Name: deposit_request deposit_request_payment_link_id_uc; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.deposit_request
    ADD CONSTRAINT deposit_request_payment_link_id_uc UNIQUE (payment_link_id);


--
-- TOC entry 2737 (class 2606 OID 209647)
-- Name: deposit_request deposit_request_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.deposit_request
    ADD CONSTRAINT deposit_request_pkey PRIMARY KEY (id);


--
-- TOC entry 2733 (class 2606 OID 101620)
-- Name: ledger ledger_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ledger
    ADD CONSTRAINT ledger_pkey PRIMARY KEY (id);


--
-- TOC entry 2738 (class 2606 OID 208991)
-- Name: account account_actual_balance_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account
    ADD CONSTRAINT account_actual_balance_fk FOREIGN KEY (actual_balance) REFERENCES public.balance(id);


--
-- TOC entry 2739 (class 2606 OID 101626)
-- Name: accounting_entry accounting_entry_account_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounting_entry
    ADD CONSTRAINT accounting_entry_account_fk FOREIGN KEY (account) REFERENCES public.account(id);


--
-- TOC entry 2740 (class 2606 OID 101631)
-- Name: accounting_entry accounting_entry_transaction_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounting_entry
    ADD CONSTRAINT accounting_entry_transaction_fk FOREIGN KEY (transaction) REFERENCES public.accounting_transaction(id);


--
-- TOC entry 2741 (class 2606 OID 101636)
-- Name: accounting_transaction accounting_transaction_ledger_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounting_transaction
    ADD CONSTRAINT accounting_transaction_ledger_fk FOREIGN KEY (ledger) REFERENCES public.ledger(id);


--
-- TOC entry 2742 (class 2606 OID 101641)
-- Name: balance balance_account_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.balance
    ADD CONSTRAINT balance_account_fk FOREIGN KEY (account) REFERENCES public.account(id);


--
-- TOC entry 2743 (class 2606 OID 101646)
-- Name: balance balance_ledger_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.balance
    ADD CONSTRAINT balance_ledger_fk FOREIGN KEY (ledger) REFERENCES public.ledger(id);


--
-- TOC entry 2745 (class 2606 OID 209650)
-- Name: deposit_request deposit_request_account_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.deposit_request
    ADD CONSTRAINT deposit_request_account_fk FOREIGN KEY (account) REFERENCES public.account(id);


--
-- TOC entry 2744 (class 2606 OID 209638)
-- Name: bn_user user_personal_account_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bn_user
    ADD CONSTRAINT user_personal_account_fk FOREIGN KEY (personal_account) REFERENCES public.account(id);


--
-- TOC entry 2873 (class 0 OID 0)
-- Dependencies: 196
-- Name: TABLE account; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.account TO banker;


--
-- TOC entry 2874 (class 0 OID 0)
-- Dependencies: 197
-- Name: SEQUENCE account_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.account_seq TO banker;


--
-- TOC entry 2875 (class 0 OID 0)
-- Dependencies: 198
-- Name: TABLE accounting_entry; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.accounting_entry TO banker;


--
-- TOC entry 2876 (class 0 OID 0)
-- Dependencies: 199
-- Name: SEQUENCE accounting_entry_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.accounting_entry_seq TO banker;


--
-- TOC entry 2877 (class 0 OID 0)
-- Dependencies: 200
-- Name: TABLE accounting_transaction; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.accounting_transaction TO banker;


--
-- TOC entry 2878 (class 0 OID 0)
-- Dependencies: 201
-- Name: SEQUENCE accounting_transaction_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.accounting_transaction_seq TO banker;


--
-- TOC entry 2879 (class 0 OID 0)
-- Dependencies: 202
-- Name: TABLE balance; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.balance TO banker;


--
-- TOC entry 2880 (class 0 OID 0)
-- Dependencies: 203
-- Name: SEQUENCE balance_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.balance_seq TO banker;


--
-- TOC entry 2881 (class 0 OID 0)
-- Dependencies: 204
-- Name: TABLE bn_user; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.bn_user TO banker;


--
-- TOC entry 2882 (class 0 OID 0)
-- Dependencies: 208
-- Name: TABLE deposit_request; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.deposit_request TO banker;


--
-- TOC entry 2883 (class 0 OID 0)
-- Dependencies: 209
-- Name: SEQUENCE deposit_request_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.deposit_request_seq TO banker;


--
-- TOC entry 2884 (class 0 OID 0)
-- Dependencies: 205
-- Name: TABLE ledger; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.ledger TO banker;


--
-- TOC entry 2885 (class 0 OID 0)
-- Dependencies: 206
-- Name: SEQUENCE ledger_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.ledger_seq TO banker;


--
-- TOC entry 2886 (class 0 OID 0)
-- Dependencies: 207
-- Name: SEQUENCE user_id_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.user_id_seq TO banker;


--
-- TOC entry 1706 (class 826 OID 101652)
-- Name: DEFAULT PRIVILEGES FOR SEQUENCES; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public REVOKE ALL ON SEQUENCES  FROM postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON SEQUENCES  TO banker;


--
-- TOC entry 1705 (class 826 OID 101651)
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public REVOKE ALL ON TABLES  FROM postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT SELECT,INSERT,DELETE,UPDATE ON TABLES  TO banker;


-- Completed on 2020-09-01 14:47:30

--
-- PostgreSQL database dump complete
--


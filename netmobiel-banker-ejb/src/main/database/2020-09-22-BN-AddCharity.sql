-- BN Add Charity
create extension IF NOT EXISTS postgis;
create extension IF NOT EXISTS postgis_topology;

CREATE TABLE public.charity (
    id bigint NOT NULL,
    description character varying(256),
    picture_url character varying(256),
    goal_amount integer NOT NULL,
    donated_amount integer NOT NULL,
    account bigint NOT NULL,
    label character varying(128) NOT NULL,
    point public.geometry NOT NULL,
    campaign_start_time timestamp without time zone NOT NULL,
    campaign_end_time timestamp without time zone
);

ALTER TABLE public.charity OWNER TO banker;

CREATE SEQUENCE public.charity_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
;
ALTER TABLE public.charity_seq OWNER TO banker;

ALTER TABLE ONLY public.charity
    ADD CONSTRAINT charity_pkey PRIMARY KEY (id)
;

ALTER TABLE ONLY public.charity
    ADD CONSTRAINT charity_account_fk FOREIGN KEY (account) REFERENCES public.account(id),
    ADD CONSTRAINT cs_charity_account_unique UNIQUE (account)
;

CREATE TABLE public.charity_user_role (
	bn_user bigint NOT NULL,
	charity bigint NOT NULL,
    created_time timestamp without time zone NOT NULL,
    modified_time timestamp without time zone NOT NULL,
    role character varying(1) NOT NULL
);

ALTER TABLE ONLY public.charity_user_role
    ADD CONSTRAINT charity_user_role_pkey PRIMARY KEY (bn_user, charity)
;
ALTER TABLE ONLY public.charity_user_role
    ADD CONSTRAINT charity_user_role_user_fk FOREIGN KEY (bn_user) REFERENCES public.bn_user(id),
    ADD CONSTRAINT charity_user_role_charity_fk FOREIGN KEY (charity) REFERENCES public.charity(id)
;

ALTER TABLE public.charity_user_role OWNER TO banker;

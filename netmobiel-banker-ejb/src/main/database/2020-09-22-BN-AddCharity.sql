-- BN Add Charity
create extension IF NOT EXISTS postgis;
create extension IF NOT EXISTS postgis_topology;

CREATE TABLE public.charity (
    id bigint NOT NULL,
    description character varying(256) NOT NULL,
    picture_url character varying(256) NOT NULL,
    goal_amount integer NOT NULL,
    donated_amount integer NOT NULL,
    account bigint NOT NULL,
    label character varying(128),
    point public.geometry
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
    ADD CONSTRAINT charity_account_fk FOREIGN KEY (account) REFERENCES public.account(id)
;

CREATE TABLE public.charity_user_role (
	bn_user bigint NOT NULL,
	charity bigint NOT NULL,
    created_time timestamp without time zone NOT NULL,
    modified_time timestamp without time zone NOT NULL,
    role character varying(1)
);

ALTER TABLE ONLY public.charity_user_role
    ADD CONSTRAINT charity_user_role_pkey PRIMARY KEY (bn_user, charity)
;
ALTER TABLE ONLY public.charity_user_role
    ADD CONSTRAINT charity_user_role_user_fk FOREIGN KEY (bn_user) REFERENCES public.bn_user(id),
    ADD CONSTRAINT charity_user_role_charity_fk FOREIGN KEY (charity) REFERENCES public.charity(id)
;

ALTER TABLE public.charity_user_role OWNER TO banker;

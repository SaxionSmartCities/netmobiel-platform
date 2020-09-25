-- BN Add Donation

CREATE TABLE public.donation (
    id bigint NOT NULL,
    description character varying(256),
    amount integer NOT NULL,
    donation_time timestamp without time zone NOT NULL,
    charity bigint NOT NULL,
    bn_user bigint NOT NULL
);

ALTER TABLE public.donation OWNER TO banker;

CREATE SEQUENCE public.donation_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
;
ALTER TABLE public.donation_seq OWNER TO banker;

ALTER TABLE ONLY public.donation
    ADD CONSTRAINT donation_pkey PRIMARY KEY (id)
;

ALTER TABLE ONLY public.donation
    ADD CONSTRAINT donation_charity_fk FOREIGN KEY (charity) REFERENCES public.charity(id),
    ADD CONSTRAINT donation_user_fk FOREIGN KEY (bn_user) REFERENCES public.bn_user(id)
;


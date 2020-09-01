-- Banker: Add Deposit Request table
CREATE TABLE public.deposit_request(
    id bigint NOT NULL,
    amount_credits integer NOT NULL,
    amount_eurocents integer,
    completed_time timestamp without time zone,
    creation_time timestamp without time zone NOT NULL,
    description character varying(128) COLLATE pg_catalog."default" NOT NULL,
    expiration_time timestamp without time zone NOT NULL,
    merchant_order_id character varying(48) COLLATE pg_catalog."default",
    payment_link_id character varying(48) COLLATE pg_catalog."default",
    status character varying(1) COLLATE pg_catalog."default" NOT NULL,
    account bigint NOT NULL,
    CONSTRAINT deposit_request_pkey PRIMARY KEY (id),
    CONSTRAINT deposit_request_payment_link_id_uc UNIQUE (payment_link_id),
    CONSTRAINT deposit_request_account_fk FOREIGN KEY (account)
        REFERENCES public.account (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

CREATE SEQUENCE public.deposit_request_seq
    INCREMENT 1
    START 50
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

GRANT INSERT, SELECT, UPDATE, DELETE ON TABLE public.deposit_request TO banker;
GRANT ALL ON SEQUENCE public.deposit_request_seq TO banker;
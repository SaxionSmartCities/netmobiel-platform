-- Banker: Withdrawal and PaymentBatch
CREATE TABLE public.payment_batch
(
    id bigint NOT NULL,
    creation_time timestamp without time zone NOT NULL,
    created_by bigint NOT NULL,
    settlement_time timestamp without time zone,
    settled_by bigint,
    order_reference character varying(32) COLLATE pg_catalog."default",
    CONSTRAINT payment_batch_pkey PRIMARY KEY (id),
    CONSTRAINT payment_batch_settled_by_fk FOREIGN KEY (settled_by)
        REFERENCES public.bn_user (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT payment_batch_created_by_fk FOREIGN KEY (created_by)
        REFERENCES public.bn_user (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
;

ALTER TABLE public.payment_batch
    OWNER to banker
;
CREATE SEQUENCE public.payment_batch_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
;
ALTER SEQUENCE public.payment_batch_seq
    OWNER TO banker
;

CREATE TABLE public.withdrawal_request
(
    id bigint NOT NULL,
    amount_credits integer NOT NULL,
    amount_eurocents integer NOT NULL,
    creation_time timestamp without time zone NOT NULL,
    created_by bigint NOT NULL,
    settlement_time timestamp without time zone,
    settled_by bigint,
    description character varying(128) COLLATE pg_catalog."default" NOT NULL,
    order_reference character varying(32) COLLATE pg_catalog."default",
    status character varying(1) COLLATE pg_catalog."default" NOT NULL,
    account bigint NOT NULL,
    payment_batch bigint,
    transaction bigint NOT NULL,
    CONSTRAINT withdrawal_request_pkey PRIMARY KEY (id),
    CONSTRAINT withdrawal_account_fk FOREIGN KEY (account)
        REFERENCES public.account (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT withdrawal_settled_by_fk FOREIGN KEY (settled_by)
        REFERENCES public.bn_user (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT withdrawal_payment_batch_fk FOREIGN KEY (payment_batch)
        REFERENCES public.payment_batch (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT withdrawal_created_by_fk FOREIGN KEY (created_by)
        REFERENCES public.bn_user (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
    CONSTRAINT withdrawal_transaction_fk FOREIGN KEY (transaction)
        REFERENCES public.accounting_transaction (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
;

ALTER TABLE public.withdrawal_request
    OWNER to banker
;
CREATE SEQUENCE public.withdrawal_request_seq
    INCREMENT 1
    START 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
;

ALTER SEQUENCE public.withdrawal_request_seq
    OWNER TO banker
;
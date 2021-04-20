-- The delegation table
CREATE TABLE public.delegation
(
    id bigint NOT NULL,
    activation_time timestamp without time zone,
    revocation_time timestamp without time zone,
    submission_time timestamp without time zone NOT NULL,
    transfer_code character varying(32) COLLATE pg_catalog."default",
    delegate bigint NOT NULL,
    delegator bigint NOT NULL,
    CONSTRAINT delegation_pkey PRIMARY KEY (id),
    CONSTRAINT delegation_delegate_fk FOREIGN KEY (delegate)
        REFERENCES public.profile (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT delegation_delegator_fk FOREIGN KEY (delegator)
        REFERENCES public.profile (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE SEQUENCE public.delegation_id_seq
    INCREMENT 1
    START 50
    MINVALUE 1
    NO MAXVALUE
    CACHE 1
;

GRANT INSERT, SELECT, UPDATE, DELETE ON TABLE public.delegation TO profilesvc;
GRANT ALL ON SEQUENCE public.delegation_id_seq TO profilesvc;
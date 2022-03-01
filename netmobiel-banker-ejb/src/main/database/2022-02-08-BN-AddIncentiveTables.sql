-- Banker - Add the incentive tables

-- Incentive - the definition of an incentive
CREATE TABLE incentive (
    id bigint NOT NULL,
    code character varying(16) NOT NULL,
    category character varying(16) NOT NULL,
    description character varying(256) NOT NULL,
    amount integer NOT NULL
);


CREATE SEQUENCE public.incentive_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
;
GRANT ALL ON SEQUENCE public.incentive_seq TO banker;

ALTER TABLE ONLY public.incentive
    ADD CONSTRAINT incentive_pkey PRIMARY KEY (id)
;

-- The name of the incentive must be unique
ALTER TABLE ONLY public.incentive
    ADD CONSTRAINT cs_incentive_code_unique UNIQUE (code)
;

GRANT DELETE, INSERT, SELECT, UPDATE ON TABLE public.incentive TO banker;

-- Reward - the realization of an incentive for a specific user 
CREATE TABLE reward (
    id bigint NOT NULL,
    amount integer NOT NULL,
    reward_time timestamp without time zone NOT NULL,
    cancel_time timestamp without time zone,
    transaction integer,
    recipient integer NOT NULL,
    incentive integer NOT NULL,
    fact_context character varying(32)
);

CREATE SEQUENCE public.reward_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
;
GRANT ALL ON SEQUENCE public.reward_seq TO banker;

ALTER TABLE ONLY public.reward
    ADD CONSTRAINT reward_pkey PRIMARY KEY (id)
;
ALTER TABLE ONLY public.reward
    ADD CONSTRAINT reward_transaction_fk FOREIGN KEY (transaction) REFERENCES public.accounting_transaction(id),
    ADD CONSTRAINT reward_recipient_fk FOREIGN KEY (recipient) REFERENCES public.bn_user(id),
    ADD CONSTRAINT reward_incentive_fk FOREIGN KEY (incentive) REFERENCES public.incentive(id)
;

-- A specific reward can be issued only once to a user (up to now anyway). 
ALTER TABLE ONLY public.reward
    ADD CONSTRAINT cs_reward_unique UNIQUE (recipient, incentive)
;

GRANT DELETE, INSERT, SELECT, UPDATE ON TABLE public.reward TO banker;

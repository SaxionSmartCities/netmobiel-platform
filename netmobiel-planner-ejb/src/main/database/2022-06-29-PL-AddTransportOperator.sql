-- Planner - add transport operator registration
create sequence transport_operator_id_seq start 50 increment 1;
ALTER SEQUENCE public.transport_operator_id_seq
    OWNER TO planner;

CREATE TABLE public.transport_operator
(
    id bigint NOT NULL,
    enabled boolean NOT NULL,
    display_name character varying(32) NOT NULL COLLATE pg_catalog."default",
    description character varying(256) COLLATE pg_catalog."default",
    base_url character varying(256) NOT NULL COLLATE pg_catalog."default",
    CONSTRAINT transport_operator_pkey PRIMARY KEY (id)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.transport_operator
    OWNER to planner
;

INSERT INTO public.transport_operator (id, enabled, display_name, description, base_url) 
	VALUES (nextval('transport_operator_id_seq'), TRUE, 'Netmobiel Rideshare', 'Efficiënt benutten van de auto met ritdelen', 'http://localhost:8080/rideshare-to/api')


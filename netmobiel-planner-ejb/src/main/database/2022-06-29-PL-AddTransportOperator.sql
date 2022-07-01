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

-- After some implementation...

ALTER TABLE public.transport_operator
	RENAME COLUMN display_name TO agency_name
;

ALTER TABLE public.transport_operator
	ADD COLUMN agency_id character varying(32), 
	ADD COLUMN agency_zone_id character varying(32)
;

UPDATE public.transport_operator SET agency_zone_id = 'Europe/Amsterdam', agency_id = 'TO:NRS', agency_name = 'Netmobiel Rideshare Service'
	WHERE agency_name = 'Netmobiel Rideshare'
;

ALTER TABLE public.transport_operator
	ALTER COLUMN agency_id SET NOT NULL, 
	ALTER COLUMN agency_name SET NOT NULL,
	ALTER COLUMN agency_zone_id SET NOT NULL
;

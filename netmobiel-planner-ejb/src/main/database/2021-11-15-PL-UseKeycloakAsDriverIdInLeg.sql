-- Planner - Increase length of driver id to fit in Keycloak user ID
ALTER TABLE public.leg 
	ALTER COLUMN driver_id type character varying(64)
;


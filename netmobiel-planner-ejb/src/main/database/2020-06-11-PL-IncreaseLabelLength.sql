-- Planner 
ALTER TABLE public.trip 
	ALTER COLUMN from_label type character varying(256),
	ALTER COLUMN to_label type character varying(256)
;
ALTER TABLE public.stop 
	ALTER COLUMN label type character varying(256)
;

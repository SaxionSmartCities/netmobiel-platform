-- Rideshare 
ALTER TABLE public.ride_template 
	ALTER COLUMN from_label type character varying(256),
	ALTER COLUMN to_label type character varying(256)
;
ALTER TABLE public.stop 
	ALTER COLUMN label type character varying(256)
;
ALTER TABLE public.booking 
	ALTER COLUMN from_label type character varying(256),
	ALTER COLUMN to_label type character varying(256)
;
ALTER TABLE public.ride 
	ALTER COLUMN from_label type character varying(256),
	ALTER COLUMN to_label type character varying(256)
;

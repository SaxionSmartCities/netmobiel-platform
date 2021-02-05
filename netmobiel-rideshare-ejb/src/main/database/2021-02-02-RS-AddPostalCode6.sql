-- Rideshare: Add postal code 6 fields for departure and arrival locations
-- These fields are used for reporting. 
ALTER TABLE public.ride 
	ADD COLUMN departure_postal_code character varying(6),
	ADD COLUMN arrival_postal_code character varying(6)
;
ALTER TABLE public.ride_template 
	ADD COLUMN departure_postal_code character varying(6),
	ADD COLUMN arrival_postal_code character varying(6)
;

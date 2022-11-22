-- Rideshare: Add confirmation reason code 
ALTER TABLE public.ride 
	ADD COLUMN conf_reason character varying(3)
;


-- Rideshare: Add the expiration time of the validation state
ALTER TABLE public.ride
	ADD COLUMN validation_exp_time timestamp without time zone,
	ADD COLUMN validation_rem_time timestamp without time zone
;

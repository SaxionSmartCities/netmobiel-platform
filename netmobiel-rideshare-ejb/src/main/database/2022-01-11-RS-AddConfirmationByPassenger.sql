-- Rideshare: Add replicated confirmation flags to booking
ALTER TABLE public.booking 
	ADD COLUMN confirmed_by_passenger boolean,
	ADD COLUMN conf_reason_by_passenger character varying(3)
;

-- Add place fields

ALTER TABLE public.place
	ADD COLUMN reference character varying(256),
	ADD COLUMN state_code character varying(3)
;

ALTER TABLE public.profile
	ADD COLUMN home_state_code character varying(3)
;

ALTER TABLE public.place
	ADD COLUMN category character varying(32)
;

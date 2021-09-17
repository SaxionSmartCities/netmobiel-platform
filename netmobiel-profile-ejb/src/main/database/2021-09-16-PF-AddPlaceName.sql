-- Profile - Add place name

ALTER TABLE public.place
	ADD COLUMN name character varying(64)
;

UPDATE public.place SET name = label;
UPDATE public.place SET label = NULL;

-- Clear away the home label, it is nonsense. 
UPDATE public.profile SET home_label = NULL;
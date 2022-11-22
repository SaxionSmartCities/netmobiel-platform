-- Profile - Add acting role

ALTER TABLE public.profile
	ADD COLUMN acting_role character varying(2);
;
UPDATE public.profile SET acting_role = 'PG' WHERE user_role = 'PG' OR user_role = 'BT'
UPDATE public.profile SET acting_role = 'DR' WHERE acting_role IS NULL
ALTER TABLE public.profile
	ALTER COLUMN acting_role SET NOT NULL;
;

-- Profile Service: Add a time to live parameter for the activation code. Reference is the activation code sent time.

ALTER TABLE public.delegation
	ADD COLUMN activation_code_ttl integer
;
UPDATE public.delegation SET activation_code_ttl = 120;

ALTER TABLE public.delegation
	ALTER COLUMN activation_code_ttl SET NOT NULL
;


-- Profile Service: Add a time to live parameter for the activation code. Reference is the activation code sent time.

ALTER TABLE public.delegation
	ADD COLUMN activation_code_ttl integer
;


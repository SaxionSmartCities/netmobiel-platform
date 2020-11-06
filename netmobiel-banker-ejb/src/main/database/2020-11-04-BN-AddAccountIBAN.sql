-- Banker - Add IBAN and account holder 
-- The IBAN is optional, only necessary in case of a withdrawal
ALTER TABLE public.account
	ADD COLUMN iban character varying(48),
	ADD COLUMN iban_holder character varying(96)
;


-- Banker - Add IBAN and iban holder to withdrawal request, to fixate the value.  
-- The IBAN is optional, only necessary in case of a withdrawal
ALTER TABLE public.withdrawal_request
	ADD COLUMN iban character varying(48),
	ADD COLUMN iban_holder character varying(96)
;


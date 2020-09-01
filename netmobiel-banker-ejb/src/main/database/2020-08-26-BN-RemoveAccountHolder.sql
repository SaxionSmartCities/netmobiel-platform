-- Banker - Remove account holder 
ALTER TABLE public.account
	DROP COLUMN holder;
DELETE FROM public.account;
ALTER TABLE public.account
	ADD COLUMN name character varying(96) NOT NULL;
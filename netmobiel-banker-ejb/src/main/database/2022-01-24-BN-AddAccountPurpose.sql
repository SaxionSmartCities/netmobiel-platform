-- Banker - premium account of a user
ALTER TABLE public.account
	ADD COLUMN purpose character varying(1)
;

UPDATE public.account SET purpose = 'S' WHERE ncan in ('banking-reserve', 'reservations', 'premiums');
--UPDATE public.account SET purpose = 'P' WHERE id > 86;
UPDATE  public.account SET purpose = 'C' WHERE purpose IS NULL;

ALTER TABLE public.account
	ALTER COLUMN purpose SET NOT NULL
;

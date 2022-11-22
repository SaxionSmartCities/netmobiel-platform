-- Add a name to the charity. Copy it from the account.
ALTER TABLE public.charity
	ADD COLUMN name character varying(96)
;

UPDATE public.charity SET name = (SELECT a.name FROM account a WHERE a.id = charity.account);
ALTER TABLE public.charity
	ALTER COLUMN name SET NOT NULL
;


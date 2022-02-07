-- Banker - premium account of a user
ALTER TABLE public.accounting_entry
	ADD COLUMN purpose character varying(2)
;

UPDATE public.accounting_entry SET purpose = 
(SELECT at.transaction_type FROM public.accounting_transaction at WHERE at.id = accounting_entry.transaction);

ALTER TABLE public.accounting_entry
	ALTER COLUMN purpose SET NOT NULL
;

ALTER TABLE public.accounting_transaction
	DROP COLUMN transaction_type
;


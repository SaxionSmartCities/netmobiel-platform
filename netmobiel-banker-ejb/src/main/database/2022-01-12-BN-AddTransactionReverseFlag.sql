-- Banker - Add transaction rollback flag.  
ALTER TABLE public.accounting_transaction
	ADD COLUMN is_rollback boolean default false
;




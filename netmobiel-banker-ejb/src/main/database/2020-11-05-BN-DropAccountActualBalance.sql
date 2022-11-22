-- Banker - Drop the actual balance, use a query when needed.  
ALTER TABLE public.account
	DROP CONSTRAINT cs_actual_balance_unique,
	DROP CONSTRAINT account_actual_balance_fk,
	DROP COLUMN actual_balance
;

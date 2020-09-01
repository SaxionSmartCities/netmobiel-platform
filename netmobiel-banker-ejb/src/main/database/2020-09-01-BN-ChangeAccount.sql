-- Banker - Change account.reference into account.ncan
ALTER TABLE public.account
	DROP CONSTRAINT cs_account_unique
;
ALTER TABLE public.account
    RENAME COLUMN reference TO ncan
;    
ALTER TABLE public.account
	ADD CONSTRAINT cs_account_unique UNIQUE (ncan)
;

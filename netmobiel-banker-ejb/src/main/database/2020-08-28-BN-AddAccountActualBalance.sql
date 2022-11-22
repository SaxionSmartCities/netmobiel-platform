-- Banker - Add actual balance reference to account. 
ALTER TABLE public.account
	ADD COLUMN actual_balance bigint NULL
;

ALTER TABLE public.account
	ADD CONSTRAINT cs_actual_balance_unique UNIQUE (actual_balance),
    ADD CONSTRAINT account_actual_balance_fk FOREIGN KEY (actual_balance)
        REFERENCES public.balance (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE SET NULL
;

-- There is only one balance per account right now.
UPDATE account SET actual_balance = (SELECT b.id FROM balance b WHERE b.account = account.id);

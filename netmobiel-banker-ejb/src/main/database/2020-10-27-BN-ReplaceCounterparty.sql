-- Banker: Replace counterparty name with counterparty account
-- First update counter party name, see script

ALTER TABLE public.accounting_entry
    RENAME counterparty TO counterparty_name
;

ALTER TABLE public.accounting_entry
    ADD COLUMN counterparty bigint,
    ADD CONSTRAINT accounting_entry_counterparty_fk FOREIGN KEY (counterparty)
        REFERENCES public.account (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
;
ALTER TABLE public.accounting_entry DROP CONSTRAINT cs_transaction_account_unique;

ALTER TABLE public.accounting_entry
    ADD CONSTRAINT cs_transaction_account_unique UNIQUE (account, transaction, counterparty)
;

UPDATE public.accounting_entry SET counterparty = (SELECT a.id FROM account a WHERE a.name = accounting_entry.counterparty_name);

ALTER TABLE public.accounting_entry DROP COLUMN counterparty_name;

ALTER TABLE public.accounting_entry
    ALTER COLUMN counterparty SET NOT NULL
;
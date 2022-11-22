-- Banker: Add a transaction reference to a deposit request
ALTER TABLE public.deposit_request
    ADD COLUMN transaction bigint,
    ADD CONSTRAINT deposit_transaction_fk FOREIGN KEY (transaction)
        REFERENCES public.accounting_transaction (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
;

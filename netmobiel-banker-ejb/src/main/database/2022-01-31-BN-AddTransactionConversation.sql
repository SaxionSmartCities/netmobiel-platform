-- Banker - Add a conversation concept to a transaction sequence by using the first one of the sequence as a head.
ALTER TABLE public.accounting_transaction
	ADD COLUMN head bigint NULL
;

ALTER TABLE public.accounting_transaction
    ADD CONSTRAINT accounting_transaction_head_fk FOREIGN KEY (head)
        REFERENCES public.accounting_transaction (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
;


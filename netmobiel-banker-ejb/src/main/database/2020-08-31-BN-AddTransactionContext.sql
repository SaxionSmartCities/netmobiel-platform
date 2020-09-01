-- Banker - Add transaction reference and transaction type 
ALTER TABLE public.accounting_transaction
	ADD COLUMN context character varying(32),
	ADD COLUMN transaction_type character varying(2) NOT NULL
;

ALTER TABLE public.accounting_entry
	ADD COLUMN counterparty character varying(96)
;

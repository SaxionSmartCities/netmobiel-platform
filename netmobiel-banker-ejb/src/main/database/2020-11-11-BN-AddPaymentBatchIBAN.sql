-- Banker - Add IBAN and iban holder to payment batch, to fixate the otriginator value.  
ALTER TABLE public.payment_batch
	ADD COLUMN originator_iban character varying(48),
	ADD COLUMN originator_iban_holder character varying(96),
	ADD COLUMN originator_account bigint null
;

ALTER TABLE public.payment_batch
    ADD CONSTRAINT payment_batch_originator_account_fk FOREIGN KEY (originator_account)
        REFERENCES public.account (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
;

UPDATE public.account SET iban = 'NL57ASNB0123456789', iban_holder = 'Stichting Netmobiel' WHERE ncan = 'banking-reserve';
UPDATE public.payment_batch SET (originator_account, originator_iban, originator_iban_holder) = 
	(SELECT id, iban, iban_holder from account WHERE ncan = 'banking-reserve')
;

ALTER TABLE public.payment_batch
	ALTER COLUMN originator_iban SET NOT NULL,
	ALTER COLUMN originator_iban_holder SET NOT NULL,
	ALTER COLUMN originator_account SET NOT NULL
;
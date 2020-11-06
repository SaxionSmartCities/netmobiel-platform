-- Banker - Change payment batch and withdrawal request: More generic field names to cover multiple outcome states  
ALTER TABLE public.withdrawal_request
	ADD COLUMN reason character varying(256)
;
ALTER TABLE public.payment_batch
	ADD COLUMN status character varying(1)
;
ALTER TABLE public.payment_batch
	ADD COLUMN reason character varying(256)
;
UPDATE public.payment_batch SET status = 'C' WHERE settlement_time is not null;
UPDATE public.payment_batch SET status = 'A' WHERE settlement_time is null;
ALTER TABLE public.payment_batch
	ALTER COLUMN status SET NOT NULL
;
ALTER TABLE public.payment_batch RENAME COLUMN settlement_time TO modification_time;
ALTER TABLE public.payment_batch RENAME COLUMN settled_by TO modified_by;

ALTER TABLE public.withdrawal_request RENAME COLUMN settlement_time TO modification_time;
ALTER TABLE public.withdrawal_request RENAME COLUMN settled_by TO modified_by;


UPDATE public.payment_batch SET modification_time = creation_time WHERE modification_time is null;
UPDATE public.payment_batch SET modified_by = created_by WHERE modified_by is null;
UPDATE public.withdrawal_request SET modification_time = creation_time WHERE modification_time is null;
UPDATE public.withdrawal_request SET modified_by = created_by WHERE modified_by is null;
ALTER TABLE public.payment_batch
	ALTER COLUMN modification_time SET NOT NULL,
	ALTER COLUMN modified_by SET NOT NULL
;
ALTER TABLE public.withdrawal_request
	ALTER COLUMN modification_time SET NOT NULL,
	ALTER COLUMN modified_by SET NOT NULL
;

ALTER TABLE public.payment_batch 
	DROP CONSTRAINT payment_batch_settled_by_fk,
	ADD CONSTRAINT payment_batch_modified_by_fk FOREIGN KEY (modified_by)
        REFERENCES public.bn_user (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
;
ALTER TABLE public.withdrawal_request 
	DROP CONSTRAINT withdrawal_request_settled_by_fk,
	ADD CONSTRAINT withdrawal_request_modified_by_fk FOREIGN KEY (modified_by)
        REFERENCES public.bn_user (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
;
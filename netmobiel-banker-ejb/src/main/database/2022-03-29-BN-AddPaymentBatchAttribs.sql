-- Banker - Charity attributes 

ALTER TABLE public.withdrawal_request
    ADD COLUMN version integer NOT NULL DEFAULT 0
;
ALTER TABLE public.payment_batch
    ADD COLUMN version integer NOT NULL DEFAULT 0,
    ADD COLUMN nr_requests integer NOT NULL DEFAULT 0,
    ADD COLUMN amount_requested_eurocents integer NOT NULL DEFAULT 0,
    ADD COLUMN amount_settled_eurocents integer NOT NULL DEFAULT 0
;

ALTER TABLE public.withdrawal_request
	ALTER COLUMN iban SET NOT NULL,
	ALTER COLUMN iban_holder SET NOT NULL
;	

UPDATE payment_batch 
SET nr_requests = 
	(SELECT COUNT(w) 
	 FROM withdrawal_request w 
	 WHERE w.payment_batch = payment_batch.id)
	
UPDATE payment_batch 
SET amount_requested_eurocents = 
	(SELECT COALESCE(SUM(w.amount_eurocents), 0) 
	 FROM withdrawal_request w 
	 WHERE w.payment_batch = payment_batch.id)

UPDATE payment_batch 
SET amount_settled_eurocents = 
	(SELECT COALESCE(SUM(w.amount_eurocents), 0)
	 FROM withdrawal_request w 
	 WHERE w.payment_batch = payment_batch.id AND w.status = 'C')

-- Banker - Join successive transactions

-- Some preparation
ALTER TABLE public.accounting_entry 
	ADD COLUMN dup_trs bigint NULL,
	ADD COLUMN old_trs bigint NULL
;
-- Select transactions that are less than 1 seconds from each other, those should be joined.
-- Keep only the most recent of the two, i.e. the last one of each set of 2. 
-- Copy th eoriginal transaction if, just to be sure
UPDATE public.accounting_entry SET dup_trs = transaction + 1, old_trs = transaction WHERE transaction IN
(select t.id from accounting_transaction t where 
exists (select tt.id from accounting_transaction tt WHERE tt.context = t.context AND tt.id > t.id AND
		abs(EXTRACT(EPOCH FROM (t.transaction_time - tt.transaction_time))) < 1) 
);

-- Transfer entries to new parent transaction
UPDATE public.accounting_entry SET transaction = dup_trs WHERE dup_trs IS NOT NULL;

-- Drop the transactions without entries
DELETE FROM accounting_transaction
WHERE NOT EXISTS (SELECT 1 FROM accounting_entry e where e.transaction = accounting_transaction.id)

-- Add a header transaction to all subsequent transactions of the same context (reference)
-- The following is not entirely correct, but as long there are premium transactions involved it will work.
-- A cancelled transaction will finish a conversation. If there is a next transaction, it will be a new one.   
UPDATE accounting_transaction 
SET head = (select tt.id from accounting_transaction tt WHERE tt.context = accounting_transaction.context order by tt.id limit 1)
WHERE id <> (select tt.id from accounting_transaction tt WHERE tt.context = accounting_transaction.context order by tt.id limit 1)

-- CLeanup
ALTER TABLE public.accounting_entry 
	DROP COLUMN dup_trs,
	DROP COLUMN old_trs
;


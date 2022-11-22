-- Banker - If a transaction is a rollback a payment is by definition a refund

UPDATE accounting_entry SET purpose = 'RF' WHERE purpose = 'PY' AND transaction IN 
(SELECT id FROM accounting_transaction WHERE is_rollback = true)
;

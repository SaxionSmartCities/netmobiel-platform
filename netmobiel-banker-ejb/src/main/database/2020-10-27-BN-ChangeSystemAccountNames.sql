-- Banker: Update names of system accounts
UPDATE public.account SET name = 'De NetMobiel Kluis' WHERE ncan = 'banking-reserve';
UPDATE public.account SET name = 'NetMobiel Reserveringen' WHERE ncan = 'reservations';
UPDATE public.accounting_entry SET counterparty = 'NetMobiel Reserveringen' WHERE counterparty = 'Reservations';
UPDATE public.accounting_entry SET counterparty = 'De NetMobiel Kluis' WHERE counterparty = 'Banking Reserve';

UPDATE public.accounting_entry SET counterparty = 'De NetMobiel Kluis' 
FROM public.accounting_transaction t 
WHERE t.id = accounting_entry.transaction AND t.transaction_type = 'DP' AND accounting_entry.counterparty is null;

SELECT ae.*, a.name, t.transaction_type FROM accounting_entry ae 
JOIN accounting_transaction t ON t.id = ae.transaction
JOIN account a ON a.id = ae.account
WHERE t.transaction_type = 'RS' AND ae.entry_type = 'D' ORDER BY t.id, ae.id

UPDATE accounting_entry SET counterparty = 'NetMobiel Reserveringen' 
FROM accounting_transaction t 
WHERE t.id = accounting_entry.transaction AND t.transaction_type = 'RL' AND accounting_entry.entry_type = 'C'

UPDATE accounting_entry SET counterparty = 'NetMobiel Reserveringen' 
FROM accounting_transaction t 
WHERE t.id = accounting_entry.transaction AND t.transaction_type = 'RS' AND accounting_entry.entry_type = 'D'



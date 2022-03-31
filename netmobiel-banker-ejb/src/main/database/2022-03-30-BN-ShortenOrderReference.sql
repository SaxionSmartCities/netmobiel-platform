-- Banker - make the order reference shorter. 
-- Manually take care to have 5 digits before the dash

UPDATE payment_batch SET order_reference = replace(order_reference, 'NMPB-', 'P')
UPDATE withdrawal_request SET order_reference = replace(order_reference, 'NMWR-', 'W')
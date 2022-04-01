-- Banker: Wipe dynamic data, reset balance, remove iban data from accounts

-- CHARITY
-- public.charity_user_role;
-- public.charity;

TRUNCATE public.donation;

-- INCENTIVE
-- public.incentive;
TRUNCATE public.reward;

-- DEPOSIT & WTHDRAWAL
TRUNCATE public.withdrawal_request;
TRUNCATE public.deposit_request;
TRUNCATE public.payment_batch;

-- BOOKKEEPING
-- public.account
-- Wipe IBAN
UPDATE public.account SET iban = null, iban_holder = null;

TRUNCATE public.accounting_entry;
TRUNCATE public.accounting_transaction;
-- public.balance;
-- Clear end_amount
UPDATE public.balance SET start_amount = 0, end_amount = 0;
-- public.ledger;
-- Change name
-- Change start period to change of year UTC
UPDATE public.ledger SET name = '2022', start_period = '2022-01-01 01:00:00'

-- USER
-- public.bn_user

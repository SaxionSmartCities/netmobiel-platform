-- Banker: Wipe dynamic data, reset balance, remove iban data from accounts

TRUNCATE public.donation, public.reward, public.withdrawal_request, public.deposit_request, public.payment_batch,
	public.accounting_entry, public.accounting_transaction;

-- public.charity_user_role;
-- public.charity;
-- public.incentive;

	-- public.account
-- Wipe IBAN
UPDATE public.account SET iban = null, iban_holder = null;

-- public.balance;
-- Clear end_amount
UPDATE public.balance SET start_amount = 0, end_amount = 0;
-- public.ledger;
-- Change name
-- Change start period to change of year UTC
UPDATE public.ledger SET name = '2022', start_period = '2022-01-01 01:00:00'

-- USER
-- public.bn_user

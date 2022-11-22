-- Find the latest transaction on each context, to check for orphan reservations

select distinct t.id, t.transaction_time, t.context, t.transaction_type from accounting_transaction t 
where (t.context, t.transaction_time) in 
	(select tt.context, max(tt.transaction_time) from accounting_transaction tt group by tt.context) 
order by t.transaction_type asc

-- Select all entries of all transactions
select distinct e.*, t.transaction_time, t.is_rollback from accounting_transaction t join accounting_entry e on e.transaction = t.id
order by e.id asc 

-- Select transactions that should have been joined in a single transaction
select e.*, t.transaction_time, t.is_rollback from accounting_transaction t join accounting_entry e on e.transaction = t.id where 
exists (select tt.id from accounting_transaction tt WHERE tt.context = t.context AND tt.id <> t.id AND
		abs(EXTRACT(EPOCH FROM (t.transaction_time - tt.transaction_time))) < 1) 
order by e.id asc

-- Balance Sanity check
-- the following two should report the same value
SELECT sum(end_amount) FROM public.balance b join account a on a.id = b.account where a.account_type= 'L'
SELECT sum(end_amount) FROM public.balance b join account a on a.id = b.account where a.account_type= 'A'
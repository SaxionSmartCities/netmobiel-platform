-- Find the latest transaction on each context, to check for orphan reservations

select distinct t.id, t.transaction_time, t.context, t.transaction_type from accounting_transaction t 
where (t.context, t.transaction_time) in 
	(select tt.context, max(tt.transaction_time) from accounting_transaction tt group by tt.context) 
order by t.transaction_type asc

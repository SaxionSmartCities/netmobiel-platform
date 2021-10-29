-- Report queries implemented as native queries
 
-- # of messages
select u.managed_identity, date_part('year', m.created_time) as year, 
	date_part('month', m.created_time) as month, count(*)
from message m 
join envelope e on m.id = e.message
join cm_user u on u.id = e.recipient
where m.delivery_mode = 'AL' or m.delivery_mode = 'MS'
--where m.delivery_mode = 'AL' or m.delivery_mode = 'NT'
group by u.managed_identity, year, month
order by u.managed_identity, year, month

-- # of notifications
select u.managed_identity, date_part('year', e.push_time) as year, 
	date_part('month', e.push_time) as month, count(*)
from envelope e
join cm_user u on u.id = e.recipient
where e.push_time is not null
group by u.managed_identity, year, month
order by u.managed_identity, year, month

-- # of read messages
select u.family_name, date_part('year', e.ack_time) as year, 
	date_part('month', e.ack_time) as month, count(*)
from message m 
join envelope e on m.id = e.message
join cm_user u on u.id = e.recipient
where (m.delivery_mode = 'AL' or m.delivery_mode = 'MS') and e.ack_time is not null
--where m.delivery_mode = 'AL' or m.delivery_mode = 'NT'
group by u.family_name, year, month
order by u.family_name, year, month

-- # of read notifications
select u.family_name, date_part('year', e.ack_time) as year, 
	date_part('month', e.ack_time) as month, count(*)
from message m 
join envelope e on m.id = e.message
join cm_user u on u.id = e.recipient
where (m.delivery_mode = 'AL' or m.delivery_mode = 'NT') and e.ack_time is not null
group by u.family_name, year, month
order by u.family_name, year, month

-- # of pushed notifications
select u.family_name, date_part('year', e.push_time) as year, 
	date_part('month', e.push_time) as month, count(*)
from message m 
join envelope e on m.id = e.message
join cm_user u on u.id = e.recipient
where (m.delivery_mode = 'AL' or m.delivery_mode = 'NT') and e.push_time is not null
group by u.family_name, year, month
order by u.family_name, year, month


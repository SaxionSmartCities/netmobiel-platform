-- Report queries for the Planner

-- RGP-1 Count the number of trips
select u.managed_identity as managed_identity, 
date_part('year', it.departure_time) as year, 
date_part('month', it.departure_time) as month, 
count(*) as count 
from trip t
join pl_user u on u.id = t.traveller 
join itinerary it on it.id = t.itinerary 
where it.departure_time >= '2020-01-01' and it.departure_time < '2021-01-01' 
group by u.managed_identity, year, month 
order by u.managed_identity, year, month

-- RGP-2 Count the number of cancelled trips
select u.managed_identity as managed_identity, 
date_part('year', it.departure_time) as year, 
date_part('month', it.departure_time) as month, 
count(*) as count 
from trip t
join pl_user u on u.id = t.traveller 
join itinerary it on it.id = t.itinerary 
where it.departure_time >= '2020-01-01' and it.departure_time < '2021-01-01' and t.state = 'CNC'
group by u.managed_identity, year, month 
order by u.managed_identity, year, month

-- RGP-5 Count the number of rideshare trips with successfull payment
select u.managed_identity as managed_identity, 
date_part('year', it.departure_time) as year, 
date_part('month', it.departure_time) as month, 
count(*) as count 
from trip t
join pl_user u on u.id = t.traveller 
join itinerary it on it.id = t.itinerary 
where it.departure_time >= '2020-01-01' and it.departure_time < '2021-01-01' and 
  exists (select 1 from leg lg where lg.itinerary = it.id and lg.traverse_mode = 'RS' and lg.payment_state = 'P')
group by u.managed_identity, year, month 
order by u.managed_identity, year, month

-- RGP-6 Count the number of rideshare trips with cancelled payment
select u.managed_identity as managed_identity, 
date_part('year', it.departure_time) as year, 
date_part('month', it.departure_time) as month, 
count(*) as count 
from trip t
join pl_user u on u.id = t.traveller 
join itinerary it on it.id = t.itinerary 
where it.departure_time >= '2020-01-01' and it.departure_time < '2021-01-01' and 
  exists (select 1 from leg lg where lg.itinerary = it.id and lg.traverse_mode = 'RS' and lg.payment_state = 'C')
group by u.managed_identity, year, month 
order by u.managed_identity, year, month

-- RGP-7 Count the number of completed mono-modal trips.
select u.managed_identity as managed_identity, 
date_part('year', it.departure_time) as year, 
date_part('month', it.departure_time) as month, 
count(*) as count 
from trip t
join pl_user u on u.id = t.traveller 
join itinerary it on it.id = t.itinerary 
where it.departure_time >= '2020-01-01' and it.departure_time < '2021-01-01' and t.state = 'CMP' and
  (select count(distinct lg.traverse_mode) from leg lg where lg.itinerary = it.id and lg.traverse_mode <> 'WK') = 1
group by u.managed_identity, year, month 
order by u.managed_identity, year, month

-- RGP-8 Count the number of completed mono-modal trips for each modality.
select u.managed_identity as managed_identity, 
date_part('year', it.departure_time) as year, 
date_part('month', it.departure_time) as month, 
count(distinct t.id) as tripCount,
lg.traverse_mode as traverse_mode
from trip t
join pl_user u on u.id = t.traveller 
join itinerary it on it.id = t.itinerary 
join leg lg on it.id = lg.itinerary 
where it.departure_time >= '2020-01-01' and it.departure_time < '2021-01-01' and 
  lg.traverse_mode <> 'WK' and t.state = 'CMP' and
  (select count(distinct lg.traverse_mode) from leg lg where lg.itinerary = it.id and lg.traverse_mode <> 'WK') = 1
group by u.managed_identity, year, month, traverse_mode
order by u.managed_identity, year, month, traverse_mode

-- RGP-9 Count the number of multi-modal trips.
select u.managed_identity as managed_identity, 
date_part('year', it.departure_time) as year, 
date_part('month', it.departure_time) as month, 
count(*) as count 
from trip t
join pl_user u on u.id = t.traveller 
join itinerary it on it.id = t.itinerary 
where it.departure_time >= '2020-01-01' and it.departure_time < '2021-01-01' and t.state = 'CMP' and
  (select count(distinct lg.traverse_mode) from leg lg where lg.itinerary = it.id and lg.traverse_mode <> 'WK') > 1
group by u.managed_identity, year, month 
order by u.managed_identity, year, month

-- RGP-10 Count the number of multi-modal trips in which each modality took part of. 
--        In multi-modal trips multiple modality take part, so the sum of each count exceeds to the total number of trips.
select u.managed_identity as managed_identity, 
date_part('year', it.departure_time) as year, 
date_part('month', it.departure_time) as month, 
count(distinct t.id) as tripCount,
lg.traverse_mode as traverse_mode
from trip t
join pl_user u on u.id = t.traveller 
join itinerary it on it.id = t.itinerary 
join leg lg on it.id = lg.itinerary 
where it.departure_time >= '2020-01-01' and it.departure_time < '2021-01-01' and 
  lg.traverse_mode <> 'WK' and t.state = 'CMP' and
  (select count(distinct lg.traverse_mode) from leg lg where lg.itinerary = it.id and lg.traverse_mode <> 'WK') > 1
group by u.managed_identity, year, month, traverse_mode
order by u.managed_identity, year, month, traverse_mode

-- RGP-11 Count the number of shout-outs issued
select u.managed_identity as managed_identity, 
date_part('year', p.creation_time) as year, 
date_part('month', p.creation_time) as month, 
count(*) as count 
from trip_plan p 
join pl_user u on u.id = p.traveller 
where p.creation_time >= '2020-01-01' and p.creation_time < '2021-01-01' and p.plan_type = 'SHO' 
group by u.managed_identity, year, month 
order by u.managed_identity, year, month

-- RGP-12 Count the number of shout-outs with at least one offer
select u.managed_identity as managed_identity, 
date_part('year', p.creation_time) as year, 
date_part('month', p.creation_time) as month, 
count(distinct it.trip_plan) as count 
from trip_plan p 
join pl_user u on u.id = p.traveller 
join itinerary it on it.trip_plan = p.id 
where p.creation_time >= '2020-01-01' and p.creation_time < '2021-01-01' and p.plan_type = 'SHO' 
group by u.managed_identity, year, month 
order by u.managed_identity, year, month

-- RGP-13 Count the number of shout-outs with an accepted offer (leading to a trip)
select u.managed_identity as managed_identity, 
date_part('year', p.creation_time) as year, 
date_part('month', p.creation_time) as month, 
count(distinct it.trip_plan) as count 
from trip_plan p 
join pl_user u on u.id = p.traveller 
join itinerary it on it.trip_plan = p.id 
join trip t on t.itinerary = it.id 
where p.creation_time >= '2020-01-01' and p.creation_time < '2021-01-01' and p.plan_type = 'SHO' 
group by u.managed_identity, year, month 
order by u.managed_identity, year, month


-- Report queries for the Planner

-- Determine the number of completed trips for each user that includes at least a rideshare leg.
select u.managed_identity, count(distinct t.id) from leg lg join itinerary it on it.id = lg.itinerary join trip t on t.itinerary = it.id 
join pl_user u on u.id = t.traveller
where lg.traverse_mode = 'RS' and (lg.confirmed = true or lg.confirmed_prov = true) and t.state = 'CMP'
group by u.managed_identity order by u.managed_identity

-- Determine the number of completed trips for each user that exclude rideshare and walking legs
select u.managed_identity, count(distinct t.id) from leg lg join itinerary it on it.id = lg.itinerary join trip t on t.itinerary = it.id 
join pl_user u on u.id = t.traveller
where lg.traverse_mode <> 'RS' and lg.traverse_mode <> 'WK' and t.state = 'CMP'
group by u.managed_identity order by u.managed_identity

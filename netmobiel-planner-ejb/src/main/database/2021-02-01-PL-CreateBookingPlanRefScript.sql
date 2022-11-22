-- Planner: Create a script to update the rideshare database booking table to set the passenger_plan_ref field.
-- This is used to track which booking are part of a shout-out.
-- The script creates a script that has to executed in the rideshare database.

copy (select FORMAT('update public.booking set passenger_trip_plan_ref = ''urn:nb:pn:tripplan:%s'' where id = %s;', 
p.id, substring(lg.booking_id from 19)) from trip_plan p
join itinerary it on it.trip_plan = p.id
join leg lg on lg.itinerary = it.id
where p.plan_type = 'SHO' and lg.traverse_mode= 'RS'
order by p.id desc,lg.id desc) to 'e:/temp/tripplan_booking.sql'
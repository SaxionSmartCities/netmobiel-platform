-- Rideshare: Add shout out (trip plan) reference to a booking
-- Especially handy for reporting, previously the passengerTripRef field had a dual use. 
ALTER TABLE public.booking 
	ADD COLUMN passenger_trip_plan_ref character varying(32);
;
alter table public.booking
	alter column passenger_trip_ref drop not null
;
update public.booking set passenger_trip_ref = null 
	where passenger_trip_ref like 'urn:nb:pn:tripplan:%';
-- The trip plan refs have to be created from the planner database with a script, 
-- see /netmobiel-planner-ejb/src/main/database/2021-02-01-PL-CreateBookingPlanRefScript.sql
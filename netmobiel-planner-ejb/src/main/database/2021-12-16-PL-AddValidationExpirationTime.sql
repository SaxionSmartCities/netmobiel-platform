-- Planner: Add the expiration time of the validation state
ALTER TABLE public.trip
	ADD COLUMN validation_rem_time timestamp without time zone,
	ADD COLUMN validation_exp_time timestamp without time zone
;

-- Add expiration date for bookable trips
UPDATE public.trip SET validation_exp_time = itinerary.arrival_time + interval '6 day' 
from public.itinerary
where itinerary.id = trip.itinerary and 
	validation_exp_time is null and
	exists (select 1 from leg lg join itinerary it on lg.itinerary = it.id 
    	    where it.id = trip.itinerary and lg.booking_id is not null)
;

-- Planner - Update trip state for trips in the past
update trip set state = 'CMP' from itinerary 
WHERE trip.itinerary = itinerary.id and itinerary.arrival_time <= current_timestamp and state = 'SCH'
;

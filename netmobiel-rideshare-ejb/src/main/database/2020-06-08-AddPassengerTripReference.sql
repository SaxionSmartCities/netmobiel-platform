-- Rideshare add leg reference of the passenger, an URN to the leg of a trip in the planner. Should be not null. 
ALTER TABLE public.booking ADD COLUMN passenger_trip_ref VARCHAR(32);
    
-- Goto the planner and issue the query
SELECT lg.id, lg.state, lg.traverse_mode, lg.booking_id, s.departure_time
	FROM public.leg lg join stop s on lg.from_stop = s.id 
	where lg.traverse_mode = 'RS' order by lg.booking_id;
	
-- Update in rideshare the passenger_trip_ref. There are only a few.

UPDATE public.booking SET passenger_trip_ref = 'urn:nb:pl:trip:12345' WHERE id = booking_id

-- Once the trip references are set:
ALTER TABLE public.booking ALTER COLUMN passenger_trip_ref SET NOT NULL;

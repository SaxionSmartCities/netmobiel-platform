-- Fix planner urns
UPDATE public.booking SET passenger_trip_ref = replace(passenger_trip_ref, 'urn:nb:pl:', 'urn:nb:pn:')
	WHERE passenger_trip_ref IS NOT NULL;
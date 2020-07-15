-- Remove the itinerary index, it is not useful and it does not allow adding itineraries without touching the plan.
ALTER TABLE public.itinerary
	DROP COLUMN itinerary_ix;
;    
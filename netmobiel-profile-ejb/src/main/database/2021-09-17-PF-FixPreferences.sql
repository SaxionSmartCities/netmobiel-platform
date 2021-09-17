-- Profile - Fix preferences

-- Remove Car (CR) from preferences. We use Rideshare (RS).
DELETE FROM public.preferred_traverse_mode WHERE traverse_mode = 'CR'

-- Remove Wheelchair (WC) and Walker (WL) from the luggage options.
DELETE FROM public.passenger_luggage WHERE luggage = 'WC' OR luggage = 'WL'

ALTER TABLE public.search_preferences
	DROP COLUMN allow_transfers,
	ADD COLUMN max_transfers integer
	ADD COLUMN needs_assistance boolean
;
ALTER TABLE public.search_preferences
	RENAME COLUMN max_transfer_time TO max_walk_distance
;

UPDATE public.search_preferences SET max_walk_distance = 2000 WHERE max_walk_distance < 250;
UPDATE public.search_preferences SET max_walk_distance = 500 WHERE max_walk_distance < 500;


-- Remove Wheelchair (WC) and Walker (WL) from the rideshare luggage options.
DELETE FROM public.rideshare_luggage WHERE luggage = 'WC' OR luggage = 'WL'

ALTER TABLE public.rideshare_preferences
	RENAME COLUMN max_minutes_detour TO max_distance_detour
;
UPDATE public.rideshare_preferences SET max_distance_detour = max_distance_detour * 1000;
UPDATE public.rideshare_preferences SET default_car_ref = 'urn:nb:rs:car:' || default_car_ref WHERE char_length(default_car_ref) < 8;
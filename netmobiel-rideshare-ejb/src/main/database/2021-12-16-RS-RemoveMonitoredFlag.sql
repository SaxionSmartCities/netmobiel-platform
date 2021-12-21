-- Rideshare: Remove the monitor flag
ALTER TABLE public.ride
	DROP COLUMN monitored
;

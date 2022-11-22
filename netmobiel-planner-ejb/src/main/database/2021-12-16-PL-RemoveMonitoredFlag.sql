-- Planner: Remove the monitor flag
ALTER TABLE public.trip
	DROP COLUMN monitored
;

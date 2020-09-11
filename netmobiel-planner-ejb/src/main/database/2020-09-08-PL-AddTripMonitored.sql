-- Planner - add monitored status to trip.
ALTER TABLE public.trip
    ADD COLUMN monitored boolean NOT NULL DEFAULT False;
;    

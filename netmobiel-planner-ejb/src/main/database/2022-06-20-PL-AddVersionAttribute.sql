-- Planner - add version column to trip and trip plan
ALTER TABLE public.trip_plan
    ADD COLUMN version integer NOT NULL DEFAULT 0
;

ALTER TABLE public.trip
    ADD COLUMN version integer NOT NULL DEFAULT 0
;

-- Planner - add plan reference travel attributes for shoutouts
ALTER TABLE public.trip_plan
    ADD COLUMN reference_type character varying(2),
    ADD COLUMN reference_distance integer,
    ADD COLUMN reference_duration integer,
    ADD COLUMN reference_fare_credits integer
;    
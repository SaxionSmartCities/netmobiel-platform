-- Planner - add plan reference travel attributes for shoutouts
ALTER TABLE public.trip_plan
    DROP COLUMN reference_type,
    DROP COLUMN reference_distance,
    DROP COLUMN reference_duration,
    DROP COLUMN reference_fare_credits,
    ADD COLUMN geodesic_distance integer,
    ADD COLUMN reference_itinerary bigint,
    ADD CONSTRAINT trip_plan_reference_itinerary_itinerary_uc UNIQUE (reference_itinerary),
	ADD CONSTRAINT trip_plan_reference_itinerary_itinerary_fk FOREIGN KEY (reference_itinerary)
        REFERENCES public.itinerary (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
;    
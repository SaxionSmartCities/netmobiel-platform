-- Planner: Add delegate feature - Add requestor to the trip plan
ALTER TABLE public.trip_plan
	ADD COLUMN requestor integer,
    ADD CONSTRAINT trip_plan_requestor_fk FOREIGN KEY (requestor)
        REFERENCES public.pl_user (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
;

UPDATE public.trip_plan SET requestor = traveller;

ALTER TABLE public.trip_plan
	ALTER COLUMN requestor SET NOT NULL
;

-- Add organizer to trip
ALTER TABLE public.trip
	ADD COLUMN organizer integer,
    ADD CONSTRAINT trip_organizer_fk FOREIGN KEY (organizer)
        REFERENCES public.pl_user (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
;

UPDATE public.trip SET organizer = traveller;

ALTER TABLE public.trip
	ALTER COLUMN organizer SET NOT NULL
;

-- Set trip creation time
ALTER TABLE public.trip
    ADD COLUMN creation_time timestamp without time zone
;

-- Not entirely correct (for shout-out), but will do
UPDATE public.trip SET creation_time = trip_plan.creation_time 
FROM itinerary JOIN trip_plan ON trip_plan.id = itinerary.trip_plan WHERE itinerary.id = trip.itinerary;

-- Fix very old trip without a plan reference
UPDATE public.trip SET creation_time = itinerary.departure_time 
FROM itinerary WHERE itinerary.id = trip.itinerary AND creation_time IS NULL;

ALTER TABLE public.trip
    ALTER COLUMN creation_time SET NOT NULL
;

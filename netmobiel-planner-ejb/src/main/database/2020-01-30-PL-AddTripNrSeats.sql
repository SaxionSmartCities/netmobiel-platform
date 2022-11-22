-- nr seats required for the trip.
ALTER TABLE public.trip
    ADD COLUMN nr_seats integer NOT NULL DEFAULT 1;
-- Planner - add fare in credits attribute
ALTER TABLE public.leg
    ADD COLUMN fare_credits integer
;    
ALTER TABLE public.itinerary
    ADD COLUMN fare_credits integer
;    
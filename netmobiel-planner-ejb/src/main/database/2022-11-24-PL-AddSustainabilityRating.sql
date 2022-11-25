-- Planner - add sustainability rating of the itinerary
ALTER TABLE public.itinerary
    ADD COLUMN average_co2_emission_rate integer,
    ADD COLUMN sustainability_rating integer
;

-- Emission rate [g / traveller km]. 
-- For Car the emission rate is for vehicle kilometer or for just the driver. 
-- For Rideshare the emission of the car is averaged over the number of seats occupied. 
ALTER TABLE public.leg
    ADD COLUMN co2_emission_rate integer
;

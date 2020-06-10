-- Rideshare add leg reference of the passenger, an URN to the leg of a trip in the planner. Should be not null. 
ALTER TABLE public.booking
    ADD COLUMN passenger_trip_ref VARCHAR(32);
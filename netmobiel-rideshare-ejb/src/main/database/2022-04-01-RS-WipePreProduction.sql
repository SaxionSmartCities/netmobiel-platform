-- Rideshare: Wipe dynamic data

TRUNCATE public.booked_legs, public.booking, public.ride_template, public.leg, public.stop, public.ride;

-- Probably irrelevant, but keep. Has also setting in profile (!)
-- public.car;
--public.rs_user;

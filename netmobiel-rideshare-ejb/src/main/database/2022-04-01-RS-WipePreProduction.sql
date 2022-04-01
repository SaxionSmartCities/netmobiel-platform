-- Rideshare: Wipe dynamic data

TRUNCATE public.booked_legs;
TRUNCATE public.booking;
TRUNCATE public.ride_template;
TRUNCATE public.leg;
TRUNCATE public.stop;
TRUNCATE public.ride;

-- Probably irrelevant, but keep. Has also setting in profile (!)
-- public.car;
--public.rs_user;

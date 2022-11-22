-- Planner: Add a booking confirmed flag
ALTER TABLE public.leg
	ADD COLUMN booking_confirmed boolean
;

UPDATE public.leg SET booking_confirmed = true WHERE booking_id IS NOT NULL;
-- Rideshare: Move the confirm ride flags from ride to booking
ALTER TABLE public.booking 
	ADD COLUMN confirmed boolean,
	ADD COLUMN fare_credits integer,
	ADD COLUMN conf_reason character varying(3),
	ADD COLUMN payment_state character varying(1),
	ADD COLUMN payment_id character varying(32)
;

UPDATE booking SET confirmed = ride.confirmed, conf_reason = ride.conf_reason FROM public.ride
    WHERE booking.ride = ride.id and booking.state = 'CFM'

-- Rideshare: Remove the confirmed flags from ride
ALTER TABLE public.ride
	DROP COLUMN confirmed,
	DROP COLUMN conf_reason
;

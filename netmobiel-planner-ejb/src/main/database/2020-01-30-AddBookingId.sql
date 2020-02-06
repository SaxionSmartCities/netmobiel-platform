-- booking id required for the leg
ALTER TABLE public.leg
    ADD COLUMN booking_id character varying(32);
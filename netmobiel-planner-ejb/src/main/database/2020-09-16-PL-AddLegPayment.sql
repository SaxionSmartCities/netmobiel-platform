-- Planner - add payment attributes
ALTER TABLE public.leg
    ADD COLUMN payment_state character varying(1),
    ADD COLUMN payment_id  character varying(32)
;
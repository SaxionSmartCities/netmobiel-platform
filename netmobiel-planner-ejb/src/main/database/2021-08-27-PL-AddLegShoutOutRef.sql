-- Planner - add a reference to the traveller's shout-out to the resolving leg proposed by a driver
ALTER TABLE public.leg
    ADD COLUMN shout_out_ref character varying(32)
;    
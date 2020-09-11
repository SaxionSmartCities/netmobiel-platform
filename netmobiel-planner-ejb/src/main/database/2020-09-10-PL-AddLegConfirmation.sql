-- Planner - add review flags to leg.
ALTER TABLE public.leg
    ADD COLUMN confirmation_req boolean NOT NULL DEFAULT False,
    ADD COLUMN confirmed boolean,
    ADD COLUMN confirmation_prov_req boolean NOT NULL DEFAULT False,
    ADD COLUMN confirmed_prov boolean
;    

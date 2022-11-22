-- Planner: Make headsign longer
ALTER TABLE public.leg
    ALTER COLUMN headsign TYPE character varying(48)
;

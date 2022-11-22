-- Rideshare: Add version column for optimistic locking 
ALTER TABLE public.ride 
	ADD COLUMN version integer default 0
;
update public.ride set version = 0 where version is null;

ALTER TABLE public.ride
    ALTER COLUMN version SET NOT NULL;
-- Rideshare: Add version column for optimistic locking 
ALTER TABLE public.ride 
	ADD COLUMN version integer
;

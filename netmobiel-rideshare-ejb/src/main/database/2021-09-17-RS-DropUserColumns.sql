-- Rideshare: Removed obsoleted columns 
ALTER TABLE public.rs_user
	DROP COLUMN gender,
	DROP COLUMN year_of_birth
;


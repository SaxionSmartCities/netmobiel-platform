-- Rideshare: A new booking can be created without a passenger (TOMP API). 
-- Once manifested as a real booking, the passenger must be present. 
 
alter table public.booking
	alter column passenger drop not null,
	alter column state set not null
;

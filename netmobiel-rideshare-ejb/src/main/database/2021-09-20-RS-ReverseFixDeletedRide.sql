-- Rideshare: Unset the deleted flag. 
update public.ride set deleted = NULL WHERE deleted = True;

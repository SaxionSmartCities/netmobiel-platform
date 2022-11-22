-- Rideshare: Unset the deleted flag. 
update public.ride set remarks = NULL WHERE remarks = 'What does this do?';
update public.ride_template set remarks = NULL WHERE remarks = 'What does this do?';

-- Rideshare: Set the deleted flag when the ride has been cancelled 
update ride set deleted = True WHERE state = 'CNC';

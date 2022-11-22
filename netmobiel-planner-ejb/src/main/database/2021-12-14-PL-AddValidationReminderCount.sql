-- Planner: Add a reminder count for the validation reminders
-- A count of 1 means that 1 reminder is sent. 
ALTER TABLE public.trip
	ADD COLUMN reminder_count integer NOT NULL DEFAULT 0
;

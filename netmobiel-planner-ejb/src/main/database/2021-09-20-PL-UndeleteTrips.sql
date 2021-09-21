-- Planner - Do not set the delete flag.
UPDATE public.trip SET deleted = NULL WHERE deleted = True;

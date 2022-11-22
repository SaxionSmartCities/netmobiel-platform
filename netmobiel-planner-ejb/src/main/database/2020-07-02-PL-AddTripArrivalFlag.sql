-- email for easy debugging and logging
ALTER TABLE public.trip
    ADD COLUMN arrival_time_is_pinned boolean default false;

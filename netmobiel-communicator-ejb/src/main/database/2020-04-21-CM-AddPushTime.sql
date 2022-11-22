-- Add push time to message envelope
ALTER TABLE public.envelope
    ADD COLUMN push_time timestamp without time zone;
    
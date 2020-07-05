-- email for easy debugging and logging
ALTER TABLE public.pl_user
    ADD COLUMN email character varying(64);    
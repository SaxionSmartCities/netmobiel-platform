-- email for easy debugging and logging
ALTER TABLE public.bn_user
    ADD COLUMN email character varying(64);    
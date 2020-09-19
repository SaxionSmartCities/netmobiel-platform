-- CM email for easy debugging and logging
ALTER TABLE public.cm_user
    ADD COLUMN email character varying(64);    
-- Envelope is for recipient as well as for the sender.
-- Easier to query.

ALTER TABLE public.envelope
    ADD COLUMN sender boolean NOT NULL DEFAULT false,
    ALTER COLUMN context SET NOT NULL
;

-- ALTER TABLE public.message
--    ALTER COLUMN context DROP NOT NULL
--;

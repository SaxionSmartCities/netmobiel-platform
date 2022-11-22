-- Banker - Charity attributes 

ALTER TABLE public.charity
    ADD COLUMN deleted boolean NOT NULL DEFAULT false,
    ADD COLUMN version integer NOT NULL DEFAULT 0
;



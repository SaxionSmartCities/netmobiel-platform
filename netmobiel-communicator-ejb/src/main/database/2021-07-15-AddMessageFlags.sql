ALTER TABLE public.message
    ADD COLUMN to_all_users boolean NOT NULL DEFAULT false,
    ADD COLUMN important boolean NOT NULL DEFAULT false,
    ADD COLUMN archived boolean NOT NULL DEFAULT false
;
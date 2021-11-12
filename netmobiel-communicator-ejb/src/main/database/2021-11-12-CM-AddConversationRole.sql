-- Add a role to the conversation to resolve ambiguity in context.

ALTER TABLE public.conversation
    ADD COLUMN owner_role character varying(2)
;

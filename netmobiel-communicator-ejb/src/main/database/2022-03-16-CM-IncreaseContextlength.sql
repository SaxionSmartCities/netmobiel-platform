-- Communicator: Fix context length to fit keycloak urn
ALTER TABLE public.conversation_context
    ALTER COLUMN context TYPE character varying(64)
;
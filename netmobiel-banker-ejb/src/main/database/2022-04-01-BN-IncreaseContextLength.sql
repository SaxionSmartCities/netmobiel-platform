-- Banker: Fix reward context length to fit keycloak urn
ALTER TABLE public.reward
    ALTER COLUMN fact_context TYPE character varying(64)
;

ALTER TABLE public.accounting_transaction
    ALTER COLUMN context TYPE character varying(64)
;
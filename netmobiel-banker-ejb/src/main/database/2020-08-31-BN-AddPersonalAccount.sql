-- Banker - personal account of a user
ALTER TABLE public.bn_user
	ADD COLUMN personal_account bigint NULL
;

ALTER TABLE public.bn_user
	ADD CONSTRAINT cs_personal_account_unique UNIQUE (personal_account),
    ADD CONSTRAINT user_personal_account_fk FOREIGN KEY (personal_account)
        REFERENCES public.account (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
;

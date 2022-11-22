-- Banker - premium account of a user
ALTER TABLE public.bn_user
	ADD COLUMN premium_account bigint NULL
;

ALTER TABLE public.bn_user
	ADD CONSTRAINT cs_premium_account_unique UNIQUE (premium_account),
    ADD CONSTRAINT user_premium_account_fk FOREIGN KEY (premium_account)
        REFERENCES public.account (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE SET NULL
;

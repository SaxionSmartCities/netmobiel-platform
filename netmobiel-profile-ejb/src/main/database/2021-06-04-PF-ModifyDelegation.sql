-- Rename the code
ALTER TABLE public.delegation
    RENAME transfer_code TO activation_code;
ALTER TABLE public.delegation
    ADD COLUMN activation_code_sent_time timestamp without time zone;
ALTER TABLE public.delegation
	ADD COLUMN sms_id character varying(32);
    
ALTER TABLE public.delegation
    ADD COLUMN transfer_from bigint;

ALTER TABLE public.delegation
    ADD CONSTRAINT delegation_transfer_from_fk FOREIGN KEY (transfer_from)
        REFERENCES public.delegation (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
;    
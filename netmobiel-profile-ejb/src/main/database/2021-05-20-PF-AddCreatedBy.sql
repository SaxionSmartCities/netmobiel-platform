-- Track the creator of the profile
ALTER TABLE public.profile
	-- NULL is allowed, only when a delegate creates a profile the creator is tracked.
	ADD COLUMN created_by bigint,
    ADD COLUMN creation_time timestamp without time zone,
	ADD CONSTRAINT profile_created_by_fk FOREIGN KEY (created_by)
        REFERENCES public.profile (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
;

-- Initialize the creation time of the existing profiles.
UPDATE public.profile SET creation_time = '2021-01-01 00:00:00' WHERE creation_time IS NULL;

ALTER TABLE public.profile
	ALTER COLUMN creation_time SET NOT NULL
;	

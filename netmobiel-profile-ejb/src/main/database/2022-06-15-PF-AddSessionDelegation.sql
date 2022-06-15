-- Profile Service: Add the delegator profile to the session

ALTER TABLE public.page_visit
	ADD COLUMN on_behalf_of integer,
	ADD CONSTRAINT page_visit_on_behalf_of_fk FOREIGN KEY (profile)
        REFERENCES public.profile (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
;

ALTER TABLE public.user_session
	RENAME COLUMN profile TO real_user
;

ALTER TABLE public.user_session
	DROP CONSTRAINT user_session_profile_fk,
	ADD CONSTRAINT user_session_real_user_fk FOREIGN KEY (real_user)
        REFERENCES public.profile (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
;
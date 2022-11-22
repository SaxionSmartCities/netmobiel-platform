-- Profile Service: Add user event, instead of page visit.

ALTER TABLE public.page_visit RENAME TO user_event;
ALTER SEQUENCE public.page_visit_id_seq RENAME TO user_event_id_seq

ALTER TABLE public.user_event
	RENAME COLUMN visit_time TO event_time
;

ALTER TABLE public.user_event
	ADD COLUMN event character varying(2),
	ADD COLUMN arguments character varying(256)
;

UPDATE public.user_event SET event = 'PV' where event is null;

ALTER TABLE public.user_event
	ALTER COLUMN event SET NOT NULL
;

ALTER TABLE public.user_event
	RENAME CONSTRAINT page_visit_pkey TO user_event_pkey
;
ALTER TABLE public.user_event
	RENAME CONSTRAINT page_visit_on_behalf_of_fk TO user_event_on_behalf_of_fk
;
ALTER TABLE public.user_event
	RENAME CONSTRAINT page_visit_user_session_fk TO user_event_user_session_fk
;

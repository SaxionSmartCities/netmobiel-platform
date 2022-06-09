-- Profile Service: Added user session and page visit tables 

CREATE TABLE public.user_session
(
    id bigint NOT NULL,
    profile bigint NOT NULL,
    session_id character varying(48) NOT NULL,
    ip_address character varying(32) NOT NULL,
    user_agent character varying(512) NOT NULL,
    session_start timestamp without time zone NOT NULL,
    session_end timestamp without time zone,
    CONSTRAINT user_session_pkey PRIMARY KEY (id),
    CONSTRAINT uc_session_id UNIQUE (session_id),
    CONSTRAINT user_session_profile_fk FOREIGN KEY (profile)
        REFERENCES public.profile (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);

GRANT DELETE, UPDATE, INSERT, SELECT ON TABLE public.user_session TO profilesvc;

CREATE SEQUENCE public.user_session_id_seq
    INCREMENT 1
    START 50
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1
;

GRANT ALL ON SEQUENCE public.user_session_id_seq TO profilesvc;

CREATE TABLE public.page_visit
(
    id bigint NOT NULL,
    user_session bigint NOT NULL,
    path character varying(128) NOT NULL,
    visit_time timestamp without time zone NOT NULL,
    CONSTRAINT page_visit_pkey PRIMARY KEY (id),
    CONSTRAINT page_visit_user_session_fk FOREIGN KEY (user_session)
        REFERENCES public.user_session (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);

GRANT DELETE, UPDATE, INSERT, SELECT ON TABLE public.page_visit TO profilesvc;

CREATE SEQUENCE public.page_visit_id_seq
    INCREMENT 1
    START 50
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1
;

GRANT ALL ON SEQUENCE public.page_visit_id_seq TO profilesvc;


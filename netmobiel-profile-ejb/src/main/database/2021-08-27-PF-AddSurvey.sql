-- Add the survey tables.

CREATE TABLE public.survey
(
    survey_id character varying(8) COLLATE pg_catalog."default" NOT NULL,
    display_name character varying(64) COLLATE pg_catalog."default" NOT NULL,
    remarks character varying(256) COLLATE pg_catalog."default",
    provider_survey_ref character varying(32) COLLATE pg_catalog."default" NOT NULL,
    take_delay_hours integer,
    take_interval_hours integer,
    start_time timestamp without time zone,
    end_time timestamp without time zone,
    reward_credits integer,
    CONSTRAINT survey_pkey PRIMARY KEY (survey_id),
    CONSTRAINT uc_provider_survey_ref UNIQUE (provider_survey_ref)
);

GRANT DELETE, UPDATE, INSERT, SELECT ON TABLE public.survey TO profilesvc;

CREATE TABLE public.survey_interaction
(
    id bigint NOT NULL,
    invitation_time timestamp without time zone NOT NULL,
    invitation_count integer DEFAULT 0 NOT NULL,
    redirect_time timestamp without time zone,
    redirect_count integer DEFAULT 0 NOT NULL,
    submit_time timestamp without time zone,
    profile bigint NOT NULL,
    survey character varying(255) COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT survey_interaction_pkey PRIMARY KEY (id),
    CONSTRAINT uc_survey_interaction UNIQUE (survey, profile),
    CONSTRAINT survey_interaction_profile_fk FOREIGN KEY (profile)
        REFERENCES public.profile (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT survey_interaction_survey_fk FOREIGN KEY (survey)
        REFERENCES public.survey (survey_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

GRANT DELETE, UPDATE, INSERT, SELECT ON TABLE public.survey_interaction TO profilesvc;

CREATE SEQUENCE public.survey_interaction_id_seq
    INCREMENT 1
    START 50
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1
;

GRANT ALL ON SEQUENCE public.survey_interaction_id_seq TO profilesvc;

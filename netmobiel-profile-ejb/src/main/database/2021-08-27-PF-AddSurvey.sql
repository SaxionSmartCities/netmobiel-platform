-- Add the survey tables.

CREATE TABLE public.survey
(
    survey_id character varying(8) COLLATE pg_catalog."default" NOT NULL,
    delay_hours integer,
    display_name character varying(64) COLLATE pg_catalog."default" NOT NULL,
    end_time timestamp without time zone,
    group_ref character varying(32) COLLATE pg_catalog."default",
    provider_survey_ref character varying(32) COLLATE pg_catalog."default" NOT NULL,
    remarks character varying(256) COLLATE pg_catalog."default",
    sequencenr integer,
    start_time timestamp without time zone,
    survey_provider character varying(16) COLLATE pg_catalog."default" NOT NULL,
    survey_trigger character varying(16) COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT survey_pkey PRIMARY KEY (survey_id)
);

GRANT DELETE, UPDATE, INSERT, SELECT ON TABLE public.survey TO profilesvc;

CREATE TABLE public.survey_response
(
    id bigint NOT NULL,
    request_time timestamp without time zone NOT NULL,
    submit_time timestamp without time zone,
    profile bigint NOT NULL,
    survey character varying(255) COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT survey_response_pkey PRIMARY KEY (id),
    CONSTRAINT uc_survey_response UNIQUE (survey, profile),
    CONSTRAINT survey_response_profile_fk FOREIGN KEY (profile)
        REFERENCES public.profile (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT survey_response_survey_fk FOREIGN KEY (survey)
        REFERENCES public.survey (survey_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

GRANT DELETE, UPDATE, INSERT, SELECT ON TABLE public.survey_response TO profilesvc;

CREATE SEQUENCE public.survey_response_id_seq
    INCREMENT 1
    START 50
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1
;

GRANT ALL ON SEQUENCE public.survey_response_id_seq TO profilesvc;

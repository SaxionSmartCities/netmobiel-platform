-- Profile - Replace the composite primary key with a synthetic key (just as in all other tables)

ALTER TABLE ONLY public.survey_interaction
    ADD COLUMN id bigint
;

CREATE SEQUENCE public.survey_interaction_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
;
GRANT ALL ON SEQUENCE public.survey_interaction_seq TO profilesvc;

UPDATE public.survey_interaction SET id = nextval('survey_interaction_seq') WHERE id IS NULL;

ALTER TABLE ONLY public.survey_interaction
    DROP CONSTRAINT survey_interaction_pkey,
    ALTER COLUMN id SET NOT NULL,
    ADD CONSTRAINT survey_interaction_pkey PRIMARY KEY (id)
;
ALTER TABLE ONLY public.survey_interaction
    ADD CONSTRAINT cs_survey_interaction_unique UNIQUE (profile, survey);


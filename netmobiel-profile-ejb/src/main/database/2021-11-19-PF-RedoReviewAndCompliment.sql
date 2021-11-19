-- Profile - Redo the compliments and reviews

-- Delete all the rubbish
DELETE FROM public.compliment;
DELETE FROM public.review;

-- Compliment
ALTER TABLE public.compliment RENAME TO compliment_set;
ALTER SEQUENCE public.compliment_id_seq RENAME TO compliment_set_id_seq;

ALTER TABLE public.compliment_set
	ADD COLUMN context character varying(32)
;

ALTER TABLE public.compliment_set
	DROP COLUMN compliment;
;

ALTER TABLE public.compliment_set
	ALTER COLUMN context SET NOT NULL,
    ADD CONSTRAINT cs_compliment_set_unique UNIQUE (receiver, context)
;

CREATE TABLE public.compliment (
    compliment_set bigint NOT NULL,
    compliment character varying(2) NOT NULL
);

ALTER TABLE public.compliment
    ADD CONSTRAINT compliment_compliment_set_fk FOREIGN KEY (compliment_set) REFERENCES public.compliment_set(id);
    ADD CONSTRAINT cs_compliment_unique UNIQUE (compliment_set, compliment)
;

-- Review
ALTER TABLE public.review
	ADD COLUMN context character varying(32)
;

ALTER TABLE public.review
	ALTER COLUMN context SET NOT NULL,
    ADD CONSTRAINT cs_review_unique UNIQUE (receiver, context)
;


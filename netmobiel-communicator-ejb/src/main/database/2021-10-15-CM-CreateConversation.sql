-- Message Thread

CREATE TABLE public.conversation(
    id bigint NOT NULL,
    topic character varying(128) NOT NULL, 
    created_time timestamp without time zone,
    archived_time timestamp without time zone,
    owner bigint NOT NULL,
);
ALTER TABLE public.conversation
	ADD CONSTRAINT conversation_pkey PRIMARY KEY (id),
    ADD CONSTRAINT conversation_owner_fk FOREIGN KEY (owner) REFERENCES public.cm_user(id)
;

ALTER TABLE public.conversation OWNER TO communicator;

CREATE SEQUENCE public.conversation_id_seq
    START WITH 50
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE public.conversation_id_seq OWNER TO communicator;

ALTER TABLE public.message
	-- System sender has no conversation to maintain
	ADD COLUMN sender_conversation bigint,
	ADD CONSTRAINT message_conversation_fk FOREIGN KEY (sender_conversation) REFERENCES public.conversation(id),
	ALTER COLUMN sender DROP NOT NULL
;

ALTER TABLE public.envelope
	-- Context if different from sender context
    ADD COLUMN context character varying(32),
	ADD COLUMN conversation bigint,
	ALTER COLUMN recipient DROP NOT NULL,
	ADD CONSTRAINT envelope_conversation_fk FOREIGN KEY (conversation) REFERENCES public.conversation(id)
;

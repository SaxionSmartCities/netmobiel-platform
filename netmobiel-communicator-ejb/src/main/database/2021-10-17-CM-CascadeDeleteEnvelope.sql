-- Make the foreign key constraint to delete envelope when message is deleted.
ALTER TABLE envelope
    DROP CONSTRAINT envelope_message_fk
;
ALTER TABLE envelope
    ADD CONSTRAINT envelope_message_fk FOREIGN KEY (message)
        REFERENCES public.message (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
;

-- Remove the test messages

DELETE FROM message WHERE context like '%:leg:%';

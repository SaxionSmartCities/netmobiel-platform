-- Delete the message to all

DELETE FROM envelope USING message WHERE envelope.message = message.id AND message.to_all_users = true;
DELETE FROM message m WHERE m.to_all_users = true;

ALTER TABLE public.message
    DROP COLUMN to_all_users,
    DROP COLUMN important,
    DROP COLUMN archived
;

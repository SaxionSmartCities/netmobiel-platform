-- Delete the message to all

DELETE FROM envelope e, message m WHERE e.message = m.id AND m.to_all_users = true;
DELETE FROM message m WHERE m.to_all_users = true;

ALTER TABLE public.message
    DROP COLUMN to_all_users,
    DROP COLUMN important,
    DROP COLUMN archived
;
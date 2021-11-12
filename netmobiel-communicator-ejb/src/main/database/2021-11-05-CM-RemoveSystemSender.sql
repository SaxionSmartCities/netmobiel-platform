-- Delete the system sender from the messages
UPDATE public.message SET sender = NULL FROM public.cm_user WHERE message.sender = cm_user.id AND cm_user.managed_identity = 'SYSTEM';

DELETE FROM cm_user WHERE cm_user.managed_identity = 'SYSTEM';

-- Get rid of old messages
DELETE FROM public.message WHERE context like 'urn:nb:pl:leg:%';
-- Get rid of old delegate messages
DELETE FROM public.message WHERE subject like 'Organisator:%';

DELETE FROM public.envelope USING public.message WHERE envelope.message = message.id AND message.sender = envelope.recipient;

UPDATE public.message SET delivery_mode = 'AL'

-- Subject is no longer required
ALTER TABLE public.message
    ALTER COLUMN subject DROP NOT NULL
;
UPDATE public.message SET subject = NULL;
-- Use conversation owner
--ALTER TABLE public.envelope
--	DROP COLUMN recipient
--;

-- Use sender conversation owner
--ALTER TABLE public.message
--	DROP COLUMN sender
--;

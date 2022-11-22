-- Delete all messages and envelopes
-- Review wheter this is really needed
-- DELETE FROM public.message;
-- DELETE FROM public.cm_user WHERE managed_identity = 'SYSTEM';

-- Use conversation owner
--ALTER TABLE public.envelope
--	DROP COLUMN recipient
--;

-- Use sender conversation owner
--ALTER TABLE public.message
--	DROP COLUMN sender
--;

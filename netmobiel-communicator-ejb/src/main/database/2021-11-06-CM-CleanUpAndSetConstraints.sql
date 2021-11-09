-- Clean up problematic messages (messages that could not be migrated)
DELETE FROM public.message WHERE id IN (SELECT distinct e.message from envelope e where e.conversation is null)

-- Constrain values
ALTER TABLE public.envelope
	DROP COLUMN recipient,
	ALTER COLUMN conversation SET NOT NULL,
	ALTER COLUMN context SET NOT NULL
;

ALTER TABLE public.message
	DROP COLUMN sender
	ALTER COLUMN context SET NOT NULL
;

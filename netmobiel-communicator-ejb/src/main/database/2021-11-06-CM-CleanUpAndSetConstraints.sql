-- Clean up ol d columns
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

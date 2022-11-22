-- Add the flag for consent on safety guidelines
ALTER TABLE public.profile
  ADD COLUMN consent_safety_guidelines boolean
;

UPDATE public.profile SET consent_safety_guidelines = true where consent_safety_guidelines IS NULL;

ALTER TABLE public.profile
  ALTER COLUMN consent_safety_guidelines SET NOT NULL
;

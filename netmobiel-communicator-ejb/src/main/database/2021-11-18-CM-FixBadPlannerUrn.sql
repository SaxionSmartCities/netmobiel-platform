-- Fix planner urns
UPDATE public.message SET context = replace(context, 'urn:nb:pl:', 'urn:nb:pn:')

-- Banker - Extend incentives with call to action attributes

ALTER TABLE public.incentive 
	-- Only after this date the incentive is evaluated
	-- if not set it is always active
	ADD COLUMN start_time timestamp without time zone,
	-- The incentive is no longer evaluated after this date
	-- if not set it remains active
	ADD COLUMN end_time timestamp without time zone
;

-- Set the start date as per june 1st 2022.
UPDATE public.incentive 
	SET start_time = '2022-06-01 00:00:00'
	WHERE code = 'repeated-ride'
;

-- Fix typo
UPDATE public.incentive 
	SET cta_body = 'Verzilver maximaal 15 premiecredits bovenop de vergoeding die je al van de passagier ontvangt!'
	WHERE code = 'shared-ride-done'
;

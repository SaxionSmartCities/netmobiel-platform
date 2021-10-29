-- Planner - Add conversation context
ALTER TABLE public.trip 
	-- Urn of the message thread for this trip.
	-- urn:nb:cm:conversation:123456
	-- Value is set when exchanging first message and/or when originating trip plan has already a value set. 
	ADD COLUMN conversation character varying(32)
;

ALTER TABLE public.trip_plan 
	-- Urn of the message thread for this trip plan, in case of a shout-out.
	-- Value is set when shout-out is sent. 
	ADD COLUMN conversation character varying(32)
;
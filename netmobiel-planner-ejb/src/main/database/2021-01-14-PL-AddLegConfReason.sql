-- Planner - add confirmation reason attributes
ALTER TABLE public.leg
	ADD COLUMN conf_reason character varying(3),
	ADD COLUMN conf_reason_prov character varying(3)
;
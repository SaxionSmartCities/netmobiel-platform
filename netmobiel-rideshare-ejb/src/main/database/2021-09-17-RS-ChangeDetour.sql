-- Rideshare: Set the detour parameters 
update ride set max_detour_meters = 10000 WHERE max_detour_meters is NULL AND max_detour_seconds is NULL;
update ride set max_detour_meters = max_detour_seconds * 25 * 1000 / 3600 WHERE max_detour_meters is NULL AND max_detour_seconds is NOT NULL;
ALTER TABLE public.ride
	ALTER COLUMN max_detour_meters SET NOT NULL,
	ADD COLUMN able_to_assist boolean NOT NULL DEFAULT True
;

update ride_template set max_detour_meters = 10000 WHERE max_detour_meters is NULL AND max_detour_seconds is NULL;
update ride_template set max_detour_meters = max_detour_seconds * 25 * 1000 / 3600 WHERE max_detour_meters is NULL AND max_detour_seconds is NOT NULL;
ALTER TABLE public.ride_template
	ALTER COLUMN max_detour_meters SET NOT NULL,
	ADD COLUMN able_to_assist boolean NOT NULL DEFAULT True
;
-- Rideshare: Remove a forgotten column
 
ALTER TABLE public.ride
	DROP COLUMN leg_geometry
;

-- The ride template still has a leg_geometry. It is used as the geometry to reconstruct a leg. 
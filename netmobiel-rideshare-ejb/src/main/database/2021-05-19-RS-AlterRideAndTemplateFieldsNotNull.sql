ALTER TABLE public.ride
    ALTER COLUMN to_point SET NOT NULL,
    ALTER COLUMN from_point SET NOT NULL,
	ALTER COLUMN arrival_time SET NOT NULL,
	ALTER COLUMN departure_time SET NOT NULL,
    ALTER COLUMN driver SET NOT NULL,
    ALTER COLUMN car SET NOT NULL,
    ALTER COLUMN state SET NOT NULL,
    ALTER COLUMN version SET NOT NULL,
    ALTER COLUMN monitored SET NOT NULL
;

ALTER TABLE public.ride_template
    ALTER COLUMN to_point SET NOT NULL,
    ALTER COLUMN from_point SET NOT NULL,
	ALTER COLUMN arrival_time SET NOT NULL,
	ALTER COLUMN departure_time SET NOT NULL,
    ALTER COLUMN driver SET NOT NULL,
    ALTER COLUMN car SET NOT NULL,
    ALTER COLUMN recurrence_interval SET NOT NULL,
    ALTER COLUMN recurrence_unit SET NOT NULL
;

ALTER TABLE public.booking
    ALTER COLUMN to_point SET NOT NULL,
    ALTER COLUMN from_point SET NOT NULL
;
ALTER TABLE public.stop
    ALTER COLUMN point SET NOT NULL
;
-- Rideshare domain model massive change
-- Get rid of all bookings and the stops in the bookings
DELETE FROM public.stop s WHERE EXISTS(SELECT 1 FROM booking b WHERE s.id = b.pickup OR s.id = b.drop_off);
DELETE FROM public.booking;
ALTER TABLE public.booking
	DROP COLUMN drop_off;
	DROP COLUMN pickup;
	ADD COLUMN departure_time timestamp without time zone,
	ADD COLUMN arrival_time timestamp without time zone,
    ADD COLUMN from_label character varying(128),
    ADD COLUMN from_point geometry,
    ADD COLUMN to_label character varying(128),
    ADD COLUMN to_point geometry,
	
-- Table: public.booked_legs

CREATE TABLE public.booked_legs
(
    booking bigint NOT NULL,
    leg bigint NOT NULL,
    CONSTRAINT booked_legs_booking_fk FOREIGN KEY (booking) REFERENCES public.leg (id),
    CONSTRAINT booked_legs_leg_fk FOREIGN KEY (leg) REFERENCES public.booking (id)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

-- ALTER TABLE public.booked_legs OWNER to rideshare;

-- Expand ride attributes
ALTER TABLE public.ride
    ADD COLUMN carthesian_bearing integer,
    ADD COLUMN carthesian_distance integer,
    ADD COLUMN co2_emission integer,
    ADD COLUMN distance integer,
    ADD COLUMN max_detour_meters integer,
    ADD COLUMN max_detour_seconds integer,
    ADD COLUMN nr_seats_available integer,
    ADD COLUMN remarks character varying(256) COLLATE pg_catalog."default",
    ADD COLUMN share_eligibility geometry,
    ADD COLUMN leg_geometry geometry,
	ADD COLUMN driver bigint,
	ADD COLUMN car bigint,
    ADD COLUMN cancel_reason character varying(256),
    RENAME COLUMN estimated_arrival_time TO arrival_time,
    ADD CONSTRAINT ride_car_fk FOREIGN KEY (car) REFERENCES public.car (id),
    ADD CONSTRAINT ride_driver_fk FOREIGN KEY (driver) REFERENCES public.rs_user (id) 
;

-- Update the ride attributes (copy from template)
UPDATE public.ride SET carthesian_bearing = 	(SELECT rt.carthesian_bearing FROM ride_template rt WHERE ride_template = rt.id);
UPDATE public.ride SET carthesian_distance = 	(SELECT rt.carthesian_distance FROM ride_template rt WHERE ride_template = rt.id);
UPDATE public.ride SET co2_emission = 			(SELECT rt.estimated_co2_emission FROM ride_template rt WHERE ride_template = rt.id);
UPDATE public.ride SET distance = 				(SELECT rt.estimated_distance FROM ride_template rt WHERE ride_template = rt.id);
UPDATE public.ride SET max_detour_meters = 		(SELECT rt.max_detour_meters FROM ride_template rt WHERE ride_template = rt.id);
UPDATE public.ride SET max_detour_seconds = 	(SELECT rt.max_detour_seconds FROM ride_template rt WHERE ride_template = rt.id);
UPDATE public.ride SET nr_seats_available = 	(SELECT rt.nr_seats_available FROM ride_template rt WHERE ride_template = rt.id);
UPDATE public.ride SET remarks = 				(SELECT rt.remarks FROM ride_template rt WHERE ride_template = rt.id);
UPDATE public.ride SET share_eligibility = 		(SELECT rt.share_eligibility FROM ride_template rt WHERE ride_template = rt.id);
UPDATE public.ride SET driver = 				(SELECT rt.driver FROM ride_template rt WHERE ride_template = rt.id);
UPDATE public.ride SET car = 					(SELECT rt.car FROM ride_template rt WHERE ride_template = rt.id);

-- Add attributes to ride template (instead of stops)
ALTER TABLE public.ride_template
	ADD COLUMN departure_time timestamp without time zone,
	ADD COLUMN arrival_time timestamp without time zone,
    ADD COLUMN leg_geometry geometry,
    ADD COLUMN from_label character varying(128),
    ADD COLUMN from_point geometry,
    ADD COLUMN to_label character varying(128),
    ADD COLUMN to_point geometry,
    ADD COLUMN recurrence_time_zone character varying(32),
    ALTER COLUMN recurrence_horizon type timestamp without time zone
;

-- Copy ride departure and arrival from stops to template
UPDATE ride_template SET from_label = (SELECT s.label FROM stop s WHERE s.id = from_place.id);
UPDATE ride_template SET from_point = (SELECT s.point FROM stop s WHERE s.id = from_place.id);
UPDATE ride_template SET to_label = (SELECT s.label FROM stop s WHERE s.id = to_place.id);
UPDATE ride_template SET to_point = (SELECT s.point FROM stop s WHERE s.id = to_place.id);

-- The template departure and arrival places are mandatory
ALTER TABLE public.ride_template
    ADD COLUMN from_point SET NOT NULL,
    ADD COLUMN to_point SET NOT NULL
;
-- The states of the templates
UPDATE ride_template SET departure_time = 
	(SELECT MAX(r.departure_time) FROM Ride r WHERE ride_template.id = r.ride_template)
UPDATE ride_template SET arrival_time = 
	(SELECT MAX(r.arrival_time) FROM Ride r WHERE ride_template.id = r.ride_template)

-- Set the recurrence horizon to the UTC time instead of local date
-- Never mind, most horizonbs are not set or in the past.
 
--UPDATE ride_template SET recurrence_state = 
--	(SELECT date MAX(r.departure_time) FROM Ride r WHERE ride_template.id = r.ride_template)
--date(current_date + INTERVAL '8 week' - INTERVAL '1 day') 
--	WHERE recurrence_horizon is null or recurrence_horizon > date(current_date + INTERVAL '8 week' - INTERVAL '1 day')
;

-- Delete all ride template stops
DELETE FROM public.stop s WHERE EXISTS(SELECT 1 FROM ride_template rt WHERE s.id = rt.from_place OR s.id = rt.to_place);
-- Drop the ride template stop columns 
ALTER TABLE public.ride_template
	DROP COLUMN from_place,
	DROP COLUMN to_place
;



-- Add departure, arrival date and ride to stops
ALTER TABLE public.stop
	ADD COLUMN departure_time timestamp without time zone,
	ADD COLUMN arrival_time timestamp without time zone,
	ALTER COLUMN ride SET NOT NULL,
	RENAME COLUMN stop_order TO stop_ix
;

-- Add Legs
CREATE TABLE public.leg
(
    id bigint NOT NULL,
    distance integer,
    duration integer,
    leg_geometry geometry,
    from_stop bigint,
    to_stop bigint,
    ride bigint NOT NULL,
    leg_ix integer,
    CONSTRAINT leg_pkey PRIMARY KEY (id),
    CONSTRAINT leg_from_stop_fk FOREIGN KEY (from_stop)
        REFERENCES public.stop (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT leg_ride_fk FOREIGN KEY (ride)
        REFERENCES public.ride (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT leg_to_stop_fk FOREIGN KEY (to_stop)
        REFERENCES public.stop (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

--ALTER TABLE public.leg OWNER to rideshare;

-- Create legs
INSERT INTO leg (id, ride, distance, duration, from_stop, to_stop, leg_geometry)
	SELECT null, r.id, r.distance, rt.estimatedDrivingTime, null, null, null 
	FROM ride r JOIN ride_template rt ON r.ride_template = rt.id
;

-- Create from stop
INSERT INTO stop (id, ride, stop_ix, point, label, arrival_time, departure_time)
	SELECT null, r.id, 0, rt.from_point, rt.from_label, null, r.departure_time
	FROM ride r JOIN ride_template rt ON r.ride_template = rt.id
;
-- Create to stop
INSERT INTO stop (id, ride, stop_ix, point, label, arrival_time, departure_time)
	SELECT null, r.id, 1, rt.to_point, rt.to_label, r.estimated_arrival_time, null
	FROM ride r JOIN ride_template rt ON r.ride_template = rt.id
;
-- Connect leg to stops
UPDATE leg SET from_stop = (SELECT s.id FROM stop s WHERE s.ride = leg.ride AND s.stop_ix = 0);
UPDATE leg SET to_stop = (SELECT s.id FROM stop s WHERE s.ride = leg.ride AND s.stop_ix = 1);
ALTER TABLE public.leg 
	ALTER COLUMN ride SET NOT NULL,
	ALTER COLUMN from_stop SET NOT NULL,
	ALTER COLUMN to_stop SET NOT NULL
;


-- Change LocalTime to Instant.
-- TO DO
-- Modify the ride table
-- Add ride_template table
	
create table ride_template (
    id int8 not null,
    carthesian_bearing int4,
    carthesian_distance int4,
    estimated_co2_emission int4,
    estimated_distance int4,
    estimated_driving_time int4,
    max_detour_meters int4,
    max_detour_seconds int4,
    nr_seats_available int4 check (nr_seats_available<=99),
    recurrence_dsow int2,
    recurrence_horizon date,
    recurrence_interval int4,
    recurrence_unit varchar(1),
    remarks varchar(256),
    share_eligibility GEOMETRY,
    car int8 not null,
    driver int8 not null,
    from_place int8 not null,
    to_place int8 not null,
    primary key (id)
);
create sequence ride_template_id_seq start 50 increment 1;
ALTER SEQUENCE public.ride_template_id_seq OWNER TO rideshare;
ALTER TABLE public.ride_template OWNER to rideshare;    

alter table ride 
    add column ride_template int8, 
    drop column carthesian_bearing,
	drop column carthesian_distance,
	drop column estimated_co2_emission,
	drop column estimated_distance,
	drop column estimated_driving_time,
	drop column max_detour_meters,
	drop column max_detour_seconds,
	drop column nr_seats_available,
	drop column remarks,
	drop column share_eligibility,
	drop column car,
	drop column driver,
	drop column from_place,
	drop column to_place;

alter table ride 
   add constraint ride_ride_template_fk 
   foreign key (ride_template) 
   references ride_template;

alter table ride_template 
   add constraint ride_template_car_fk 
   foreign key (car) 
   references car;

alter table ride_template 
   add constraint ride_template_driver_fk 
   foreign key (driver) 
   references rs_user;

alter table ride_template 
   add constraint ride_template_from_stop_fk 
   foreign key (from_place) 
   references stop;

alter table ride_template 
   add constraint ride_template_to_stop_fk 
   foreign key (to_place) 
   references stop;

alter table rs_user 
	drop constraint if exists uk_5cok4010603wyyathlyr6yfj7,
	drop constraint if exists cs_managed_identity_unique,
	add constraint cs_managed_identity_unique unique (managed_identity);

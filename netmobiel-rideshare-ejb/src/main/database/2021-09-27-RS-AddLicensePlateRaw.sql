-- Rideshare: car type an color is optional, it is not known for old cars 
alter table public.car 
	add column license_plate_raw character varying(12)
;

update public.car set license_plate_raw = regexp_replace(license_plate, '[\s\-]', '', 'g')

alter table public.car 
	alter column license_plate_raw set not null
;

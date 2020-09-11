-- Rideshare 
ALTER TABLE public.ride 
	ADD COLUMN state character varying(3),
	ADD COLUMN monitored boolean NOT NULL DEFAULT False,
	ADD COLUMN confirmed boolean
;
update ride set state = 'SCH' WHERE arrival_time > current_timestamp;
update ride set state = 'CMP' WHERE arrival_time <= current_timestamp;
update ride set state = 'CNC' WHERE deleted = True;
alter table public.ride
	alter column state set not null
;
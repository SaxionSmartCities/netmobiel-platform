-- Report on available rides in a specific interval 
SELECT r.id, u.email, r.departure_time, r.from_label, ST_AsText(r.from_point) as from_point, 
	r.arrival_time, r.to_label, ST_AsText(r.to_point) as to_point, r.arrival_time_pinned, r.distance, 
	r.max_detour_meters, r.max_detour_seconds, r.nr_seats_available, r.deleted, r.cancel_reason
	FROM ride r join rs_user u on u.id = r.driver
	WHERE r.departure_time >= '2020-06-15' and r.departure_time <= '2020-06-18'
	ORDER BY r.id;
	
COPY (
SELECT r.id, u.email, r.departure_time, r.from_label, ST_AsText(r.from_point) as from_point, 
	r.arrival_time, r.to_label, ST_AsText(r.to_point) as to_point, r.arrival_time_pinned, r.distance, 
	r.max_detour_meters, r.max_detour_seconds, r.nr_seats_available, r.deleted, r.cancel_reason
	FROM ride r join rs_user u on u.id = r.driver
	WHERE r.departure_time >= '2020-06-15' and r.departure_time <= '2020-06-18'
	ORDER BY r.id
) TO '/tmp/created_rides.csv'
WITH (FORMAT CSV, DELIMITER ';', HEADER true);
	
-- Aantal boekingen per vertrekdag
select count(*) as nr_bookings, date_trunc('day', b.departure_time) as day 
from booking b 
where b.departure_time > '2022-04-02'
group by day order by day;

-- Alle ritten en email adres na een bepaalde vertrekdatum
select r.*, u.email from ride r join rs_user u on u.id = r.driver where r.departure_time > '2022-04-02' order by r.id

-- Aantal ritten per vertrekdag
select count(*) as nr_rides, date_trunc('day', r.departure_time) as day 
from ride r
where r.departure_time > '2022-04-02'
group by day order by day;

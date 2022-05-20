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

-- Aantal losse ritten
select count(*) from ride r
where r.departure_time > '2022-04-02' and ride_template is null;

-- Aantal herhalingen (waaruit ritten worden aangemaakt)
select count(*) from ride_template rt
where rt.departure_time > '2022-04-02';

-- Aantal ritten uit herhalingen
select count(*) from ride r
where r.departure_time > '2022-04-02' and ride_template is not null;


-- Count for all user the number of occasions (i.e, at each recurrent ride) how often there were at least 4 recurrent rides 
-- within 30 days, given a date range
SELECT driver, count(placed), (SELECT u.email FROM rs_user u WHERE driver = u.id)  FROM (
	SELECT r.driver as driver, r.departure_time, 
		(SELECT count(*) FROM ride rs 
		 WHERE rs.departure_time >= r.departure_time AND 
			rs.departure_time < r.departure_time + interval '30 days'
			AND rs.ride_template IS NOT null AND rs.driver = r.driver  
		 HAVING count(*) >= 4
		 ) AS placed
	FROM ride r 
	WHERE r.ride_template IS NOT null
		AND r.departure_time > '2022-01-01' AND r.departure_time < COALESCE(null, '2022-07-01')::date
	GROUP BY r.driver, r.departure_time
	ORDER by r.departure_time asc
) evcount GROUP BY driver
;

-- Count for a specifc user the number of occasions (i.e, at each recurrent ride) how often there were at least 4 recurrent rides 
-- within 30 days, given a date range
SELECT count(placed) FROM (
	SELECT r.departure_time, 
		(SELECT count(*) FROM ride rs 
		 WHERE rs.departure_time >= r.departure_time AND 
		 	rs.departure_time < r.departure_time + interval '30 days'
		 	AND rs.ride_template IS NOT null AND rs.driver = r.driver  
		 HAVING count(*) >= 4
		 ) AS placed
	FROM ride r 
	WHERE r.ride_template IS NOT null AND r.driver = 1 
		AND r.departure_time > '2022-01-01' AND r.departure_time < COALESCE(null, '2022-07-01')::date
) rec_rides30;

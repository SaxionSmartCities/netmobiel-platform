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
	
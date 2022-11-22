-- Maak een cirkel in Nederland voor de geo service waar heel Nederland in valt, maar niet ruimer dan dat.
select geometry(ST_Buffer(geography(ST_GeomFromText('POINT(5.35 52.25)')), 175000))


-- SRID WGS84 is 4326 (long, lat)
-- SRID RD Amersfoort New + NAP is 7415; RD rekent in meters.

-- Geef de clusters waar meer dan N routes passeren

-- Geef multimodale clusters, met voor elk cluster het aantal modaliteiten

-- Geef de clusters die op minder dan N meter van elkaar liggen en allebei meer dan M stop bevatten

-- Geef alle stops op 200 meter van station 
-- Hengelo station 52.2623176574707, 6.794579029083252
 select * 
 from otp_stop s 
 where ST_DWithin(ST_Transform(s.point, 7415), 
	    ST_Transform(st_geomFromText('POINT (6.794579 52.262317)', 4326), 7415),
	   200)
;

 select * 
 from otp_cluster c 
 where ST_DWithin(ST_Transform(c.point, 7415), 
	    ST_Transform(st_geomFromText('POINT (6.794579 52.262317)', 4326), 7415),
	   200)
;

-- Zoek de clusters rond een bepaald punt en tel de stops in elk cluster 
select *, (select count(*) from otp_stop s where s.cluster = c.id) 
from otp_cluster c 
where ST_DWithin(ST_Transform(c.point, 7415), 
				ST_Transform(st_geomFromText('POINT (6.794579 52.262317)', 4326), 7415),
				400)
;

-- Zoek de clusters rond een bepaald punt en tel de stops in elk cluster en de routes die door dat cluster lopen 
select distinct c.*, 
  (select count(*) from otp_stop s where s.cluster = c.id) as nrStops, 
  (select count(distinct r) 
  from otp_cluster c2 
  join otp_stop s on s.cluster = c2.id
  join otp_route_stop rs on rs.stop_id = s.id
  join otp_route r on r.id = rs.route_id
  where c2.id = c.id) as nrRoutes
  from otp_cluster c 
  join otp_stop s on s.cluster = c.id
  join otp_route_stop rs on rs.stop_id = s.id
  join otp_route r on r.id = rs.route_id
  where ST_DWithin(ST_Transform(c.point, 7415), 
					ST_Transform(st_geomFromText('POINT (6.794579 52.262317)', 4326), 7415),
					400);


-- Zoek de clusters rond een bepaald punt en tel de stops in elk cluster en de routes die door dat cluster lopen
-- Filter op minimaal aantal routes en order aflopend. 
select * from 
  (select distinct c.*, 
  (select count(*) from otp_stop s where s.cluster = c.id) as nrStops, 
  (select count(distinct r) 
  from otp_cluster c2 
  join otp_stop s on s.cluster = c2.id
  join otp_route_stop rs on rs.stop_id = s.id
  join otp_route r on r.id = rs.route_id
  where c2.id = c.id) as nr_routes
  from otp_cluster c 
  where ST_DWithin(ST_Transform(c.point, 7415), 
					ST_Transform(st_geomFromText('POINT (6.794579 52.262317)', 4326), 7415),
					1000)) ss where nr_routes >= 5 order by nr_routes desc

--Voorgaande queries zijn relatief zwaar (ca 300ms)
					
-- K-nearest stops. Stel rembrandtstraat 8 52.273401,6.7833773
-- See https://postgis.net/workshops/postgis-intro/knn.html
-- K-nearest stops met afstanden. Stel rembrandtstraat 8 52.273401,6.7833773
select *, ST_Distance(ST_Transform(s.point,7415), ST_Transform(st_geomFromText('POINT (6.7833773 52.273401)', 4326), 7415))
 from otp_stop s 
ORDER BY ST_Transform(s.point,7415) <-> ST_Transform(st_geomFromText('POINT (6.7833773 52.273401)', 4326), 7415)
LIMIT 10;
-- K-nearest clusters met afstanden. Stel rembrandtstraat 8 52.273401,6.7833773
select *, ST_Distance(ST_Transform(c.point,7415), ST_Transform(st_geomFromText('POINT (6.7833773 52.273401)', 4326), 7415))
 from otp_cluster c 
ORDER BY ST_Transform(c.point,7415) <-> ST_Transform(st_geomFromText('POINT (6.7833773 52.273401)', 4326), 7415)
LIMIT 10;
-- Deze queries zijn snel (< 100 ms)

-- K-nearest met route criterium
select *, ST_Distance(ST_Transform(ss.point,7415), ST_Transform(st_geomFromText('POINT (6.7833773 52.273401)', 4326), 7415)) as distance from
  (select c.*, 
  (select count(distinct r) 
  from otp_cluster c2 
  join otp_stop s on s.cluster = c2.id
  join otp_route_stop rs on rs.stop_id = s.id
  join otp_route r on r.id = rs.route_id
  where c2.id = c.id) as nr_routes
  from otp_cluster c 
  ORDER BY ST_Transform(c.point,7415) <-> ST_Transform(st_geomFromText('POINT (6.7833773 52.273401)', 4326), 7415)
LIMIT 10) ss 
where ss.nr_routes >= 5 order by ss.nr_routes desc
-- Relatief zwaar, 1200 msec

-- Met cluster index op de stop 844 msec
-- Met point index op cluster 829 msec

-- Overzicht van cluster met routes en type OV
select c.id, c.label, r.ov_type, count(distinct r) 
  from otp_cluster c 
  join otp_stop s on s.cluster = c.id
  join otp_route_stop rs on rs.stop_id = s.id
  join otp_route r on r.id = rs.route_id
group by c.id, c.label, r.ov_type
order by count(distinct r) desc
limit 50;


-- Multimodale clusters met gebruikmaking van de transfers
select distinct c1.id, c1.label, r1.ov_type, c2.id, c2.label, r2.ov_type, t.distance 
from otp_cluster c1 
join otp_stop s1 on s1.cluster = c1.id
join otp_route_stop rs1 on s1.id = rs1.stop_id
join otp_route r1 on r1.id = rs1.route_id
join otp_transfer t on s1.id = t.from_stop
join otp_stop s2 on s2.id = t.to_stop
join otp_route_stop rs2 on s2.id = rs2.stop_id
join otp_route r2 on r2.id = rs2.route_id
join otp_cluster c2 on c2.id = s2.cluster
where r1.ov_type <> r2.ov_type and c1.id <> c2.id
order by t.distance desc
limit 100;

-- De maximale distance is rond de 1900 meter. Dat is wel erg veel voor een overstap. En het gaat dan niet om schiphol.
-- Conclusie: De transfers van OTP zijn zo niet bruikbaar. Het is een configuratie instelling bam OTP: maxTransferDistance (default 2000m)

-- Geef alle multimodale clusters
select ss.id, ss.label, count(*) as nr_types from
(select c.id, c.label, r.ov_type, count(distinct r) as nrRoutes
  from otp_cluster c 
  join otp_stop s on s.cluster = c.id
  join otp_route_stop rs on rs.stop_id = s.id
  join otp_route r on r.id = rs.route_id
group by c.id, c.label, r.ov_type
order by count(distinct r) desc) as ss
group by ss.id, ss.label
having count(*) > 1;

-- Cluster "Q2x1c3RlcjpDNzQyNw==" heeft 4 OV types, dit is AMsterdam Centraal.

-- Geef alle routes voor een specifieke cluster
select distinct c.id, c.label, r.id, r.ov_type, r.long_name
  from otp_cluster c 
  join otp_stop s on s.cluster = c.id
  join otp_route_stop rs on rs.stop_id = s.id
  join otp_route r on r.id = rs.route_id
  where c.id = 'Q2x1c3RlcjpDNzQyNw=='
order by c.label, r.ov_type, r.long_name

-- Wat blijkt: Hier staat tram, metro, bus en ferry bij elkaar, maar de trein niet.

-- Sommige stops blijken door 2 verschillende route types gebruikt te worden.
select ss.id, ss.label, count(*) as nr_types from
(select s.id, s.label, r.ov_type, count(distinct r) as nrRoutes
  from otp_stop s
  join otp_route_stop rs on rs.stop_id = s.id
  join otp_route r on r.id = rs.route_id
group by s.id, s.label, r.ov_type
order by count(distinct r) desc) as ss
group by ss.id, ss.label
having count(*) > 1
-- Voorbeeld "U3RvcDpOTDozMjI2MTQ="

-- Beetje raar is het wel
select s.id, s.label, r.ov_type, r.long_name
  from otp_stop s
  join otp_route_stop rs on rs.stop_id = s.id
  join otp_route r on r.id = rs.route_id
where s.id = 'U3RvcDpOTDozMjI2MTQ='

-- Het aantal routes nu in de cluster tabel
select *, ST_Distance(ST_Transform(ss.point,7415), ST_Transform(st_geomFromText('POINT (6.7833773 52.273401)', 4326), 7415)) as distance from
  (select c.* from otp_cluster c 
  ORDER BY ST_Transform(c.point,7415) <-> ST_Transform(st_geomFromText('POINT (6.7833773 52.273401)', 4326), 7415)
LIMIT 100) ss 
where ss.nr_routes >= 5 order by ss.nr_routes desc
-- Executietijd gaat van 900 naar 100 ms

-- Rond Zieuwent
select *, ST_Distance(ST_Transform(ss.point,7415), ST_Transform(st_geomFromText('POINT (6.51783481357 52.0041659889)', 4326), 7415)) as distance from
  (select c.* from otp_cluster c 
  ORDER BY ST_Transform(c.point,7415) <-> ST_Transform(st_geomFromText('POINT (6.51783481357 52.0041659889)', 4326), 7415)
LIMIT 100) ss 
where ss.nr_routes >= 3 order by distance asc

-- Plan een route van de Kennedystraat Zieuwent naar Doetinchem Slingeland; dit zijn de stops in het eerste reisplan
-- Bus 191 naar Ruurlo, trein ri Zutphen naar Vorden, bus 51 naar Slingeland 
SELECT c.label, c.nr_routes, c.nr_stops, s.* from otp_stop s join otp_cluster c on c.id = s.cluster 
where s.gtfs_id in ('NL:1532280', 'NL:824', 'NL:644917', 'NL:259', 'NL:318828', 'NL:506568', 'NL:205', 
					'NL:1542567', 'NL:1542684', 
					'NL:1440343', 'NL:252332', 'NL:1440341', 'NL:126000', 'NL:387', 'NL:783', 'NL:1440409', 'NL:435215', 'NL:1544363', 'NL:239', 'NL:723', 'NL:1355723')
-- Alternatief plan 
-- Bus 191 naar Lichtenvoorde, Bus 74 naar Doetinchem, Bus 2 naar Vlinder Oost Doetinchem
SELECT c.label, c.nr_routes, c.nr_stops, s.* from otp_stop s join otp_cluster c on c.id = s.cluster 
where s.gtfs_id in ('NL:1532246', 'NL:1355699', 'NL:1355699', 'NL:1440434', 'NL:1548283', 'NL:1301899', 'NL:25', 'NL:1355707', 
					'NL:1524258', 'NL:819', 'NL:1524250', 'NL:95', 'NL:1079', 'NL:14', 'NL:957', 'NL:701', 'NL:326973')

					
-- Geef een overzicht van trip en bijbehorende legs.
SELECT t.id as trip_id, t.traveller, t.state, it.departure_time, t.deleted, t.cancel_reason, it.id as it_id, leg.id as leg_id, leg.state, leg.payment_state, leg.payment_id
	FROM public.trip t join itinerary it on t.itinerary = it.id join leg leg on leg.itinerary = it.id
	order by t.id asc;

-- Geef een overzicht van trip plan, legs en booking id's
select p.id as plan_id, it.id as it_id, it.departure_time, lg.id as leg_id, lg.booking_id, lg.state as leg_state, lg.driver_id from trip_plan p
join itinerary it on it.trip_plan = p.id
join leg lg on lg.itinerary = it.id
where p.plan_type = 'SHO' and lg.traverse_mode= 'RS'
order by p.id desc,lg.id desc

-- Analyze Planner errors

SELECT p.id, p.creation_time, p.error_vendor_code, p.execution_time, p.from_label, st_astext(p.from_point), 
	p.to_label, st_astext(p.to_point), st_astext(p.request_geometry) 
	from planner_report p WHERE p.error_vendor_code is not null;

-- Check distances in case of TOO_CLOSE with VIA routing
-- 7415 is the Dutch Rijksdriehoekcoordinaten system, with coordinates in meter. That is easier for reading the distance :-)
SELECT p.id, p.travel_time, p.from_label, p.from_point, p.to_label, p.to_point, r.traverse_mode, v.label as via_label, v.point as via_point,
st_distance(ST_Transform(p.from_point, 7415), 
				   ST_Transform(p.to_point, 7415))	as d_from_to,
st_distance(ST_Transform(p.from_point, 7415), 
				   ST_Transform(v.point, 7415))	as d_from_via,
st_distance(ST_Transform(p.to_point, 7415), 
				   ST_Transform(v.point, 7415))	as d_via_to
FROM planner_report p 
JOIN report_traverse_mode r on r.report_id = p.id 
JOIN report_via v on v.report_id = p.id
WHERE p.error_vendor_code = 'TOO_CLOSE'


-- Check distances in case of TOO_CLOSE with simple 2-points routing
SELECT p.id, p.travel_time, p.from_label, p.from_point, p.to_label, p.to_point, r.traverse_mode,
st_distance(ST_Transform(p.from_point, 7415), 
				   ST_Transform(p.to_point, 7415))	as d_from_to
FROM planner_report p 
JOIN report_traverse_mode r on r.report_id = p.id 
WHERE p.error_vendor_code = 'TOO_CLOSE' AND NOT EXISTS (select 1 FROM report_via v WHERE v.report_id = p.id)

-- Simple static transformation and calculation
select st_distance(ST_Transform(st_geomFromText('POINT(6.749130 52.298530)', 4326), 7415), 
				   ST_Transform(st_geomFromText('POINT(6.748682 52.298512)', 4326), 7415))

-- Aantal trip plan zoekacties				   
select count(*) as count
from trip_plan p 
where p.creation_time > '2022-04-02'
			
-- Number of any searches per day since a specific date
select count(*) as nr_searches, date_trunc('day', p.creation_time) as day 
from trip_plan p 
where p.creation_time > '2022-04-02' --and p.plan_type = 'REG'
group by day order by day;

-- Aantal shout-outs				   
select count(*) as count
from trip_plan p 
where p.creation_time > '2022-04-02' and p.plan_type = 'SHO'

-- Number of shout-outs per day since a specific date
select count(*) as nr_shout_outs, date_trunc('day', p.creation_time) as day 
from trip_plan p 
where p.creation_time > '2022-04-02' and p.plan_type = 'SHO'
group by day order by day;

-- Aantal shout-out oplossingen				   
select count(*) as count
from trip_plan p 
where p.creation_time > '2022-04-02' and p.plan_type = 'SOS'

-- Number of travel offers per day since a specific date
select count(*) as nr_shout_outs, date_trunc('day', p.creation_time) as day 
from trip_plan p 
where p.creation_time > '2022-04-02' and p.plan_type = 'SOS'
group by day order by day;


-- Number of shout-outs per day since a specific date that have been fulfilled and accepted
select count(*) as nr_shout_outs, date_trunc('day', p.creation_time) as day 
from trip_plan p 
where p.creation_time > '2022-04-02' and p.plan_type = 'SHO' and p.plan_state = 'FN'
group by day order by day;

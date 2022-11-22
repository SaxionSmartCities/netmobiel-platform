-- Hengelo station 52.2623176574707, 6.794579029083252 POINT (6.794579 52.262317)
-- Zieuwent, Kennedystraat::52.004166,6.517835 POINT (6.517835 52.004166)
-- includes zutphen, doetinchem
create table otp_cluster_export as 
 select * 
 from otp_cluster c 
 where ST_DWithin(ST_Transform(c.point, 7415), 
	    ST_Transform(st_geomFromText('POINT (6.517835 52.004166)', 4326), 7415),
	   30000)
;
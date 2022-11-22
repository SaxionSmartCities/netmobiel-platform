-- Select all profiles that have a home location set to 0, 0.

SELECT st_astext(p.home_point), p.* from profile p WHERE st_astext(p.home_point) = 'POINT(0 0)'

-- Number of new profiles counted by day
select count(*) as nr_profiles, date_trunc('day', p.creation_time) as day 
from profile p 
where p.creation_time > '2022-04-02'
group by day order by day;

-- Number of new profiles created after a certain date
select count(*) as nr_profiles from profile p where p.creation_time > '2022-04-02';

-- Report about the home localities of the users
SELECT count(*) as count, p.home_locality, p.home_state_code as woonplaats
	FROM public.profile p
	WHERE p.creation_time > '2022-04-02'
	GROUP BY p.home_locality, p.home_state_code
	ORDER BY count DESC, p.home_locality ASC
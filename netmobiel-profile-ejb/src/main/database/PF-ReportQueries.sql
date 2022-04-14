-- Select all profiles that have a home location set to 0, 0.

SELECT st_astext(p.home_point), p.* from profile p WHERE st_astext(p.home_point) = 'POINT(0 0)'

-- Number of new profiles countend by day
select count(*) as nr_profiles, date_trunc('day', p.creation_time) as day from profile p group by day order by day;

select count(*) as nr_profiles, from profile p where p.creation_time > '2022-04-02';

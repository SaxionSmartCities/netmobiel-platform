-- Select all profiles that have a home location set to 0, 0.

SELECT st_astext(p.home_point), p.* from profile p WHERE st_astext(p.home_point) = 'POINT(0 0)'

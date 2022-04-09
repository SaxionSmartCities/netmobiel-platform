-- Profile Service: Clear invalid home addresses

UPDATE public.profile SET home_country_code = 'NLD' WHERE home_country_code = 'NL';

UPDATE public.profile SET home_point = null, home_locality = null, home_street = null, 
	home_postal_code = null, home_house_nr = null, home_label = null, home_state_code = null
	WHERE st_astext(home_point) = 'POINT(0 0)'
;



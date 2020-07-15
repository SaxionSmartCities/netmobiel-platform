-- All CAR traverse mode is issued from Rideshare, for now.
UPDATE leg
    SET agency_id = 'NB:RS', agency_name = 'NetMobiel Rideshare Service' 
    WHERE traverse_mode = 'CR' and agency_id is null
;    
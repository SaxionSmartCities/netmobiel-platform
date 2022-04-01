-- Profile Service: Wipe dynamic data

-- public.survey;
TRUNCATE public.survey_interaction;

TRUNCATE public.compliment_set;
TRUNCATE public.compliment;
TRUNCATE public.review;

-- Tricky, has setting in Keycloak
-- public.delegation;

-- Keep all user data
-- public.passenger_luggage;
-- public.place;
-- public.preferred_traverse_mode;
-- public.profile;
-- public.rideshare_luggage;
-- public.rideshare_preferences;
-- public.search_preferences;

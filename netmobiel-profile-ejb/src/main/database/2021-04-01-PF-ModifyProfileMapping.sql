-- Make rideshare and search preferences unidirectional, and pure one-to-one

ALTER TABLE public.rideshare_preferences RENAME COLUMN defaultcarref TO default_car_ref;
ALTER TABLE public.rideshare_preferences DROP CONSTRAINT fkrlbivlpiqbvs6qtvf4n1jsf4x;
ALTER TABLE public.rideshare_preferences ADD CONSTRAINT rideshare_prefs_profile_fk FOREIGN KEY (id) REFERENCES public.profile;

ALTER TABLE public.search_preferences DROP CONSTRAINT fkn3simblon7opqbkk89mabkaqj;
ALTER TABLE public.search_preferences ADD CONSTRAINT search_prefs_profile_fk FOREIGN KEY (id) REFERENCES public.profile;

ALTER TABLE public.passenger_luggage RENAME COLUMN profile TO prefs;
ALTER TABLE public.passenger_luggage ADD CONSTRAINT passenger_luggage_prefs_fk FOREIGN KEY (prefs) REFERENCES public.search_preferences;

ALTER TABLE public.preferred_traverse_mode RENAME COLUMN profile TO prefs;
ALTER TABLE public.preferred_traverse_mode ADD CONSTRAINT preferred_traverse_mode_prefs_fk FOREIGN KEY (prefs) REFERENCES public.search_preferences;

ALTER TABLE public.ridehare_luggage RENAME TO rideshare_luggage;
ALTER TABLE public.rideshare_luggage RENAME COLUMN profile TO prefs;
ALTER TABLE public.rideshare_luggage ADD CONSTRAINT rideshare_luggage_prefs_fk FOREIGN KEY (prefs) REFERENCES public.rideshare_preferences;


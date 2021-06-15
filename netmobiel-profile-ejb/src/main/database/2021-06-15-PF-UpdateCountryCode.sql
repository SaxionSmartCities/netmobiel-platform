-- Rename the country code from ISO 3166-2 to 3166-3
UPDATE public.profile SET home_country_code = 'NLD' WHERE home_country_code = 'NL';
UPDATE public.profile SET home_country_code = 'NLD' WHERE home_country_code IS NULL;
UPDATE public.place SET country_code = 'NLD' WHERE country_code = 'NL';

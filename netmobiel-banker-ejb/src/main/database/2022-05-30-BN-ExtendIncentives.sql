-- Banker - Extend incentives with call to action attributes

ALTER TABLE public.incentive 
	ADD COLUMN cta_enabled boolean NOT NULL DEFAULT FALSE,
	ADD COLUMN cta_title character varying(128),
	ADD COLUMN cta_body character varying(256),
	ADD COLUMN cta_button_label character varying(48),
	-- Button action is a vocabulary, opaque to the banker.
	ADD COLUMN cta_button_action character varying(32),
	ADD COLUMN cta_hide_beyond_reward_count INTEGER
;

--INSERT INTO incentive (id, code, category, description, amount)
--	VALUES (nextval('incentive_seq'), 'profile-starter', 'PROFILE', 'Eerste gebruik van de NetMobiel app', 10)
--;
--INSERT INTO incentive (id, code, category, description, amount)
--	VALUES (nextval('incentive_seq'), 'profile-plus', 'PROFILE', 'Beantwoorden van aanvullende profielvragen', 10)
--;
--INSERT INTO incentive (id, code, category, description, amount)
--	VALUES (nextval('incentive_seq'), 'profile-youth', 'PROFILE', 'Je jeugdige leeftijd', 25)
--;
--INSERT INTO incentive (id, code, category, description, amount, max_amount, redemption, relative)
--	VALUES (nextval('incentive_seq'), 'shared-ride-done', 'CARPOOL', 'Meenemen van een passagier', 50, 15, TRUE, TRUE)
--;
--INSERT INTO incentive (id, code, category, description, amount)
--	VALUES (nextval('incentive_seq'), 'survey-0', 'SURVEY', 'Invullen van de enquête voor nieuwe gebruikers', 40)
--;

UPDATE public.incentive 
	SET cta_enabled = true, 
		cta_title = 'Verdien met je profiel',
		cta_body = 'Maak je profiel compleet en verdien 10 credits!',
		cta_button_label = 'Ik doe mee',
		cta_button_action = 'onboarding-profile',
		cta_hide_beyond_reward_count = 0
WHERE code = 'profile-plus';

UPDATE public.incentive 
	SET cta_enabled = true, 
		cta_title = 'Verdien met een enquête',
		cta_body = 'Vul de enquête in en verdien 40 credits!',
		cta_button_label = 'Start Enquête',
		cta_button_action = 'start-survey',
		cta_hide_beyond_reward_count = 0
WHERE code = 'survey-0';

UPDATE public.incentive 
	SET cta_enabled = true, 
		cta_title = 'Verdien extra met meenemen van passagiers',
		cta_body = 'Verzilver maximaal 15 premiecredits bovenop de vergoeding die je al van de passgier ontvangt!',
		cta_button_label = 'Bied Rit aan',
		cta_button_action = 'plan-ride',
		cta_hide_beyond_reward_count = 1
WHERE code = 'shared-ride-done';

--UPDATE public.incentive 
--	SET cta_enabled = true, 
--		cta_title = 'Verdien 15 premiecredits met herhaalde ritten',
--		cta_body = 'Maak je regelmatig dezelfde autorit? Bied deze dan aan als een herhaalde rit. Bied je minimaal 4 herhaalde ritten aan binnen 30 dagen, dan krijg je gratis 15 premiecredits!',
--		cta_button_label = 'Bied Ritten aan',
--		cta_button_action = 'plan-ride',
--		cta_hide_beyond_reward_count = 0
--WHERE code = 'repeated-ride';

INSERT INTO incentive (id, code, category, description, amount, cta_enabled, cta_title, cta_body, cta_button_label, cta_button_action, cta_hide_beyond_reward_count)
	VALUES (nextval('incentive_seq'), 'repeated-ride', 'CARPOOL', 'Aanbieden van minstens 4 herhaalde ritten in 30 dagen', 15,
	true, 'Verdien 15 premiecredits met herhaalde ritten',
	'Maak je regelmatig dezelfde autorit? Bied deze dan aan als een herhaalde rit. Bied je minimaal 4 herhaalde ritten aan binnen 30 dagen, dan krijg je gratis 15 premiecredits!',
	'Bied Ritten aan',
	'plan-ride',
	0
	)
;

-- Zoek alle incentives die niet aan de criteria voldoen en die een CTA mogen gebruiken
select inc.* from incentive inc where inc.cta_enabled and not exists 
	(select 1 from reward r
	 join bn_user u on u.id = r.recipient
	 where r.incentive = inc.id and u.email = 'passagier-acc@netmobiel.eu' and inc.cta_enabled
	 group by r.incentive
	 having count(*) > inc.cta_hide_beyond_reward_count
	)
order by inc.id asc;


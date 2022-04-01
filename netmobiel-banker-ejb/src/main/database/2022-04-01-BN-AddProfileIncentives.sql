-- Banker - Add incentives for profile data

INSERT INTO incentive (id, code, category, description, amount)
	VALUES (nextval('incentive_seq'), 'profile-starter', 'PROFILE', 'Eerste gebruik van de NetMobiel app', 10)
;
INSERT INTO incentive (id, code, category, description, amount)
	VALUES (nextval('incentive_seq'), 'profile-plus', 'PROFILE', 'Beantwoorden van aanvullende profielvragen', 10)
;
INSERT INTO incentive (id, code, category, description, amount)
	VALUES (nextval('incentive_seq'), 'profile-youth', 'PROFILE', 'Je jeugdige leeftijd', 25)
;


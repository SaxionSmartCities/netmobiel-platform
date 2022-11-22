-- Banker - Add an incentive for surveys

INSERT INTO incentive (id, code, category, description, amount)
	VALUES (nextval('incentive_seq'), 'survey-0', 'SURVEY', 'Invullen van de enquête voor nieuwe gebruikers', 10)
;


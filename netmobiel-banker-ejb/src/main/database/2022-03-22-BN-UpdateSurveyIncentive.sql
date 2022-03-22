-- Banker - Update incentive for first survey

UPDATE incentive SET description = 'Invullen van de enquête voor nieuwe gebruikers', SET amount = 40 WHERE code = 'survey-0';


-- Banker - Add an incentive for carpooling
-- This is a redemption type incentive, based on the ride fare price
-- The amount field is a percentage of the fare and capped by max_amount.
-- The code is hardcoded in the Overseer PaymentProcessor.

INSERT INTO incentive (id, code, category, description, amount, max_amount, redemption, relative)
	VALUES (nextval('incentive_seq'), 'shared-ride-done', 'CARPOOL', 'Meenemen van een passagier', 50, 20, TRUE, TRUE)
;


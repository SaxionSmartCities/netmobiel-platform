-- Banker: Fix invalid unique constraint
ALTER TABLE public.reward
	DROP CONSTRAINT cs_reward_unique
;
ALTER TABLE public.reward
	ADD CONSTRAINT cs_reward_unique UNIQUE (incentive, recipient, fact_context)
;
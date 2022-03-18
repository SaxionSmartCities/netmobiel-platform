-- Banker - Add the premium redemption feature as well as a percentage relative reward. 

ALTER TABLE public.incentive
	-- Is the amount absolute or relative?
    ADD relative boolean NOT NULL DEFAULT false,
    -- Is the incentive a payment in premium credits or a redemption of premium credits
    ADD redemption boolean NOT NULL DEFAULT false,
    -- The maximum amount of credits that can be rewarded (redeemed or as premium).
    ADD max_amount INTEGER,
    -- Time to disable the incentive
    ADD disable_time timestamp without time zone
;

ALTER TABLE public.reward
	-- if true the reward ispaid with premium credits or a redemption.
    ADD paid_out boolean NOT NULL DEFAULT false
;
UPDATE public.reward SET paid_out = true WHERE transaction is not null; 

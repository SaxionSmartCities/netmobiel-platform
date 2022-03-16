-- Banker - Add the premium redemption feature as well as a percentage relative reward. 

ALTER TABLE public.incentive
	-- Is the amount absolute or relative?
    ADD percentage boolean NOT NULL DEFAULT false,
    -- Is the incentive a payment in premium credits or a redemption of premium credits
    ADD redemption boolean NOT NULL DEFAULT false,
    -- The maximum number of premium credits that can be redeemed.
    ADD max_redemption INTEGER
;



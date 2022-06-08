-- Profile Service: Drop FCM token and timestamp, these are moved to the communicator. 

ALTER TABLE public.profile
	DROP COLUMN fcm_token,
	DROP COLUMN fcm_token_timestamp
;


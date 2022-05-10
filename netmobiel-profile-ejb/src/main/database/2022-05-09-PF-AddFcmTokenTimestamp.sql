-- Profile Service: Add a timestamp to the FCM token

ALTER TABLE public.profile
	ADD COLUMN fcm_token_timestamp timestamp without time zone
;


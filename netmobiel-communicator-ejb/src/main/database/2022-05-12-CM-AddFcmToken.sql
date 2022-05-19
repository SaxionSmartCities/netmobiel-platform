-- Communicator: Add FCM token and the timestamp, plus phone number

ALTER TABLE public.cm_user
	-- Add support for Firebase Cloud Messaging
    ADD COLUMN fcm_token character varying(512),
	ADD COLUMN fcm_token_timestamp timestamp without time zone,
	-- Add attributes for SMS messaging
	ADD COLUMN phone_number character varying(16),
	ADD COLUMN country_code character varying(3)
;


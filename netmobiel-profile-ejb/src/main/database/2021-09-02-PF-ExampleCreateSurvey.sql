-- Create the survey entry for Qualtrics test survey created by Mark M.

INSERT INTO public.survey(
	id, survey_id, display_name, remarks, take_delay_hours, take_interval_hours, 
	start_time, end_time, reward_credits)
	VALUES (nextval('survey_id_seq'), 'SV_38g7VCfcJFDB75X', 'Netmobiel - onboardingvragenlijst' , 'Test voor front-end', 24, null, 
			'2021-09-01 00:00:00', '2021-10-01 00:00:00', 40);
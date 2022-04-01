-- Planner: Wipe dynamic data, but keep the OTP data


TRUNCATE public.report_traverse_mode;
TRUNCATE public.report_via;
TRUNCATE public.planner_report;

TRUNCATE public.trip;
TRUNCATE public.plan_traverse_mode;
TRUNCATE public.trip_plan CASCADE;

TRUNCATE public.itinerary;
TRUNCATE public.stop;
TRUNCATE public.guide_step;
TRUNCATE public.leg;


-- public.otp_cluster;
-- public.otp_route;
-- public.otp_route_stop;
-- public.otp_stop;
-- public.otp_transfer;

-- public.pl_user;

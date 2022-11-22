-- Planner: Wipe dynamic data, but keep the OTP data

TRUNCATE public.report_traverse_mode, public.report_via, public.planner_report, public.trip, public.plan_traverse_mode, 
  public.trip_plan, public.itinerary, public.stop, public.guide_step, public.leg;

-- public.otp_cluster;
-- public.otp_route;
-- public.otp_route_stop;
-- public.otp_stop;
-- public.otp_transfer;

-- public.pl_user;

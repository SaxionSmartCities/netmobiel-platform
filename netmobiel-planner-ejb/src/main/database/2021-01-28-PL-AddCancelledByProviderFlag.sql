-- Planner: Add cancelled by transport provider column to leg and trip
alter table public.leg add column cancelled_by_provider boolean;
alter table public.trip add column cancelled_by_provider boolean;


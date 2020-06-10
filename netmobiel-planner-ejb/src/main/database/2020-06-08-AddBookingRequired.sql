-- Planner: Add booking required column
alter table leg add column booking_required boolean;
update leg set booking_required = true where leg.traverse_mode = 'RS';

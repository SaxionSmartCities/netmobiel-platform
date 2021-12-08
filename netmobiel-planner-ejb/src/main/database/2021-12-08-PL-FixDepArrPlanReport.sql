-- Swap latestArrival and earliestDeparture in plan_report table
UPDATE public.planner_report SET earliest_departure_time = latest_arrival_time, latest_arrival_time = earliest_departure_time 
	WHERE earliest_departure_time > latest_arrival_time;
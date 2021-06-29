-- Planner: Execution time is always set.
ALTER TABLE public.planner_report
    ALTER COLUMN execution_time SET NOT NULL
;

-- Create a report on the number of profiles created by year and month 
select count(*) as created_count, extract(year from p.creation_time) as year, extract(month from p.creation_time) as month 
from profile p group by year, month order by year, month

-- BN: Report queries. Scratch file.

select d.bn_user, sum(d.amount) from donation d where charity = 53 group by bn_user order by sum(amount) desc 

select count(*) from 
 (select d.bn_user, sum(d.amount) from donation d group by bn_user order by sum(amount) desc) der; 

select d.* from donation d;

-- Populair in de buurt: Count distinct users per charity ordered by descending donor count and by
-- charity id descending (younger charities are prioritized)
select d.charity, count(distinct d.bn_user) from donation d 
	where d.anonymous = false 
 	group by d.charity 
 	order by count(distinct d.bn_user) desc
; 


-- Donated before: List of latest donations by a specific donor to each charity ordered by donation date descending 
select distinct d.* from donation d 
	where d.bn_user = 53 and (d.charity, d.donation_time) in 
    (select dd.charity, max(dd.donation_time) from donation dd
     	where dd.bn_user = 53 and dd.anonymous = false 
     	group by dd.charity) 
    order by d.donation_time desc
; 
 
-- Donated in total to any charity ordered by total amount donated descending
select d.bn_user, sum(d.amount) from donation d 
	where d.anonymous = false 
	group by bn_user 
	order by sum(amount) desc
;

-- List top N donors for a specific charity, ordered by donated summed amount descending 
-- and user id descending (users that joined more recently are prioritized). MOre or less same as previous query.
select d.bn_user, sum(d.amount) from donation d 
	where d.charity = 53 and d.anonymous = false 
	group by bn_user 
	order by sum(d.amount) desc, d.bn_user desc
;

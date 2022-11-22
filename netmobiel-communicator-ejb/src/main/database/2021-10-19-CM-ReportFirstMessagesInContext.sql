select distinct m.id, m.body, m.context, m.subject, m.sender, m.created_time, e.recipient from envelope e join message m on m.id = e.message
where (m.context, m.created_time, e.recipient) in 
(select mm.context, min(mm.created_time), ee.recipient from envelope ee join message mm on mm.id = ee.message  
 group by mm.context, ee.recipient)
order by m.created_time asc

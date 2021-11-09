-- Report on conversations by user

SELECT c.id as conv_id, m.id as msg_id, m.context, e.context, m.body, u.given_name, u.family_name from Envelope e
join message m on m.id = e.message
join conversation c on c.id = e.conversation
join cm_user u on u.id = c.owner
where u.id = 58
order by c.id desc, m.id desc

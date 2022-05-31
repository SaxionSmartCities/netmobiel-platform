-- Communicator: Acknowledge old envelopes, it is a lot of work from the GUI.

-- select e.*, m.* from envelope e join message m on m.id = e.message where e.ack_time is null and m.created_time < '2022-01-01';

update envelope set ack_time = '2022-01-01 00:00:00' 
	from message 
	where message.id = envelope.message and ack_time is null and message.created_time < '2022-01-01'
;
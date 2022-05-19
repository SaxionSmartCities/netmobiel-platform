-- Communicator: Repair message context with correct booking context from envelope

ALTER TABLE message
	ADD COLUMN orig_context character varying(32)
;

UPDATE message SET orig_context = context; 

UPDATE message SET context = envelope.context 
FROM envelope 
WHERE envelope.message = message.id AND message.context <> envelope.context AND envelope.context like '%booking%'
;

UPDATE envelope SET context = message.orig_context 
FROM message 
WHERE envelope.message = message.id AND message.orig_context not like '%booking%' AND envelope.context like '%booking%'
;

ALTER TABLE message
	DROP COLUMN orig_context
;


SELECT m.context, e.context FROM message m JOIN envelope e on e.message = m.id
WHERE e.context like '%booking%' AND 
	(SELECT count(*) from conversation_context cc 
	 WHERE cc.conversation = e.conversation AND cc.context like '%ride%') > 0
;

ALTER TABLE envelope
	ADD COLUMN orig_context character varying(32)
;
UPDATE envelope SET orig_context = context; 

-- Replace recipient context booking with ride context
UPDATE envelope SET context = 
	(SELECT DISTINCT cc.context FROM conversation_context cc 
	 JOIN conversation c ON c.id = cc.conversation 
	 JOIN envelope e ON e.conversation = c.id
	 WHERE e.id = envelope.id AND cc.context like '%ride%') 
WHERE context like '%booking%'	 
;

-- Replace driver recipient trip context with ride context 
UPDATE envelope SET context = 
	(SELECT DISTINCT cc.context FROM conversation_context cc 
	 JOIN conversation c ON c.id = cc.conversation 
	 JOIN envelope e ON e.conversation = c.id
	 WHERE e.id = envelope.id AND cc.context like '%ride%') 
FROM conversation c
WHERE conversation = c.id and context like '%trip:%' and not sender and c.owner_role = 'DR'
;

ALTER TABLE envelope
	DROP COLUMN orig_context
;

-- CHeck whether there are messages with a context that is not in the conversation context
SELECT distinct e.id, e.message, e.context as env_context, m.context as msg_context, m.body, e.conversation, e.sender, c.owner, c.topic, c.owner_role
	FROM public.envelope e
	JOIN public.message m ON e.message = m.id
	JOIN public.conversation c ON e.conversation  = c.id
	WHERE -- m.context like '%booking%' and
	 not exists (select 1 from public.conversation_context cc where cc.conversation = c.id and cc.context = m.context)
	ORDER BY e.id ASC;

-- Assure all message contexts are in conversation contexts as well.
-- That should be booking and tripplan only for now
INSERT INTO public.conversation_context (context, conversation)
SELECT DISTINCT m.context as msg_context, e.conversation
	FROM public.envelope e
	JOIN public.message m ON e.message = m.id
	JOIN public.conversation c ON e.conversation  = c.id
	WHERE m.context NOT IN (select context from conversation_context where conversation = e.conversation)
	ORDER BY e.conversation ASC
;
	
-- Ensure all sender envelopes are acknowledged
UPDATE envelope SET ack_time = m.created_time 
FROM message m where envelope.message = m.id and envelope.ack_time is null and envelope.sender = true;
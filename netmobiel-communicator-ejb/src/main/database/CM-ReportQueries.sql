-- Select envelope and message context
SELECT e.id, e.message, e.context as env_context, m.context as msg_context, m.body, e.conversation, e.sender, c.owner, c.topic
	FROM public.envelope e
	JOIN public.message m ON e.message = m.id
	JOIN public.conversation c ON e.conversation  = c.id
	WHERE e.context <> m.context and m.context not like '%booking%' 
	--and not exists (select 1 from public.conversation_context cc where cc.conversation = c.id and cc.context = e.context)
	ORDER BY m.id ASC
;

-- Select all contexts
SELECT e.id, e.message, e.context as env_context, m.context as msg_context, cc.context as conv_context, e.conversation, e.sender, c.owner, c.topic
	FROM public.envelope e
	JOIN public.message m ON e.message = m.id
	JOIN public.conversation c ON e.conversation  = c.id
	JOIN public.conversation_context cc ON cc.conversation  = c.id
	WHERE c.id = 133
	ORDER BY m.id ASC
;

-- Select all envelopes
SELECT distinct e.id, e.message, e.context as env_context, m.context as msg_context, m.body, e.conversation, e.sender, c.owner, c.topic, c.owner_role
	FROM public.envelope e
	JOIN public.message m ON e.message = m.id
	JOIN public.conversation c ON e.conversation  = c.id
	JOIN public.conversation_context cc ON cc.conversation  = c.id
	--WHERE c.id = 137 
	--WHERE exists (select 1 from public.conversation_context where conversation = c.id and context = 'urn:nb:pn:tripplan:785')
	--WHERE e.context = 'urn:nb:rs:booking:113' and e.context <> m.context
	--WHERE e.sender = true
	WHERE m.context like '%booking%' and e.context like '%trip:%' and c.owner_role = 'DR'
	ORDER BY e.id ASC;

	
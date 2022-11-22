-- Communicator: Remove the duplicate conversations (caused by a race 

-- List the duplicates
select conversation, context from conversation_context where context in (
select cc.context from conversation_context cc 
where cc.context not like '%:booking:%' and cc.context not like '%:tripplan:%'
group by cc.context having count(*) > 1
);

-- Assign the lowest id of the duplicated conversations to the envelopes involved
-- Note: Not necessarily the best solution, but with 'Persoonlijke berichten' it works.
-- Only booking and tripplan contexts are shared between passenger and driver. 
-- All others must be unique. 
--begin;
with dups as (
	select conversation, context from conversation_context where context in (
	select cc.context from conversation_context cc 
	where cc.context not like '%:booking:%' and cc.context not like '%:tripplan:%'
	group by cc.context having count(*) > 1
	) 
), mindups as (
	select min(conversation) as mincv, context from dups group by context
)
update envelope SET conversation = 
	(select md.mincv from mindups md
	 where md.context = (select d.context from dups d where d.conversation = envelope.conversation)
	)
where envelope.conversation in (select d.conversation from dups d except select md.mincv from mindups md)
--returning envelope.*;
--rollback;

-- Check the obsoleted conversations
select * from conversation where not exists (select 1 from envelope e where e.conversation = conversation.id)
-- Remove the obsoleted conversations
delete from conversation where not exists (select 1 from envelope e where e.conversation = conversation.id)


-- Now add an initial context to the conversation, to assure uniqueness at creation
-- the initial context is also the first context is the list of contexts associated with the conversation.  
alter table conversation
	add column initial_context character varying(64),
	add constraint cs_unique_conversation UNIQUE (initial_context, owner)
;

-- If there is a tripplan, than it is the initial context
UPDATE conversation SET initial_context = 
	(SELECT cc.context FROM conversation_context cc 
	 WHERE cc.conversation = conversation.id AND cc.context like '%:tripplan:%') 
WHERE initial_context is null
;

-- If still null and there is a trip, than that is the initial context
UPDATE conversation SET initial_context = 
	(SELECT cc.context FROM conversation_context cc 
	 WHERE cc.conversation = conversation.id AND cc.context like '%:trip:%') 
WHERE initial_context is null
;

-- If still null and there is a ride, than that is the initial context
UPDATE conversation SET initial_context = 
	(SELECT cc.context FROM conversation_context cc 
	 WHERE cc.conversation = conversation.id AND cc.context like '%:ride:%') 
WHERE initial_context is null
;

-- If still null and there is a keycloak user id, than that is the initial context
UPDATE conversation SET initial_context = 
	(SELECT cc.context FROM conversation_context cc 
	 WHERE cc.conversation = conversation.id AND cc.context like '%:kc:user:%') 
WHERE initial_context is null
;

-- If still null and there is a delegation id, than that is the initial context
UPDATE conversation SET initial_context = 
	(SELECT cc.context FROM conversation_context cc 
	 WHERE cc.conversation = conversation.id AND cc.context like '%:delegation:%') 
WHERE initial_context is null
;

SELECT count(*) from conversation c where c.initial_context is null;
select cc.conversation, cc.context from conversation_context cc 
join conversation c on c.id = cc.conversation 
where initial_context is null
order by cc.conversation

-- Finally, make the initial context mandatory
alter table conversation
	alter column initial_context set not null
;

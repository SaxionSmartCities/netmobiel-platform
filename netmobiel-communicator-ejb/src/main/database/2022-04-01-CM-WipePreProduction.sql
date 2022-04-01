-- Communicator: Wipe all dynamic data, keep users

--public.cm_user
TRUNCATE public.envelope;
TRUNCATE public.conversation_context;
TRUNCATE public.conversation;
TRUNCATE public.message;

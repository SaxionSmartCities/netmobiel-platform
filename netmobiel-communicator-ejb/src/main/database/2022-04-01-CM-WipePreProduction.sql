-- Communicator: Wipe all dynamic data, keep users

TRUNCATE public.envelope, public.conversation_context, public.conversation, public.message;

--public.cm_user

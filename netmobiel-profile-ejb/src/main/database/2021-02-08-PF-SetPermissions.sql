ALTER DEFAULT PRIVILEGES IN SCHEMA public 
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO profilesvc;

ALTER DEFAULT PRIVILEGES IN SCHEMA public 
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO profilesvc;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA PUBLIC TO profilesvc;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA PUBLIC TO profilesvc;
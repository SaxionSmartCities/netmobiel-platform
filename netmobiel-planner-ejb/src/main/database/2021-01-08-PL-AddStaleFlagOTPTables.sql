-- Add state flag to OTP tables

ALTER TABLE public.otp_route_stop
    DROP CONSTRAINT otp_route_stop_route_fk;
ALTER TABLE public.otp_route_stop
    ADD CONSTRAINT otp_route_stop_route_fk FOREIGN KEY (route_id) REFERENCES public.otp_route(id) ON DELETE CASCADE;

ALTER TABLE public.otp_route_stop
    DROP CONSTRAINT otp_route_stop_stop_fk;
ALTER TABLE public.otp_route_stop
    ADD CONSTRAINT otp_route_stop_stop_fk FOREIGN KEY (stop_id) REFERENCES public.otp_stop(id) ON DELETE CASCADE;

ALTER TABLE public.otp_transfer
    DROP CONSTRAINT otp_transfer_from_stop_fk;
ALTER TABLE public.otp_transfer
    ADD CONSTRAINT otp_transfer_from_stop_fk FOREIGN KEY (from_stop) REFERENCES public.otp_stop(id) ON DELETE CASCADE;

ALTER TABLE public.otp_transfer
    DROP CONSTRAINT otp_transfer_to_stop_fk;
ALTER TABLE public.otp_transfer
    ADD CONSTRAINT otp_transfer_to_stop_fk FOREIGN KEY (to_stop) REFERENCES public.otp_stop(id) ON DELETE CASCADE;

ALTER TABLE public.otp_stop
	ADD COLUMN stale boolean NOT NULL DEFAULT False;
ALTER TABLE public.otp_route
	ADD COLUMN stale boolean NOT NULL DEFAULT False;
ALTER TABLE public.otp_cluster
	ADD COLUMN stale boolean NOT NULL DEFAULT False;

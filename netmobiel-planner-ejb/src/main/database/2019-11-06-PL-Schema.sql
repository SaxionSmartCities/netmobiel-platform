--
-- PostgreSQL database dump
--

-- Dumped from database version 10.10
-- Dumped by pg_dump version 10.10

-- Started on 2019-11-08 09:12:16

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 220 (class 1259 OID 64252)
-- Name: otp_cluster; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.otp_cluster (
    id character varying(64) NOT NULL,
    gtfs_id character varying(64),
    label character varying(255),
    point public.geometry,
    nr_routes integer,
    nr_stops integer,
    transportation_types integer
);


ALTER TABLE public.otp_cluster OWNER TO planner;

--
-- TOC entry 221 (class 1259 OID 64260)
-- Name: otp_route; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.otp_route (
    id character varying(64) NOT NULL,
    gtfs_id character varying(64),
    long_name character varying(96),
    short_name character varying(32),
    ov_type integer NOT NULL
);


ALTER TABLE public.otp_route OWNER TO planner;

--
-- TOC entry 222 (class 1259 OID 64265)
-- Name: otp_route_stop; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.otp_route_stop (
    route_id character varying(64) NOT NULL,
    stop_id character varying(64) NOT NULL
);


ALTER TABLE public.otp_route_stop OWNER TO planner;

--
-- TOC entry 223 (class 1259 OID 64268)
-- Name: otp_stop; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.otp_stop (
    id character varying(64) NOT NULL,
    gtfs_id character varying(64),
    label character varying(255),
    point public.geometry,
    nr_routes integer,
    platform_code character varying(32),
    transportation_types integer,
    cluster character varying(64)
);


ALTER TABLE public.otp_stop OWNER TO planner;

--
-- TOC entry 224 (class 1259 OID 64276)
-- Name: otp_transfer; Type: TABLE; Schema: public; Owner: planner
--

CREATE TABLE public.otp_transfer (
    from_stop character varying(255) NOT NULL,
    to_stop character varying(255) NOT NULL,
    distance integer NOT NULL
);


ALTER TABLE public.otp_transfer OWNER TO planner;

--
-- TOC entry 4203 (class 2606 OID 64259)
-- Name: otp_cluster otp_cluster_pkey; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_cluster
    ADD CONSTRAINT otp_cluster_pkey PRIMARY KEY (id);


--
-- TOC entry 4207 (class 2606 OID 64264)
-- Name: otp_route otp_route_pkey; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_route
    ADD CONSTRAINT otp_route_pkey PRIMARY KEY (id);


--
-- TOC entry 4211 (class 2606 OID 64275)
-- Name: otp_stop otp_stop_pkey; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_stop
    ADD CONSTRAINT otp_stop_pkey PRIMARY KEY (id);


--
-- TOC entry 4216 (class 2606 OID 64283)
-- Name: otp_transfer otp_transfer_pkey; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_transfer
    ADD CONSTRAINT otp_transfer_pkey PRIMARY KEY (from_stop, to_stop);


--
-- TOC entry 4214 (class 2606 OID 64290)
-- Name: otp_stop uk_bit5j4eh3x2xle0q7jpbt76kr; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_stop
    ADD CONSTRAINT uk_bit5j4eh3x2xle0q7jpbt76kr UNIQUE (gtfs_id);


--
-- TOC entry 4205 (class 2606 OID 64285)
-- Name: otp_cluster uk_d15xrr8q8u6ms0scvhlfqt5sf; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_cluster
    ADD CONSTRAINT uk_d15xrr8q8u6ms0scvhlfqt5sf UNIQUE (gtfs_id);


--
-- TOC entry 4209 (class 2606 OID 64287)
-- Name: otp_route uk_k1kavxrv2bdlluxuhmurdrb0u; Type: CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_route
    ADD CONSTRAINT uk_k1kavxrv2bdlluxuhmurdrb0u UNIQUE (gtfs_id);


--
-- TOC entry 4212 (class 1259 OID 64288)
-- Name: stop_cluster_ix; Type: INDEX; Schema: public; Owner: planner
--

CREATE INDEX stop_cluster_ix ON public.otp_stop USING btree (cluster);


--
-- TOC entry 4218 (class 2606 OID 64296)
-- Name: otp_route_stop otp_route_stop_route_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_route_stop
    ADD CONSTRAINT otp_route_stop_route_fk FOREIGN KEY (route_id) REFERENCES public.otp_route(id);


--
-- TOC entry 4217 (class 2606 OID 64291)
-- Name: otp_route_stop otp_route_stop_stop_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_route_stop
    ADD CONSTRAINT otp_route_stop_stop_fk FOREIGN KEY (stop_id) REFERENCES public.otp_stop(id) ON DELETE CASCADE;


--
-- TOC entry 4219 (class 2606 OID 64301)
-- Name: otp_stop otp_stop_cluster_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_stop
    ADD CONSTRAINT otp_stop_cluster_fk FOREIGN KEY (cluster) REFERENCES public.otp_cluster(id);


--
-- TOC entry 4220 (class 2606 OID 64306)
-- Name: otp_transfer otp_transfer_from_stop_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_transfer
    ADD CONSTRAINT otp_transfer_from_stop_fk FOREIGN KEY (from_stop) REFERENCES public.otp_stop(id);


--
-- TOC entry 4221 (class 2606 OID 64311)
-- Name: otp_transfer otp_transfer_to_stop_fk; Type: FK CONSTRAINT; Schema: public; Owner: planner
--

ALTER TABLE ONLY public.otp_transfer
    ADD CONSTRAINT otp_transfer_to_stop_fk FOREIGN KEY (to_stop) REFERENCES public.otp_stop(id);


-- Completed on 2019-11-08 09:12:17

--
-- PostgreSQL database dump complete
--


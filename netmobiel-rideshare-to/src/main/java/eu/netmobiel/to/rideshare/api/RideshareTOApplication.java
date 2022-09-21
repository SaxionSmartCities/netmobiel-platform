/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.netmobiel.to.rideshare.api;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.slf4j.Logger;

import eu.netmobiel.commons.jaxrs.Jackson2ObjectMapperContextResolver;
import eu.netmobiel.commons.jaxrs.LocalDateParamConverterProvider;
import eu.netmobiel.commons.jaxrs.OffsetDateTimeParamConverterProvider;
import eu.netmobiel.to.rideshare.api.resource.BookingsResource;
import eu.netmobiel.to.rideshare.api.resource.OperatorResource;
import eu.netmobiel.to.rideshare.api.resource.PlanningResource;


/**
 * A class extending {@link Application} and annotated with @ApplicationPath is the Java EE 8 "no XML" approach to activating
 * JAX-RS.
 * <p>
 * <p>
 * Resources are served relative to the servlet path specified in the {@link ApplicationPath} annotation.
 * </p>
 */
@ApplicationPath("/api")
@ApplicationScoped
public class RideshareTOApplication extends Application {
    @Inject
    private Logger log;

    @Inject
    private RideshareTOVersion version;

    @PostConstruct
    public void postConstruct() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n------------------------------------------------");
        builder.append("\nStarting up Netmobiel Rideshare Transport Operator Service");
        builder.append("\n\tVersion:  " + version.getVersionString());
        builder.append("\n\tBuilt On: " + version.getVersionDate());
        builder.append("\n\tBuild:    " + version.getCommitId());
        builder.append("\n------------------------------------------------");
        log.info(builder.toString());
    }

    /**
     * When an REST interface is defined through an API, the scanning does not work. 
     * Lists the classes explicitly.
     */
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new HashSet<>();
        resources.add(BookingsResource.class);
        resources.add(OperatorResource.class);
        resources.add(PlanningResource.class);
        resources.add(Jackson2ObjectMapperContextResolver.class);
        resources.add(LocalDateParamConverterProvider.class);
        resources.add(OffsetDateTimeParamConverterProvider.class);
        // TransportOperator requires a different response.
//        resources.add(WebApplicationExceptionMapper.class);
//        resources.add(EJBExceptionMapper.class);
//        resources.add(SecurityExceptionMapper.class);
//        resources.add(BusinessExceptionMapper.class);
//        resources.add(ProcessingExceptionMapper.class);
//        resources.add(JsonProcessingExceptionMapper.class);
        return resources;
    }

}

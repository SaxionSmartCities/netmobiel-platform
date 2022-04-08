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
package eu.netmobiel.communicator.api;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.slf4j.Logger;

import eu.netmobiel.commons.Version;
import eu.netmobiel.commons.jaxrs.BusinessExceptionMapper;
import eu.netmobiel.commons.jaxrs.EJBExceptionMapper;
import eu.netmobiel.commons.jaxrs.Jackson2ObjectMapperContextResolver;
import eu.netmobiel.commons.jaxrs.JsonProcessingExceptionMapper;
import eu.netmobiel.commons.jaxrs.OffsetDateTimeParamConverterProvider;
import eu.netmobiel.commons.jaxrs.ProcessingExceptionMapper;
import eu.netmobiel.commons.jaxrs.SecurityExceptionMapper;
import eu.netmobiel.commons.jaxrs.WebApplicationExceptionMapper;
import eu.netmobiel.communicator.api.resource.ConversationsResource;
import eu.netmobiel.communicator.api.resource.MessagesResource;
import eu.netmobiel.communicator.api.resource.TestsResource;


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
public class CommunicatorApplication extends Application {
    @Inject
    private Logger log;

    @Inject
    private Version version;

    @PostConstruct
    public void postConstruct() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n------------------------------------------------");
        builder.append("\nStarting up Netmobiel Communicator REST Service");
        builder.append("\n\tVersion:  " + version.getVersionString());
        builder.append("\n\tBuilt On: " + version.getVersionDate().toString());
        builder.append("\n\tBuild:    " + version.getVersionInfo());
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
        resources.add(ConversationsResource.class);
        resources.add(MessagesResource.class);
        resources.add(TestsResource.class);
        resources.add(Jackson2ObjectMapperContextResolver.class);
        resources.add(OffsetDateTimeParamConverterProvider.class);
        resources.add(WebApplicationExceptionMapper.class);
        resources.add(EJBExceptionMapper.class);
        resources.add(SecurityExceptionMapper.class);
        resources.add(BusinessExceptionMapper.class);
        resources.add(ProcessingExceptionMapper.class);
        resources.add(JsonProcessingExceptionMapper.class);
     return resources;
    }

//	@Override
//	public Set<Object> getSingletons() {
//        Set<Object> singletons = new HashSet<>();
//        singletons.add(new LocalDataParamConverterProvider());
//		return singletons;
//	}
    
    
}

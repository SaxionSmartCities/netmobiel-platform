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
package eu.netmobiel.profile.api;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.Application;

import org.jboss.resteasy.plugins.interceptors.CorsFilter;
import org.slf4j.Logger;

import eu.netmobiel.commons.jaxrs.BusinessExceptionMapper;
import eu.netmobiel.commons.jaxrs.EJBExceptionMapper;
import eu.netmobiel.commons.jaxrs.Jackson2ObjectMapperContextResolver;
import eu.netmobiel.commons.jaxrs.JsonProcessingExceptionMapper;
import eu.netmobiel.commons.jaxrs.OffsetDateTimeParamConverterProvider;
import eu.netmobiel.commons.jaxrs.ProcessingExceptionMapper;
import eu.netmobiel.commons.jaxrs.SecurityExceptionMapper;
import eu.netmobiel.commons.jaxrs.WebApplicationExceptionMapper;
import eu.netmobiel.profile.api.resource.ComplimentsResource;
import eu.netmobiel.profile.api.resource.DelegationsResource;
import eu.netmobiel.profile.api.resource.ProfilesResource;
import eu.netmobiel.profile.api.resource.ReviewsResource;
import eu.netmobiel.profile.api.resource.SurveyInteractionsResource;
import eu.netmobiel.profile.api.resource.TestsResource;
import eu.netmobiel.profile.api.resource.VersionResource;


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
public class ProfileServiceApplication extends Application {
    @Inject
    private Logger log;

    @Inject
    private ProfileServiceVersion version;

    private Set<Class<?>> resources = new HashSet<>();
    private Set<Object> singletons = new HashSet<>();
    
    public static class MyCorsFilter extends CorsFilter  {

		@Override
		protected void preflight(String origin, ContainerRequestContext requestContext) throws IOException {
			// TODO Auto-generated method stub
			super.preflight(origin, requestContext);
		}

		@Override
		protected void checkOrigin(ContainerRequestContext requestContext, String origin) {
			// TODO Auto-generated method stub
			super.checkOrigin(requestContext, origin);
		}

		@Override
		public void filter(ContainerRequestContext requestContext) throws IOException {
			// TODO Auto-generated method stub
			super.filter(requestContext);
		}

		@Override
		public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
				throws IOException {
			// TODO Auto-generated method stub
			super.filter(requestContext, responseContext);
		}
    	
    }
    public ProfileServiceApplication() {
        resources.add(ComplimentsResource.class);
        resources.add(DelegationsResource.class);
        resources.add(ProfilesResource.class);
        resources.add(ReviewsResource.class);
        resources.add(SurveyInteractionsResource.class);
        resources.add(TestsResource.class);
        resources.add(VersionResource.class);
        resources.add(OffsetDateTimeParamConverterProvider.class);
        resources.add(WebApplicationExceptionMapper.class);
        resources.add(EJBExceptionMapper.class);
        resources.add(SecurityExceptionMapper.class);
        resources.add(BusinessExceptionMapper.class);
        resources.add(ProcessingExceptionMapper.class);
        resources.add(JsonProcessingExceptionMapper.class);
        resources.add(Jackson2ObjectMapperContextResolver.class);

//        CorsFilter corsFilter = new MyCorsFilter();
//        corsFilter.getAllowedOrigins().add("*");
//        corsFilter.setExposedHeaders("*");
//        singletons.add(corsFilter);
//        singletons.add(new LocalDataParamConverterProvider());

    }
    
    @PostConstruct
    public void postConstruct() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n------------------------------------------------");
        builder.append("\nStarting up Netmobiel Profile Service REST Service");
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
        return resources;
    }

	@Override
	public Set<Object> getSingletons() {
		return singletons;
	}
    
    
}

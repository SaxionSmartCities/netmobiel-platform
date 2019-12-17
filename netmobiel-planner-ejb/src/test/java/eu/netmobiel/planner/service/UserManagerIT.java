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
package eu.netmobiel.planner.service;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import javax.ejb.EJB;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.planner.Resources;
import eu.netmobiel.planner.model.User;
import eu.netmobiel.planner.repository.UserDao;
import eu.netmobiel.planner.util.PlannerUrnHelper;

@RunWith(Arquillian.class)
public class UserManagerIT {
    @Deployment
    public static Archive<?> createTestArchive() {
    	File[] deps = Maven.configureResolver()
				.loadPomFromFile("pom.xml")
				.importCompileAndRuntimeDependencies() 
				.resolve()
				.withTransitivity()
				.asFile();
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "test.war")
                .addAsLibraries(deps)
                .addPackage(PlannerUrnHelper.class.getPackage())
                .addPackages(true, User.class.getPackage())
                .addPackages(true, AbstractDao.class.getPackage())
                .addPackages(true, UserDao.class.getPackage())
//                .addPackage(BookingMappingBuilder.class.getPackage()) // Necessary, otherwise the Resources.class is not accepted (silent error)
            .addClass(UserManager.class)
            .addClass(Resources.class)
            .addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml")
            .addAsResource("import.sql")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
		System.out.println(archive.toString(true));
		return archive;
    }

    @EJB(beanName =  "plannerUserManager")
    private UserManager userManager;

    @Inject
    private Logger log;

    @Test
    public void testListUsers() throws Exception {
        List<User> users = userManager.listUsers();
        assertNotNull(users);
        log.info("List users: #" + users.size());
        users.forEach(u -> log.debug(u.toString()));
        assertEquals(2, users.size());
    }

}

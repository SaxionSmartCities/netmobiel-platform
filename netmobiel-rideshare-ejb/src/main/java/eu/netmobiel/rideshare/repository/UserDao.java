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
package eu.netmobiel.rideshare.repository;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import eu.netmobiel.rideshare.model.User;

@ApplicationScoped
@Typed(UserDao.class)
public class UserDao extends AbstractDao<User, Long> {

	@Inject
    private EntityManager em;

    public UserDao() {
		super(User.class);
	}

    public Optional<User> findByManagedIdentity(String managedId) {
    	List<User> users = em.createQuery("from User where managedIdentity = :identity", User.class)
    			.setParameter("identity", managedId)
    			.getResultList();
    	return Optional.ofNullable(users.size() > 0 ? users.get(0) : null); 
    }

}

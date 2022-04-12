/*
 * Copyright 2017 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.netmobiel.geoservice.api;

import javax.enterprise.context.ApplicationScoped;

import eu.netmobiel.commons.Version;

/**
 * Each REST service has its own version object. Not really necessary in the current situation, but it prepared for a future split-up.
 * 
 * @author Jaap Reitsma
 *
 */
@ApplicationScoped
public class GeoVersion extends Version {

	public GeoVersion() {
		super(GeoVersion.class.getClassLoader(), "eu/netmobiel/geoservice/version.properties");
	}

}

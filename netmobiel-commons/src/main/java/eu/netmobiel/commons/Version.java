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

package eu.netmobiel.commons;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * Each REST service has its own version object. Not really necesasary in the current situatoin, but it prepared for a future split-up.
 * 
 * @author Jaap Reitsma
 *
 */
public class Version {

    @Inject
    private Logger log;

    private Properties properties;

    private String versionResourcePath = "version.properties";
    private ClassLoader classLoader;

    public Version() {
    	
    }
    
    public Version(ClassLoader classLoader, String versionResourcePath) {
    	this.classLoader = classLoader;
    	this.versionResourcePath = versionResourcePath;
    }

    @PostConstruct
    public void load() {
    	ClassLoader loader = this.classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
        this.properties = new Properties();
        try (InputStream input = loader.getResourceAsStream(versionResourcePath)) {
            if (input == null) {
            	log.warn("No version.properties, loading default properties");
                this.properties.setProperty("git.build.version", "<Non-Maven Build>");
                this.properties.setProperty("git.build.time", LocalDateTime.now().toString());
                this.properties.setProperty("git.commit.id", "<Non-Git Build>");
            } else {
            	log.debug("Loading version properties");
                this.properties.load(input);
            }
        } catch (IOException e) {
            log.error("Error loading version information.", e);
        }
    }

    /**
     * @return the maven version
     */
    public String getVersionString() {
        return this.properties.getProperty("git.build.version");
    }

    /**
     * @return the git build time
     */
    public String getVersionDate() {
        return this.properties.getProperty("git.build.time");
    }
    
    /**
     * @return the composite "full" version info
     */
    public String getCommitId() {
        return properties.getProperty("git.commit.id");
    }
    
    /**
     * Gets a version property value.
     * @param propertyName
     */
    public String getVersionProperty(String propertyName) {
        return this.properties.getProperty(propertyName);
    }

}

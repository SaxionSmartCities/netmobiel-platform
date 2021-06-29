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
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class Version {

    @Inject
    private Logger log;

    private Properties properties;
    
    @PostConstruct
    public void load() {
        this.properties = new Properties();
        try (InputStream input = Version.class.getResourceAsStream("version.properties")) {
            if (input == null) {
                this.properties.setProperty("version", "Unknown");
                this.properties.setProperty("date", LocalDateTime.now().toString());
            } else {
                this.properties.load(input);
            }
        } catch (IOException e) {
            log.error("Error loading version information.", e);
        }
    }

    /**
     * @return the versionString
     */
    public String getVersionString() {
        return this.properties.getProperty("version", "Unknown");
    }

    /**
     * @return the versionDate
     */
    public LocalDateTime getVersionDate() {
        String vds = this.properties.getProperty("date");
        try {
            if (vds == null) {
                return LocalDateTime.now();
            }
            return LocalDateTime.parse(vds);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
    
    /**
     * @return the composite "full" version info
     */
    public String getVersionInfo() {
        return properties.getProperty("git.commit.id.describe", "<Non-Git Build>");
    }
    
    /**
     * Gets a version property value.
     * @param propertyName
     */
    public String getVersionProperty(String propertyName) {
        return this.properties.getProperty(propertyName);
    }

}

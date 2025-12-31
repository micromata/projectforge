/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.config;

import org.projectforge.Constants;
import org.projectforge.framework.configuration.PFSpringConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebApplicationConfig implements WebMvcConfigurer {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebApplicationConfig.class);

    @Autowired
    private PFSpringConfiguration pfSpringConfiguration;

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/" + Constants.REACT_APP_PATH + "**").setViewName("forward:/react-app.html");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
       if (pfSpringConfiguration.getCorsFilterEnabled()) {
           PFSpringConfiguration.logCorsFilterWarning(log);
            // Allow maximum access for development on localhost
            registry.addMapping("/**")
                    // '*' doesn't work for modern browsers, use 'http://localhost:3000' instead:
                    .allowedOrigins(pfSpringConfiguration.getCorsAllowedOrigins())
                    .allowedMethods("*")  // Allow all HTTP methods (GET, POST, PUT, DELETE, OPTIONS, etc.)
                    .allowedHeaders("*")  // Allow all headers
                    .allowCredentials(true)  // Allow credentials (cookies, authorization headers)
                    .maxAge(3600);  // Cache the preflight response for 1 hour
        }
    }
}

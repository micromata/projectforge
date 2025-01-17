/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.apache.wicket.protocol.http.WicketFilter;
import org.apache.wicket.spring.SpringWebApplicationFactory;
import org.projectforge.business.user.filter.WicketUserFilter;
import org.projectforge.carddav.CardDavInit;
import org.projectforge.framework.configuration.PFSpringConfiguration;
import org.projectforge.model.rest.RestPaths;
import org.projectforge.rest.config.LocaleFilter;
import org.projectforge.rest.config.Rest;
import org.projectforge.rest.config.RestUtils;
import org.projectforge.security.LoggingFilter;
import org.projectforge.security.SecurityHeaderFilter;
import org.projectforge.web.OrphanedLinkFilter;
import org.projectforge.web.filter.ResponseHeaderFilter;
import org.projectforge.web.filter.SpringThreadLocalFilter;
import org.projectforge.web.rest.RestCalendarSubscriptionUserFilter;
import org.projectforge.web.rest.RestUserFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Configuration;

/**
 * This class is the replacement of the web.xml. It registers the wicket filter in the spring aware configuration style.
 *
 * @author Florian Blumenstein
 */
@Configuration
public class WebXMLInitializer implements ServletContextInitializer {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebXMLInitializer.class);

    @Value("${projectforge.security.csp-header-value:}") // defaults to empty string
    private String cspHeaderValue;

    private static final String PARAM_APP_BEAN = "applicationBean";

    @Autowired
    private CardDavInit cardDavInit;

    @Autowired
    private PFSpringConfiguration pfSpringConfiguration;

    @Override
    public void onStartup(ServletContext sc) throws ServletException {
        final FilterRegistration securityHeaderFilter = sc.addFilter("SecurityHeaderFilter", SecurityHeaderFilter.class);
        securityHeaderFilter.addMappingForUrlPatterns(null, false, "/*");
        securityHeaderFilter.setInitParameter(SecurityHeaderFilter.PARAM_CSP_HEADER_VALUE, cspHeaderValue);

        if (pfSpringConfiguration.getCorsFilterEnabled()) {
            log.warn("************* Enabling CorsPreflightFilter for development. *************");
            FilterRegistration.Dynamic corsPreflightFilter = sc.addFilter("CorsPreflightFilter", CorsPreflightFilter.class);
            corsPreflightFilter.addMappingForUrlPatterns(null, false, "/*");
        }

        /*
         * Redirect orphaned links from former versions of ProjectForge (e. g. if link in e-mails were changed due to migrations or refactoring.
         */
        sc.addFilter("redirectOrphanedLinks", new OrphanedLinkFilter()).addMappingForUrlPatterns(null, false, "/*");

        cardDavInit.init(sc);

        boolean filterAfterInternal = false;
        RestUtils.registerFilter(sc, "loggingFilter", LoggingFilter.class, false, "/*");
        RestUtils.registerFilter(sc, "UserFilter", WicketUserFilter.class, filterAfterInternal, "/wa/*");
        RestUtils.registerFilter(sc, "springContext", SpringThreadLocalFilter.class, filterAfterInternal, "/wa/*");

        final FilterRegistration wicketApp = RestUtils.registerFilter(sc, "wicket.app", WicketFilter.class, filterAfterInternal, "/wa/*");
        wicketApp.setInitParameter(WicketFilter.APP_FACT_PARAM, SpringWebApplicationFactory.class.getName());
        wicketApp.setInitParameter(PARAM_APP_BEAN, "wicketApplication");
        wicketApp.setInitParameter(WicketFilter.FILTER_MAPPING_PARAM, "/wa/*");

        sc.addFilter("locale", new LocaleFilter()).addMappingForUrlPatterns(null, false,
                "/" + RestPaths.REST_PUBLIC + "/*"); // Needed for login service.

        RestUtils.registerFilter(sc, "restUserFilter", RestUserFilter.class, false,
                "/" + RestPaths.REST + "/*");
        RestUtils.registerFilter(sc, "calendarSubscriptionFilter", RestCalendarSubscriptionUserFilter.class, false, Rest.CALENDAR_EXPORT_BASE_URI);

        final FilterRegistration expire = sc.addFilter("expire", ResponseHeaderFilter.class);
        expire.setInitParameter("Cache-Control", "public, max-age=7200");
        expire.addMappingForUrlPatterns(null, false, "*.css");
        expire.addMappingForUrlPatterns(null, false, "*.gif");
        expire.addMappingForUrlPatterns(null, false, "*.gspt");
        expire.addMappingForUrlPatterns(null, false, "*.jpg");
        expire.addMappingForUrlPatterns(null, false, "*.js");
        expire.addMappingForUrlPatterns(null, false, "*.png");
        expire.addMappingForUrlPatterns(null, false, "*.swf");
    }
}

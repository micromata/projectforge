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
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class WebApplicationConfig implements WebMvcConfigurer {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebApplicationConfig.class);

    @Autowired
    private PFSpringConfiguration pfSpringConfiguration;

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/" + Constants.REACT_APP_PATH + "**").setViewName("forward:/react-app.html");
        // Root of the Next.js app: an empty resource path isn't resolved by the resource handler below, so map the
        // bare base path explicitly. These patterns match exactly and therefore don't interfere with asset requests.
        registry.addViewController("/" + Constants.NEXT).setViewName("redirect:/" + Constants.NEXT_APP_PATH);
        registry.addViewController("/" + Constants.NEXT_APP_PATH).setViewName("forward:/" + Constants.NEXT_APP_PATH + "index.html");
    }

    /**
     * projectforge-next (Next.js static export) is served side-by-side with the legacy React app during the migration.
     * <p>
     * Unlike the legacy React app (whose assets live at the root, so a plain view-controller forward suffices), the
     * Next.js export places its assets under the base path ({@code /next/_next/**}). A naive forward of {@code /next/**}
     * to the SPA shell would therefore swallow asset requests. This resource handler instead serves real files first
     * (assets, per-route {@code index.html}) and only falls back to the SPA shell ({@code 404.html}) for page routes
     * that have no own file (deep links such as {@code /next/books/5}). Missing assets still yield a real 404.
     * <p>
     * The export is packaged into {@code classpath:/static/next/} (see projectforge-next Gradle build).
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/" + Constants.NEXT_APP_PATH + "**")
                .addResourceLocations("classpath:/static/" + Constants.NEXT_APP_PATH)
                .resourceChain(true)
                .addResolver(new NextSpaResourceResolver());
    }

    /**
     * Resolves a request under {@code /next/**} to a static file, applying Next.js static-export conventions
     * (directory {@code index.html}, {@code <route>.html}) and falling back to the SPA shell for page routes.
     */
    private static class NextSpaResourceResolver extends PathResourceResolver {
        private static final String SPA_SHELL = "404.html";

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws java.io.IOException {
            // 1. Exact file (assets like _next/static/*.js, favicon.ico, and <route>/index.html when path ends in "/").
            Resource resource = tryResource(resourcePath, location);
            if (resource != null) return resource;
            if (resourcePath.isEmpty() || resourcePath.endsWith("/")) {
                resource = tryResource(resourcePath + "index.html", location);
                if (resource != null) return resource;
            } else {
                // 2. Page route without trailing slash: <route>/index.html or <route>.html.
                resource = tryResource(resourcePath + "/index.html", location);
                if (resource == null) resource = tryResource(resourcePath + ".html", location);
                if (resource != null) return resource;
            }
            // 3. Asset request (has a non-.html extension) that wasn't found → real 404, do not mask with HTML.
            int lastSlash = resourcePath.lastIndexOf('/');
            String lastSegment = lastSlash >= 0 ? resourcePath.substring(lastSlash + 1) : resourcePath;
            if (lastSegment.contains(".") && !lastSegment.endsWith(".html")) {
                return null;
            }
            // 4. Page route (deep link) with no own file → serve the SPA shell for client-side routing.
            return tryResource(SPA_SHELL, location);
        }

        private Resource tryResource(String resourcePath, Resource location) throws java.io.IOException {
            Resource resource = location.createRelative(resourcePath);
            return (resource.isReadable()) ? resource : null;
        }
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

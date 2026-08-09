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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
     * (assets, per-route {@code index.html}) and only then falls back to the shell of the matching dynamic route for
     * deep links such as {@code /next/books/5}. Missing assets still yield a real 404.
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
     * (directory {@code index.html}, {@code <route>.html}) and mapping deep links of a dynamic route onto the one
     * route Next prerendered for it.
     * <p>
     * A static export has no file for {@code /next/books/25219084}: {@code books/[id]} is prerendered exactly once,
     * from the placeholder of its {@code generateStaticParams} ({@code books/new}). That prerender — not
     * {@code 404.html}, which is Next's own not-found page and renders as such wherever it is served — is the shell a
     * deep link has to be answered with, because the HTML carries the route's page component. Which shell belongs to
     * which route pattern is read from {@code next-spa-shell-map.json}, written by the Next build
     * (projectforge-next/scripts/generate-spa-shell-map.mjs), so a new dynamic route needs no change here.
     * <p>
     * Substituting the whole directory rather than just the HTML also serves the route's RSC payloads
     * ({@code books/25219084/__next._tree.txt} → {@code books/new/__next._tree.txt}), which is what makes
     * client-side navigation to a deep link work.
     */
    private static class NextSpaResourceResolver extends PathResourceResolver {
        private static final String SHELL_MAP = "next-spa-shell-map.json";

        /** Lazily loaded: the location isn't known before the first request. */
        private volatile List<DynamicRoute> dynamicRoutes;

        private record DynamicRoute(String page, Pattern pattern, String shellDir) {
        }

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
            // 3. Deep link of a dynamic route → the shell Next prerendered for that route.
            return tryShell(resourcePath, location, isAssetRequest(resourcePath));
        }

        /** Whether the last segment carries a non-.html extension, i.e. a file was asked for, not a page. */
        private static boolean isAssetRequest(String resourcePath) {
            int lastSlash = resourcePath.lastIndexOf('/');
            String lastSegment = lastSlash >= 0 ? resourcePath.substring(lastSlash + 1) : resourcePath;
            return lastSegment.contains(".") && !lastSegment.endsWith(".html");
        }

        /**
         * Replaces the part of {@code resourcePath} that identifies a dynamic route with that route's shell directory.
         * <p>
         * Three shapes occur, all of which the client router needs:
         * <ol>
         * <li>the page itself, {@code /books/5} → {@code books/new/index.html};</li>
         * <li>the RSC payload of a client side navigation, {@code /books/5.txt} → {@code books/new/index.txt} — the
         * route with an extension appended, not a file below it;</li>
         * <li>a file below the page, {@code /books/5/__next._tree.txt} → {@code books/new/__next._tree.txt}.</li>
         * </ol>
         *
         * @param assetRequest whether a file was asked for rather than a page. For those only an exact hit counts: an
         *                     unresolvable asset has to stay a 404 rather than be masked with HTML.
         */
        private Resource tryShell(String resourcePath, Resource location, boolean assetRequest) throws java.io.IOException {
            String path = "/" + (resourcePath.endsWith("/") ? resourcePath.substring(0, resourcePath.length() - 1) : resourcePath);
            if (!assetRequest) {
                String shellDir = matchShellDir(path, location);
                return shellDir == null ? null : tryResource(shellDir + "/index.html", location);
            }
            // 3. before 2.: a file below the page, matching the route against the path without the file name. Tried
            // first because stripping the extension instead (below) can turn such a path into a match of a *different*
            // route — "/books/5/__next._tree.txt" without ".txt" looks like "/[category]/[type]/[...params]".
            int lastSlash = path.lastIndexOf('/');
            String shellDir = matchShellDir(path.substring(0, lastSlash), location);
            if (shellDir != null) {
                Resource resource = tryResource(shellDir + path.substring(lastSlash), location);
                if (resource != null) return resource;
            }
            // 2. <route>.<ext>: the payload of the page, which the export stores as the page's own index.<ext>.
            int dot = path.lastIndexOf('.');
            shellDir = matchShellDir(path.substring(0, dot), location);
            return shellDir == null ? null : tryResource(shellDir + "/index" + path.substring(dot), location);
        }

        /** The shell directory of the first dynamic route matching {@code path}, or null. */
        private String matchShellDir(String path, Resource location) {
            // The map keeps Next's own order, which is most specific first — so the first match is the right one.
            for (DynamicRoute route : getDynamicRoutes(location)) {
                if (route.pattern().matcher(path).matches()) return route.shellDir();
            }
            return null;
        }

        private List<DynamicRoute> getDynamicRoutes(Resource location) {
            List<DynamicRoute> routes = dynamicRoutes;
            if (routes == null) {
                synchronized (this) {
                    routes = dynamicRoutes;
                    if (routes == null) {
                        dynamicRoutes = routes = loadDynamicRoutes(location);
                    }
                }
            }
            return routes;
        }

        private static List<DynamicRoute> loadDynamicRoutes(Resource location) {
            List<DynamicRoute> routes = new ArrayList<>();
            try {
                Resource resource = location.createRelative(SHELL_MAP);
                if (!resource.isReadable()) {
                    log.warn("{} not found in the Next.js export: deep links such as /{}books/5 will not work. Rebuild projectforge-next.",
                            SHELL_MAP, Constants.NEXT_APP_PATH);
                    return routes;
                }
                try (java.io.InputStream in = resource.getInputStream()) {
                    for (com.fasterxml.jackson.databind.JsonNode node : new com.fasterxml.jackson.databind.ObjectMapper().readTree(in).path("routes")) {
                        routes.add(new DynamicRoute(node.path("page").asText(),
                                Pattern.compile(node.path("regex").asText()),
                                node.path("shellDir").asText()));
                    }
                }
                log.info("Next.js dynamic routes served from their prerendered shell: {}",
                        routes.stream().map(route -> route.page() + " -> " + route.shellDir()).toList());
            } catch (Exception ex) {
                log.error("Can't read " + SHELL_MAP + " of the Next.js export: deep links will not work: " + ex.getMessage(), ex);
            }
            return routes;
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

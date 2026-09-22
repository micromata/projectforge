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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The four cases {@link WebApplicationConfig.NextSpaResourceResolver} resolves a request under
 * {@code /next/**} with, and above all the order they are tried in.
 * <p>
 * That order is the whole correctness argument, and getting it wrong is close to invisible in the
 * browser: every case answers with <em>some</em> readable file, so a mismatch shows up not as a 404
 * but as the wrong page — which is how {@code /next/book.txt} came to be served with the payload of
 * the catch-all {@code /[category]} shell, letting a client side navigation to the books list render
 * Next's own not-found page while a reload of the same url worked.
 * <p>
 * Against a fixture tree rather than the real export: the export exists only after a Next build, and
 * the cases are about path shapes, not about that build's content.
 */
class NextSpaResourceResolverTest {
    /** As {@code next-spa-shell-map.json} of a build has it: most specific first, catch-all last. */
    private static final String SHELL_MAP = """
            {"routes": [
              {"page": "/book/[id]", "regex": "^/book/([^/]+?)(?:/)?$", "shellDir": "book/new"},
              {"page": "/book/[id]/history", "regex": "^/book/([^/]+?)/history(?:/)?$", "shellDir": "book/new/history"},
              {"page": "/[category]", "regex": "^/([^/]+?)(?:/)?$", "shellDir": "address"},
              {"page": "/[category]/[type]/[...params]", "regex": "^/([^/]+?)/([^/]+?)/(.+?)(?:/)?$", "shellDir": "address/edit/new"}
            ]}
            """;

    private Resource location;

    private WebApplicationConfig.NextSpaResourceResolver resolver;

    @BeforeEach
    void writeExport(@TempDir Path dir) throws IOException {
        write(dir, "next-spa-shell-map.json", SHELL_MAP);
        // A static route with a concrete prerender of its own, as the migrated list pages have.
        write(dir, "book/index.html", "book page");
        write(dir, "book/index.txt", "book payload");
        // The shell of book/[id], from the placeholder of its generateStaticParams.
        write(dir, "book/new/index.html", "book edit shell");
        write(dir, "book/new/index.txt", "book edit payload");
        write(dir, "book/new/__next._tree.txt", "book edit tree");
        write(dir, "book/new/history/index.html", "book history shell");
        // The shell of the server laid out list, /[category].
        write(dir, "address/index.html", "dynamic list shell");
        write(dir, "address/index.txt", "dynamic list payload");
        write(dir, "address/edit/new/index.html", "dynamic edit shell");
        write(dir, "_next/static/chunks/main.js", "asset");
        write(dir, "index.html", "start page");
        // A trailing slash, as Spring's resource location has one: createRelative resolves against
        // the directory then, not against its parent.
        location = new FileSystemResource(dir.toString() + "/");
        resolver = new WebApplicationConfig.NextSpaResourceResolver();
    }

    private static void write(Path dir, String path, String content) throws IOException {
        Path file = dir.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    @Test
    void servesAnExistingFileAsItIs() throws IOException {
        assertEquals("asset", resolve("_next/static/chunks/main.js"));
        assertEquals("start page", resolve(""));
        assertEquals("start page", resolve("/"));
    }

    @Test
    void servesAStaticRouteFromItsOwnPrerender() throws IOException {
        assertEquals("book page", resolve("book"));
        assertEquals("book page", resolve("book/"));
    }

    /**
     * The case the bug was in: {@code /book} matches the catch-all {@code /[category]} once the
     * extension is stripped, so the route's own payload has to be looked up before any shell.
     */
    @Test
    void servesThePayloadOfAStaticRouteFromItsOwnPrerender() throws IOException {
        assertEquals("book payload", resolve("book.txt"));
    }

    @Test
    void servesADeepLinkFromTheShellOfItsRoute() throws IOException {
        assertEquals("book edit shell", resolve("book/25219084"));
        assertEquals("book history shell", resolve("book/25219084/history"));
        // No prerender of its own, so the catch-all's shell answers - which reads the category from
        // the url at runtime.
        assertEquals("dynamic list shell", resolve("vacation"));
        assertEquals("dynamic edit shell", resolve("vacation/edit/17"));
    }

    @Test
    void servesThePayloadsADeepLinksClientSideNavigationNeeds() throws IOException {
        // The route with an extension appended, i.e. the shell's own index file.
        assertEquals("book edit payload", resolve("book/25219084.txt"));
        // A file below the page, which must not be resolved by stripping the extension: without the
        // ".txt" the path looks like a match of /[category]/[type]/[...params].
        assertEquals("book edit tree", resolve("book/25219084/__next._tree.txt"));
        assertEquals("dynamic list payload", resolve("vacation.txt"));
    }

    /** An unresolvable asset stays a 404 rather than being masked with a page's HTML. */
    @Test
    void leavesAMissingAssetUnresolved() throws IOException {
        assertNull(resolver.getResource("_next/static/chunks/gone.js", location));
        assertNull(resolver.getResource("book/25219084/gone.txt", location));
        assertNull(resolver.getResource("favicon.ico", location));
    }

    /** @return The content of the file the resolver answers with, or null if it answers with none. */
    private String resolve(String resourcePath) throws IOException {
        Resource resource = resolver.getResource(resourcePath, location);
        return resource == null ? null : resource.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }
}

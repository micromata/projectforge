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

package org.projectforge.start;

import org.projectforge.plugins.core.AbstractPlugin;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Describes the plugins found on the class path for the startup banner: version, build date and git id, as written
 * into the jar by the build of the plugin ({@code META-INF/projectforge/plugin-build-<id>.properties}). The build date
 * is the commit time (not the wall clock), so that the build of a plugin stays reproducible.
 */
final class PluginBuildInfo {
    private static final Pattern JAR_NAME = Pattern.compile("^projectforge-plugin-(.+?)-(\\d.*)\\.jar$");

    private PluginBuildInfo() {
    }

    /**
     * One line per plugin on the class path. Never throws, a log line must not break the start.
     */
    static List<String> describePlugins() {
        final List<String> lines = new ArrayList<>();
        try {
            // Providers only: no plugin is instantiated this early.
            for (final ServiceLoader.Provider<AbstractPlugin> provider : ServiceLoader.load(AbstractPlugin.class).stream().toList()) {
                try {
                    lines.add(describe(provider.type()));
                } catch (Exception ex) {
                    lines.add("Plugin " + provider.type().getName() + ": no build info (" + ex + ")");
                }
            }
        } catch (Throwable ex) {
            lines.add("Plugins: not readable (" + ex + ")");
        }
        return lines;
    }

    private static String describe(final Class<?> type) throws Exception {
        final String jarName = jarName(type);
        String id = null;
        String version = null;
        if (jarName != null) {
            final Matcher matcher = JAR_NAME.matcher(jarName);
            if (matcher.matches()) {
                id = matcher.group(1);
                version = matcher.group(2);
            }
        }
        if (id == null) {
            return "Plugin " + type.getSimpleName() + ": " + (jarName != null ? jarName : "classes") + ", no build info";
        }
        final Properties props = new Properties();
        try (InputStream in = type.getClassLoader().getResourceAsStream("META-INF/projectforge/plugin-build-" + id + ".properties")) {
            if (in != null) {
                props.load(in);
            }
        }
        return format(id, version, props);
    }

    /**
     * @param props The content of {@code plugin-build-<id>.properties}, empty if the jar has none (older plugin jar).
     */
    static String format(final String id, final String version, final Properties props) {
        if (props.isEmpty()) {
            return "Plugin " + id + " " + version + ": no build info";
        }
        final String dirty = "true".equals(props.getProperty("git.dirty")) ? "*" : "";
        return "Plugin " + id + " " + props.getProperty("plugin.version", version)
                + ": build=" + props.getProperty("git.commit.time", "?")
                + ", git=" + props.getProperty("git.branch", "?") + "@" + props.getProperty("git.commit.id.abbrev", "?") + dirty;
    }

    /**
     * The name of the jar of the class, e.g. {@code projectforge-plugin-cveradar-9.2.16.jar} (also nested in a boot
     * jar: {@code ...!/BOOT-INF/lib/<name>.jar!/}); null for a directory.
     */
    static String jarName(final Class<?> type) {
        final java.security.CodeSource source = type.getProtectionDomain().getCodeSource();
        if (source == null || source.getLocation() == null) {
            return null;
        }
        final URL location = source.getLocation();
        String path = location.getPath();
        while (path.endsWith("/") || path.endsWith("!")) {
            path = path.substring(0, path.length() - 1);
        }
        final String name = path.substring(path.lastIndexOf('/') + 1);
        return name.endsWith(".jar") ? name : null;
    }
}

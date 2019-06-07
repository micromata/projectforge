/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.jpa.impl;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.projectforge.plugins.core.ProjectforgePluginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.micromata.genome.jpa.impl.JpaExtScannerUrlProvider;
import de.micromata.genome.util.matcher.CommonMatchers;
import de.micromata.genome.util.matcher.Matcher;
import de.micromata.genome.util.matcher.StringMatchers;

/**
 * URLs to plugins.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class JpaPfJpaPluginScannerUrlProvider implements JpaExtScannerUrlProvider
{
  private static final Logger LOG = LoggerFactory.getLogger(JpaPfJpaPluginScannerUrlProvider.class);

  @Override
  public Collection<URL> getScannUrls()
  {
    Map<String, URL> ret = new HashMap<>();
    Matcher<String> urlMatcher = CommonMatchers.or(
        StringMatchers.containsString("/target/"),
        StringMatchers.containsString("/plugins/"),
        StringMatchers.containsString("org.projectforge"));
    ServiceLoader<ProjectforgePluginService> ls = ServiceLoader.load(ProjectforgePluginService.class);
    for (ProjectforgePluginService ps : ls) {
      Class<? extends ProjectforgePluginService> clazz = ps.getClass();
      ClassLoader cls = ps.getClass().getClassLoader();
      if (cls instanceof URLClassLoader) {
        URLClassLoader urlcls = (URLClassLoader) cls;
        URL[] urls = urlcls.getURLs();
        for (URL url : urls) {
          if (urlMatcher.match(url.toString()) == true) {
            ret.put(url.toString(), url);
          }
        }
      }
    }
    return ret.values();
  }

}

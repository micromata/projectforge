package org.projectforge.framework.persistence.jpa.impl;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.log4j.Logger;
import org.projectforge.plugins.core.ProjectforgePluginService;

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
  private static final Logger LOG = Logger.getLogger(JpaPfJpaPluginScannerUrlProvider.class);

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

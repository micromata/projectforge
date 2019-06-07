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

package org.projectforge.framework.i18n;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.projectforge.Const;
import org.projectforge.business.configuration.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.micromata.genome.util.types.Pair;

@Service
public class I18nServiceImpl implements I18nService
{
  @Autowired
  private ConfigurationService configurationService;

  private final Set<String> resourceBundles = new HashSet<>();

  private Map<Pair<Locale, String>, ResourceBundle> localeResourceBundleMap;

  @PostConstruct
  public void init()
  {
    I18nHelper.setI18nService(this);
    loadResourceBundles();
  }

  @Override
  public void loadResourceBundles()
  {
    if (localeResourceBundleMap == null) {
      localeResourceBundleMap = new HashMap<>();
    }
    File file = new File(configurationService.getResourceDir());
    URL[] urls = new URL[0];
    try {
      urls = new URL[] { file.toURI().toURL() };
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }

    for (File iterFile : file.listFiles()) {
      if (iterFile.isFile() && iterFile.getName().matches(".*i18n(_.*)?.properties")) {
        String[] strings = iterFile.getName().split("(_.*)?.prop");
        resourceBundles.add(strings[strings.length - 2]);
      }
    }

    ClassLoader loader = new URLClassLoader(urls);
    for (Locale locale : Const.I18NSERVICE_LANGUAGES) {
      for (String bundleName : resourceBundles) {
        try {
          if (new File(configurationService.getResourceDir() + File.separator + bundleName + "_" + locale.toString() + ".properties").exists()) {
            localeResourceBundleMap.put(new Pair<>(locale, bundleName), ResourceBundle.getBundle(bundleName, locale, loader));
          } else {
            ResourceBundle defaultBundle = ResourceBundle.getBundle(bundleName, Locale.ROOT,
                new URLClassLoader(new URL[] { new File(configurationService.getResourceDir()).toURI().toURL() }));
            localeResourceBundleMap.put(new Pair<>(locale, bundleName), defaultBundle);
          }
        } catch (MissingResourceException ignored) {

        } catch (MalformedURLException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  @Override
  public String getLocalizedStringForKey(String i18nKey, Locale locale)
  {
    ResourceBundle resourceBundle = getResourceBundleFor("I18NResources", locale);
    if (resourceBundle != null) {
      return (String) resourceBundle.getObject(i18nKey);
    } else {
      throw new RuntimeException("No ResourceBundle for given locale found" + locale);
    }

  }

  @Override
  public String getAdditionalString(String key, Locale locale)
  {
    return getValueFromBundles(key, locale);
  }

  private String getValueFromBundles(String key, Locale locale)
  {
    for (String resourceBundle : resourceBundles) {
      // the pair searched for
      Pair<Locale, String> localeStringPair = new Pair<>(locale, resourceBundle);
      if (localeResourceBundleMap.keySet().contains(localeStringPair) == false) {
        for (Pair<Locale, String> iterationPair : localeResourceBundleMap.keySet()) {
          if (iterationPair.getKey().equals(Locale.ROOT) == false && locale.toString().startsWith(iterationPair.getKey().toString())) {
            // replace searched for with nearest candidate e.g. for de_de use de
            localeStringPair = iterationPair;
            break;
          }
        }
        // if no candidate was found use default
        if (new Pair<>(locale, resourceBundle).equals(localeStringPair)) {
          localeStringPair = new Pair<>(new Locale(Locale.getDefault().getLanguage()), resourceBundle);
        }
      }

      final ResourceBundle resourceBundleFile = localeResourceBundleMap.get(localeStringPair);
      try {
        return (String) resourceBundleFile.getObject(key);
      } catch (MissingResourceException ignored) {
        // not found
      }
    }
    return null;
  }

  @Override
  public ResourceBundle getResourceBundleFor(String name, Locale locale)
  {
    return localeResourceBundleMap.get(new Pair<>(locale, name));
  }
}

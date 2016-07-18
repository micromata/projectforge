package org.projectforge.web.i18n;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.annotation.PostConstruct;

import org.projectforge.Const;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.user.I18nHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.micromata.genome.util.types.Pair;

@Service
public class I18NServiceImpl implements I18NService
{

  @Autowired
  private ConfigurationService configurationService;

  private Map<Pair<Locale, String>, ResourceBundle> localeResourceBundleMap;
  private List<String> resourceBundles;

  @PostConstruct
  public void init()
  {
    I18nHelper.i18NService = this;
    resourceBundles = new ArrayList<>();
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
          if(new File(configurationService.getResourceDir() + File.separator + bundleName + "_" + locale.toString() + ".properties").exists()) {
            localeResourceBundleMap
                .put(new Pair<>(locale, bundleName), ResourceBundle.getBundle(bundleName, locale, loader));
          } else {
            ResourceBundle defaultBundle = ResourceBundle.getBundle(bundleName, Locale.ROOT,
                new URLClassLoader(new URL[] {
                    new File(configurationService.getResourceDir()).toURI().toURL() }));
            localeResourceBundleMap
                .put(new Pair<>(locale, bundleName), defaultBundle);
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
    try {
      for (String resourceBundle : resourceBundles) {
        // the pair searched for
        Pair<Locale, String> localeStringPair = new Pair<>(locale, resourceBundle);
        if (localeResourceBundleMap.keySet().contains(localeStringPair) == false) {
          for (Pair<Locale, String> iterationPair : localeResourceBundleMap.keySet()) {
            if (iterationPair.getKey().equals(Locale.ROOT) ==  false && locale.toString().startsWith(iterationPair.getKey().toString()) == true) {
              // replace searched for with nearest candidate e.g. for de_de use de
              localeStringPair = iterationPair;
              break;
            }
          }
          // if no candidate was found use default
          if (new Pair<>(locale, resourceBundle).equals(localeStringPair) == true) {
            localeStringPair = new Pair<>(new Locale(Locale.getDefault().getLanguage()), resourceBundle);
          }
        }

        ResourceBundle resourceBundleFile = localeResourceBundleMap.get(localeStringPair);
        Object resourceBundleFileObject = resourceBundleFile.getObject(key);
        if (resourceBundleFileObject != null) {
          return (String) resourceBundleFileObject;
        }
      }
    } catch (MissingResourceException ignored) {
      return null;
    }
    return null;
  }

  public ResourceBundle getResourceBundleFor(String name, Locale locale)
  {
    return localeResourceBundleMap.get(new Pair<>(locale, name));
  }
}

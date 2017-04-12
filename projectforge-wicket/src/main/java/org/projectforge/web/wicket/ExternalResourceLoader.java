package org.projectforge.web.wicket;

import java.util.Locale;

import org.apache.wicket.Component;
import org.apache.wicket.resource.loader.IStringResourceLoader;
import org.projectforge.framework.i18n.I18nHelper;

public class ExternalResourceLoader implements IStringResourceLoader
{
  private String findResource(Locale locale, String key)
  {
    return I18nHelper.getI18nService().getAdditionalString(key, locale);
  }

  @Override
  public String loadStringResource(Class<?> clazz, String key, Locale locale, String style, String variation)
  {
    return findResource(locale, key);
  }

  @Override
  public String loadStringResource(Component component, String key, Locale locale, String style, String variation)
  {
    return findResource(locale, key);
  }
}

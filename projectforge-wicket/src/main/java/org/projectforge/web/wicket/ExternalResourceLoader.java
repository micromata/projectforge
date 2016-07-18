package org.projectforge.web.wicket;

import org.apache.wicket.Component;
import org.apache.wicket.resource.loader.IStringResourceLoader;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.web.i18n.I18NService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class ExternalResourceLoader implements IStringResourceLoader
{
  private static final Logger logger = LoggerFactory.getLogger(ExternalResourceLoader.class);

  public ExternalResourceLoader()
  {

  }

  public String loadStringResource(Component component, String key)
  {
    return findResource(component.getLocale(), key);
  }

  public String loadStringResource(Class<?> clazz, String key, Locale locale, String style)
  {
    return findResource(locale, key);
  }

  private String findResource(Locale locale, String key)
  {
    return I18nHelper.i18NService.getAdditionalString(key, locale);
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

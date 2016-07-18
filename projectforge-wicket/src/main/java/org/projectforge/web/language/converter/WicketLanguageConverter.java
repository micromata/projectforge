package org.projectforge.web.language.converter;

import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.business.converter.LanguageConverter;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class WicketLanguageConverter implements IConverter
{
  private static final long serialVersionUID = 2554286471459716772L;

  /**
   * Uses all available locales and compares the string value with the display name (in the given locale).
   * 
   * @param value The string representation.
   * @param locale The user's locale to use the correct translation.
   * @see org.apache.wicket.util.convert.IConverter#convertToObject(java.lang.String, java.util.Locale)
   */
  @Override
  public Object convertToObject(final String value, final Locale locale)
  {
    if (StringUtils.isEmpty(value) == true) {
      return null;
    }
    final String lvalue = value.toLowerCase(locale);
    for (final Locale lc : Locale.getAvailableLocales()) {
      if (LanguageConverter.getLanguageAsString(lc, locale).toLowerCase().equals(lvalue) == true) {
        return lc;
      }
    }
    error();
    return null;
  }

  /**
   * @param value The locale to convert.
   * @param locale The user's locale to use the correct translation.
   * @see org.apache.wicket.util.convert.IConverter#convertToString(java.lang.Object, java.util.Locale)
   * @see Locale#getDisplayCountry(Locale)
   */
  @Override
  public String convertToString(final Object value, final Locale locale)
  {
    if (value == null) {
      return null;
    }
    final Locale language = (Locale) value;
    return LanguageConverter.getLanguageAsString(language, locale);
  }

  protected void error()
  {
  }
}

package org.projectforge.framework.i18n;

import java.util.Arrays;
import java.util.Objects;

/**
 * A i18n key along with its parameters (optional).
 */
public class I18nKeyAndParams
{
  private final String key;

  private final Object[] params;

  public I18nKeyAndParams(final String key, final Object... params)
  {
    this.key = Objects.requireNonNull(key);
    this.params = (params != null) ? params : new Object[0];
  }

  public String getKey()
  {
    return key;
  }

  public Object[] getParams()
  {
    return params;
  }

  @Override
  public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o instanceof I18nKeyAndParams) == false) {
      return false;
    }

    final I18nKeyAndParams other = (I18nKeyAndParams) o;
    return Objects.equals(key, other.key) &&
        Arrays.equals(params, other.params);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(key, params);
  }
}

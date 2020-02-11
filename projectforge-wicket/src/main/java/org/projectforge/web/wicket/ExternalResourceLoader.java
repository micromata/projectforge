/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.wicket;

import org.apache.wicket.Component;
import org.apache.wicket.resource.loader.IStringResourceLoader;
import org.projectforge.framework.i18n.I18nHelper;

import java.util.Locale;

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

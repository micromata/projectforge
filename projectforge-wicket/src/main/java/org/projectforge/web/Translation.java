/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.web;

import java.util.Locale;

import org.projectforge.framework.xstream.XmlField;
import org.projectforge.framework.xstream.XmlObject;

/**
 * For configuring translations e. g. in the config.xml file.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@XmlObject(alias = "translation")
public class Translation
{
  private Locale locale;

  @XmlField(asAttribute = true)
  private String translation;

  public Translation()
  {
  }

  public Translation(final Locale locale, final String translation)
  {
    this.locale = locale;
    this.translation = translation;
  }

  /**
   * en, de, en_US etc.
   */
  public Locale getLocale()
  {
    return locale;
  }

  public String getTranslation()
  {
    return translation;
  }
}

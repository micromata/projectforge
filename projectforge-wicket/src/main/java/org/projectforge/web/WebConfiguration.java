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

import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.web.wicket.WicketApplication;

public class WebConfiguration
{
  /**
   * At application start the flag developmentModus is perhaps not already set.
   * 
   * @return true if the system is configured as development system e. g. in the context.xml, otherwise false.
   */
  public static boolean isDevelopmentMode()
  {
    final Boolean developmentMode = WicketApplication.internalIsDevelopmentMode();
    return developmentMode != null && developmentMode == true;
  }

  /**
   * Experimental and undocumented setting. For now, ProjectForge does only hide the navigation header (with logos and
   * menu) of every top page, if set to true.
   * 
   * @see ConfigXml#isPortletMode()
   */
  public static boolean isPortletMode()
  {
    return ConfigXml.getInstance().isPortletMode();
  }
}

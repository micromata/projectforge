/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

/**
 * TODO: To be done: add alt i18n key to every image.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public enum ImageDef
{
  SMS(DIRS.MAIN + "sms.png"); //

  private String path;

  public String getPath()
  {
    return path;
  }

  private ImageDef(final String path)
  {
    this.path = path;
  }

  private class DIRS
  {
    private static final String MAIN = "images/";
  }
}

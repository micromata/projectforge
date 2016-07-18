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

package org.projectforge.framework.persistence.api;


/**
 * Defines different constants (typical length of string columns) usable by plugins and core package.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class Constants
{
  /**
   * Default length of text fields in the data-base (4,000).
   */
  public static final int LENGTH_TEXT = 4000;

  /**
   * Default length of comment fields in the data-base (4,000).
   */
  public static final int LENGTH_COMMENT = 4000;

  /**
   * Default length of text fields in the data-base (1,000).
   */
  public static final int LENGTH_SUBJECT = 1000;

  /**
   * Default length of title fields in the data-base (1,000).
   */
  public static final int LENGTH_TITLE = 1000;
}

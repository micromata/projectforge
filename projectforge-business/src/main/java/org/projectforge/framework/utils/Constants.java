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

package org.projectforge.framework.utils;

import java.math.BigDecimal;

// TODO Da gab es noch eine andere constants
public class Constants
{
  /** The default length of comment strings in the data base. Used by data base definition and front-end validation. */
  public static final int COMMENT_LENGTH = 4000;

  public static final BigDecimal TEN_BILLION = new BigDecimal("10000000000");

  public static final BigDecimal TEN_BILLION_NEGATIVE = new BigDecimal("-10000000000");
}

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

package org.projectforge.common.props;

/**
 * If the type of a field isn't represented by the Java type it may be defined in more detail by this enum. For example a BigDecimal may
 * represent a currency value.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public enum PropertyType
{
  CURRENCY, DATE, DATE_TIME, DATE_TIME_SECONDS, DATE_TIME_MILLIS, TIME, TIME_SECONDS, TIME_MILLIS, UNSPECIFIED;
}

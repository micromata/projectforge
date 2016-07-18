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

package org.projectforge.web.wicket.bootstrap;

/**
 * Used for defining class attribute value for elements (bootstrap grid sizes).
 * <ul>
 * <li>COL25 is an alias for SPAN3</li>
 * <li>COL33 is an alias for SPAN4</li>
 * <li>COL50 is an alias for SPAN6</li>
 * <li>COL66 is an alias for SPAN8</li>
 * <li>COL75 is an alias for SPAN9</li>
 * <li>COL100 is an alias for SPAN12</li>
 * </ul>
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public enum GridSize
{
  SPAN1(1), SPAN2(2), SPAN3(3), SPAN4(4), SPAN6(6), SPAN8(8), SPAN9(9), SPAN12(12), COL25(SPAN3), COL33(SPAN4), COL50(
      SPAN6), COL66(SPAN8), COL75(
          SPAN9), COL100(SPAN12);

  private final String classAttrValue;

  private final int length;

  public String getClassAttrValue()
  {
    return classAttrValue;
  }

  /**
   * @return the length
   */
  public int getLength()
  {
    return length;
  }

  private GridSize(final int length)
  {
    this.length = length;
    // To maintain fluid widths on desktops & tablets, but stack vertically on smartphones:
    // Replace .col-md-* with .col-sm-*
    //
    // To maintain fluid widths on all devices (no stacking):
    // Replace .col-md-* with .col-xs-*
    this.classAttrValue = "col-sm-" + length;
  }

  private GridSize(final GridSize master)
  {
    this.length = master.length;
    this.classAttrValue = master.classAttrValue;
  }

  public static GridSize fromInt(final int span)
  {
    for (final GridSize gs : values()) {
      if (gs.getLength() == span) {
        return gs;
      }
    }
    throw new IllegalArgumentException("Cannot find GridSize for size: " + span);
  }
}

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

package org.projectforge.export;

public enum SVGColor
{
  BLACK("black"), DARK_BLUE("darkblue"), DARK_RED("darkred"), LIGHT_BLUE("lightblue"), LIGHT_GRAY("lightgray"), NONE("none"), RED("red");

  private String name;

  /**
   * The name defined in SVG.
   */
  public String getName()
  {
    return name;
  }

  SVGColor(final String name)
  {
    this.name = name;
  }
}

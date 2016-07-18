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

package org.projectforge.renderer.custom;

import java.util.Map;

/**
 * @author Sebastian Hardt (s.hardt@micromata.de)
 *
 */
public class FormatterFactory {

  private Map<String, Formatter> formatters;
  
  /**
   * Returns the Formatter mapped to the given type
   * @param type
   * @return
   */
  public Formatter getFormatter(final String type) {
    if(!formatters.containsKey(type)) return null;
    
    return formatters.get(type);
  }

  /**
   * @return the formatters
   */
  public Map<String, Formatter> getFormatters() {
    return formatters;
  }

  /**
   * @param formatters the formatters to set
   */
  public void setFormatters(Map<String, Formatter> formatters) {
    this.formatters = formatters;
  }
  
}

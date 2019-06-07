/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.scripting;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.common.i18n.I18nEnum;


/**
 * 
 * Type of input parameter for script.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 *         <ul>
 *         <li>INTEGER</li>
 *         <li>STRING</li>
 *         <li>DECIMAL</li>
 *         <li>TASK</li>
 *         <li>USER</li>
 *         </ul>
 */
public enum ScriptParameterType implements I18nEnum
{
  INTEGER("integer"), DECIMAL("decimal"), STRING("string"), DATE("date"), TIME_PERIOD("timePeriod"), TASK("task"), USER("user");

  private String key;

  public static ScriptParameterType get(String s)
  {
    if (StringUtils.isEmpty(s) == true) {
      return null;
    }
    for (final ScriptParameterType type : ScriptParameterType.values()) {
      if (s.equals(type.name()) == true) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown ScriptParameterType: '" + s + "'");
  }

  public boolean isIn(ScriptParameterType... types)
  {
    for (ScriptParameterType type : types) {
      if (this == type) {
        return true;
      }
    }
    return false;
  }

  /**
   * The key will be used e. g. for i18n.
   * @return
   */
  public String getKey()
  {
    return key;
  }

  @Override
  public String getI18nKey()
  {
    return "scripting.script.parameterType." + key;
  }

  private ScriptParameterType(String key)
  {
    this.key = key;
  }
}

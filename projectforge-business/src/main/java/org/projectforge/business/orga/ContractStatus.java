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

package org.projectforge.business.orga;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.common.i18n.I18nEnum;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 *         <ul>
 *         <li>IN_PROGRESS - in progress.</li>
 *         <li>COMPLETED - completed / signed.</li>
 *         <li>SUSPENDED - completed / not signed.</li>
 *         <li>ESCALATED - escalated.</li>
 *         </ul>
 */
public enum ContractStatus implements I18nEnum
{
  UNKNOWN("unknown"), IN_PROGRESS("inProgress"), IN_COORDINATION("inCoordination"), COMPLETED("completed"), SIGNED("signed"), SUSPENDED(
      "suspended"), ESCALATED("escalated");

  private String key;

  public static ContractStatus get(final String s)
  {
    if (StringUtils.isEmpty(s) == true) {
      return null;
    }
    if ("IN_PROGRESS".equals(s) == true || "IN_PROGRES".equals(s) == true) {
      return IN_PROGRESS;
    } else if ("COMPLETED".equals(s) == true) {
      return COMPLETED;
    } else if ("SUSPENDED".equals(s) == true) {
      return SUSPENDED;
    } else if ("ESCALATED".equals(s) == true) {
      return ESCALATED;
    } else {
      return UNKNOWN;
    }
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
    return "legalAffaires.contract.status." + key;
  }

  ContractStatus(final String key)
  {
    this.key = key;
  }
}

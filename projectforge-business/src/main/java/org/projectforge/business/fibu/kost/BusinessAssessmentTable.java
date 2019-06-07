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

package org.projectforge.business.fibu.kost;

import java.util.ArrayList;
import java.util.List;

import org.projectforge.framework.utils.LabelValueBean;

/**
 * Stores multiple business assessments in a list. A table with different business assessment columns can be used.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class BusinessAssessmentTable
{
  private final List<LabelValueBean<String, BusinessAssessment>> businessAssessmentList;

  public BusinessAssessmentTable()
  {
    businessAssessmentList = new ArrayList<LabelValueBean<String, BusinessAssessment>>();
  }

  public List<LabelValueBean<String, BusinessAssessment>> getBusinessAssessmentList()
  {
    return businessAssessmentList;
  }

  public void addBusinessAssessment(final String label, final BusinessAssessment businessAssessment)
  {
    businessAssessmentList.add(new LabelValueBean<String, BusinessAssessment>(label, businessAssessment));
  }
}

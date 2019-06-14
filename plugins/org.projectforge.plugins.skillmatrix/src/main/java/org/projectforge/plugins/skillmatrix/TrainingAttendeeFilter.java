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

package org.projectforge.plugins.skillmatrix;

import org.projectforge.framework.persistence.api.BaseSearchFilter;

/**
 * @author Werner Feder (werner.feder@t-online.de)
 * 
 */
public class TrainingAttendeeFilter extends BaseSearchFilter
{
  private static final long serialVersionUID = 1278054558397436842L;

  private Integer attendeeId, trainingId;

  public TrainingAttendeeFilter()
  {
  }

  public TrainingAttendeeFilter(final BaseSearchFilter filter)
  {
    super(filter);
  }
  /**
   * @return the attendeeId
   */
  public Integer getAttendeeId()
  {
    return attendeeId;
  }

  /**
   * @param attendeeId the attendeeId to set
   * @return this for chaining.
   */
  public TrainingAttendeeFilter setAttendeeId(final Integer attendeeId)
  {
    this.attendeeId = attendeeId;
    return this;
  }

  /**
   * @return the trainingId
   */
  public Integer getTrainingId()
  {
    return trainingId;
  }

  /**
   * @param trainingId the trainingId to set
   * @return this for chaining.
   */
  public TrainingAttendeeFilter setTrainingId(final Integer trainingId)
  {
    this.trainingId = trainingId;
    return this;
  }


}

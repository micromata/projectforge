/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.teamcal.event;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.i18n.I18nHelper;

import java.util.Collection;

public class TeamEventAttendeeValidator implements IValidator<Collection<TeamEventAttendeeDO>>
{
  private static final long serialVersionUID = -4704767715429841467L;

  @Override
  public void validate(IValidatable<Collection<TeamEventAttendeeDO>> validatable)
  {
    Collection<TeamEventAttendeeDO> attendeeList = validatable.getValue();
    for (TeamEventAttendeeDO attendee : attendeeList) {
      if (attendee.getUser() == null && attendee.getAddress() == null && attendee.getUrl() != null) {
        boolean isValid = StringHelper.isEmailValid(attendee.getUrl());
        if (isValid == false) {
          error(validatable, I18nHelper.getLocalizedMessage("plugins.teamcal.attendee.email.invalid"));
        }
      }
    }
  }

  private void error(IValidatable<Collection<TeamEventAttendeeDO>> validatable, String message)
  {
    ValidationError error = new ValidationError(message);
    validatable.error(error);
  }

}

package org.projectforge.web.teamcal.event;

import java.util.Collection;

import org.apache.commons.validator.routines.EmailValidator;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.user.I18nHelper;

public class TeamEventAttendeeValidator implements IValidator<Collection<TeamEventAttendeeDO>>
{
  private static final long serialVersionUID = -4704767715429841467L;

  @Override
  public void validate(IValidatable<Collection<TeamEventAttendeeDO>> validatable)
  {
    Collection<TeamEventAttendeeDO> attendeeList = validatable.getValue();
    for (TeamEventAttendeeDO attendee : attendeeList) {
      if (attendee.getUser() == null && attendee.getAddress() == null && attendee.getUrl() != null) {
        EmailValidator validator = EmailValidator.getInstance();
        boolean isValid = validator.isValid(attendee.getUrl());
        if (isValid == false) {
          error(validatable, I18nHelper.getLocalizedString("plugins.teamcal.attendee.email.invalid"));
        }
      }
    }
  }

  private void error(IValidatable<Collection<TeamEventAttendeeDO>> validatable, String errorKey)
  {
    ValidationError error = new ValidationError(errorKey);
    validatable.error(error);
  }

}

package org.projectforge.web.common;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

/**
 * This is a very simple validation: A BIC must be 8 or 11 characters long.
 */
public class BicValidator implements IValidator<String>
{

  @Override
  public void validate(IValidatable<String> validatable)
  {
    final String value = validatable.getValue();

    if (value.length() != 8 && value.length() != 11) {
      validatable.error(
          new ValidationError().addKey("bicvalidator.wronglength")
      );
    }
  }

}

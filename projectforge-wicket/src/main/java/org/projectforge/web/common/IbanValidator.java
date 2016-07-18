package org.projectforge.web.common;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

/**
 * This is a very simple validation: A german IBAN must be 22 characters long.
 */
public class IbanValidator implements IValidator<String>
{

  @Override
  public void validate(IValidatable<String> validatable)
  {
    final String value = validatable.getValue();

    if (value.toUpperCase().startsWith("DE") && value.length() != 22) {
      validatable.error(
          new ValidationError().addKey("ibanvalidator.wronglength.de")
      );
    }
  }

}

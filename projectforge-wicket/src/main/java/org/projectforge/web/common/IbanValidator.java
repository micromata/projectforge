package org.projectforge.web.common;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

/**
 * This is a simple validation: A german IBAN must be 22 characters long.
 */
public class IbanValidator implements IValidator<String>
{
  private static final long serialVersionUID = -4044002686092636415L;

  @Override
  public void validate(IValidatable<String> validatable)
  {
    String value = validatable.getValue();
    //removes all whitespaces and non-visible characters (e.g., tab, \n)
    value = value.replaceAll("\\s+", "");
    if (value.toUpperCase().startsWith("DE") && value.length() != 22) {
      validatable.error(
          new ValidationError().addKey("ibanvalidator.wronglength.de"));
    }
  }

}

package org.projectforge.web.fibu;

import static org.testng.Assert.assertEquals;

import org.apache.wicket.validation.Validatable;
import org.projectforge.web.common.BicValidator;
import org.projectforge.web.common.IbanValidator;
import org.testng.annotations.Test;

public class EmployeeDataTest
{
  @Test
  public void testBicValidator()
  {
    BicValidator bicValidator = new BicValidator();

    Validatable<String> validatable = new Validatable<>("12345678");
    bicValidator.validate(validatable);

    assertEquals(validatable.getErrors().size(), 0);

    validatable = new Validatable<>("123456789AB");
    bicValidator.validate(validatable);

    assertEquals(validatable.getErrors().size(), 0);

    validatable = new Validatable<>("12345");
    bicValidator.validate(validatable);

    assertEquals(validatable.getErrors().size(), 1);

    validatable = new Validatable<>("123456789A");
    bicValidator.validate(validatable);

    assertEquals(validatable.getErrors().size(), 1);

    validatable = new Validatable<>("123456789ABCDEF");
    bicValidator.validate(validatable);

    assertEquals(validatable.getErrors().size(), 1);
  }

  @Test
  public void testIbanValidator()
  {
    IbanValidator ibanValidator = new IbanValidator();

    Validatable<String> myiban = new Validatable<>("MYIBAN");
    ibanValidator.validate(myiban);

    assertEquals(myiban.getErrors().size(), 0);

    myiban = new Validatable<>("DEAAAAAAAAAAAAAAAAAAAA");
    ibanValidator.validate(myiban);

    assertEquals(myiban.getErrors().size(), 0);

    myiban = new Validatable<>("      DE 19 1234 1234 1234 1234 12");
    ibanValidator.validate(myiban);

    assertEquals(myiban.getErrors().size(), 0);

    myiban = new Validatable<>("DEAAAAAAAAAAAAAA");
    ibanValidator.validate(myiban);

    assertEquals(myiban.getErrors().size(), 1);

    myiban = new Validatable<>("DEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    ibanValidator.validate(myiban);

    assertEquals(myiban.getErrors().size(), 1);
  }
}

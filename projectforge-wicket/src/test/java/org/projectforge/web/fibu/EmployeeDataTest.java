/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.fibu;

import org.apache.wicket.validation.Validatable;
import org.junit.jupiter.api.Test;
import org.projectforge.web.common.BicValidator;
import org.projectforge.web.common.IbanValidator;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EmployeeDataTest {
  @Test
  public void testBicValidator() {
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
  public void testIbanValidator() {
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

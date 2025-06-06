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

package org.projectforge.web.common;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.projectforge.business.fibu.IBANUtils;

/**
 * This is a simple validation: A german IBAN must be 22 characters long.
 */
public class IbanValidator implements IValidator<String> {
  private static final long serialVersionUID = -4044002686092636415L;

  @Override
  public void validate(IValidatable<String> validatable) {
    String value = validatable.getValue();
    if (!IBANUtils.validate(value)) {
      validatable.error(
          new ValidationError().addKey("ibanvalidator.wronglength.de"));
    }
  }

}

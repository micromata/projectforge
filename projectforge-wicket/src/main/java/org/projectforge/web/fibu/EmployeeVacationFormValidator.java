/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.web.wicket.components.MinMaxNumberField;

import java.math.BigDecimal;

public class EmployeeVacationFormValidator implements IFormValidator
{
  private static final long serialVersionUID = -8478416041230851983L;

  // Components for form validation.
  private final FormComponent<?>[] dependentFormComponents = new FormComponent[2];

  @Override
  public void validate(final Form<?> form)
  {
    final MinMaxNumberField<BigDecimal> fieldPreviousYear = (MinMaxNumberField<BigDecimal>) dependentFormComponents[0];
    final MinMaxNumberField<BigDecimal> fieldPreviousYearUsed = (MinMaxNumberField<BigDecimal>) dependentFormComponents[1];

    if (fieldPreviousYearUsed.getConvertedInput() == null && fieldPreviousYear.getConvertedInput() == null) {
      return;
    }

    if (fieldPreviousYearUsed.getConvertedInput() != null && fieldPreviousYear.getConvertedInput() == null) {
      form.error(I18nHelper.getLocalizedMessage("vacation.validate.usedButNoPreviousYear"));
      return;
    }

    if (fieldPreviousYearUsed.getConvertedInput() == null && fieldPreviousYear.getConvertedInput() != null) {
      return;
    }

    if (fieldPreviousYear.getConvertedInput().compareTo(fieldPreviousYearUsed.getConvertedInput()) < 0) {
      form.error(I18nHelper.getLocalizedMessage("vacation.validate.usedBiggerThanPreviousYear"));
      return;
    }
  }

  @Override
  public FormComponent<?>[] getDependentFormComponents()
  {
    return dependentFormComponents;
  }

}

package org.projectforge.web.fibu;

import java.math.BigDecimal;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.web.wicket.components.MinMaxNumberField;

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

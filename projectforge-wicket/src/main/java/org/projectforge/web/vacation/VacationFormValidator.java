package org.projectforge.web.vacation;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.business.vacation.model.VacationAttrProperty;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.web.wicket.components.DatePanel;

public class VacationFormValidator implements IFormValidator
{
  private static final long serialVersionUID = -8478416045860851983L;

  // Components for form validation.
  private final FormComponent<?>[] dependentFormComponents = new FormComponent[2];

  private VacationService vacationService;

  private VacationDO data;

  public VacationFormValidator(VacationService vacationService, VacationDO data)
  {
    this.vacationService = vacationService;
    this.data = data;
  }

  @Override
  public void validate(final Form<?> form)
  {
    final DatePanel startDatePanel = (DatePanel) dependentFormComponents[0];
    final DatePanel endDatePanel = (DatePanel) dependentFormComponents[1];

    if (endDatePanel.getConvertedInput().before(startDatePanel.getConvertedInput())) {
      form.error(I18nHelper.getLocalizedMessage("vacation.validate.endbeforestart"));
      return;
    }

    List<VacationDO> vacationListForPeriod = vacationService.getVacationForDate(data.getEmployee(),
        startDatePanel.getConvertedInput(), endDatePanel.getConvertedInput());
    if (vacationListForPeriod != null && data.getPk() != null) {
      vacationListForPeriod = vacationListForPeriod.stream().filter(vac -> vac.getPk().equals(data.getPk()) == false)
          .collect(Collectors.toList());
    }
    if (vacationListForPeriod != null && vacationListForPeriod.size() > 0) {
      form.error(I18nHelper.getLocalizedMessage("vacation.validate.leaveapplicationexists"));
    }

    boolean enoughDaysLeft = true;
    Calendar marchEnd = new GregorianCalendar();
    marchEnd.set(Calendar.MONTH, Calendar.MARCH);
    marchEnd.set(Calendar.DAY_OF_MONTH, 31);

    //Positiv
    BigDecimal vacationDays = new BigDecimal(data.getEmployee().getUrlaubstage());
    BigDecimal vacationDaysFromLastYear = data.getEmployee().getAttribute(
        VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(),
        BigDecimal.class) != null ? data.getEmployee().getAttribute(
            VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(),
            BigDecimal.class) : BigDecimal.ZERO;

    //Negative
    BigDecimal usedVacationDaysWholeYear = vacationService.getUsedVacationdays(data.getEmployee());
    BigDecimal usedVacationDaysFromLastYear = data.getEmployee().getAttribute(
        VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(),
        BigDecimal.class) != null ? data.getEmployee().getAttribute(
            VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(),
            BigDecimal.class) : BigDecimal.ZERO;
    BigDecimal usedVacationDaysWithoutDaysFromLastYear = usedVacationDaysWholeYear
        .subtract(usedVacationDaysFromLastYear);

    //Available
    BigDecimal availableVacationDays = vacationDays.subtract(usedVacationDaysWithoutDaysFromLastYear);
    BigDecimal availableVacationDaysFromLastYear = vacationDaysFromLastYear.subtract(usedVacationDaysFromLastYear);

    //Need
    BigDecimal neededVacationDays = DayHolder.getNumberOfWorkingDays(startDatePanel.getConvertedInput(),
        endDatePanel.getConvertedInput());
    BigDecimal neededVacationDaysBeforeMarch = DayHolder.getNumberOfWorkingDays(startDatePanel.getConvertedInput(),
        marchEnd.getTime());

    //Vacation after March
    if (startDatePanel.getConvertedInput().after(marchEnd.getTime())) {
      if (availableVacationDays.subtract(neededVacationDays).compareTo(BigDecimal.ZERO) < 0) {
        enoughDaysLeft = false;
      }
    }
    //Vacation before March
    if (endDatePanel.getConvertedInput().before(marchEnd.getTime())
        || endDatePanel.getConvertedInput().equals(marchEnd.getTime())) {
      if (availableVacationDays.add(availableVacationDaysFromLastYear).subtract(neededVacationDays)
          .compareTo(BigDecimal.ZERO) < 0) {
        enoughDaysLeft = false;
      }
    }
    //Vacation over March April
    if ((startDatePanel.getConvertedInput().before(marchEnd.getTime())
        || startDatePanel.getConvertedInput().equals(marchEnd.getTime()))
        && endDatePanel.getConvertedInput().after(marchEnd.getTime())) {
      BigDecimal restFromLastYear = availableVacationDaysFromLastYear.subtract(neededVacationDaysBeforeMarch);
      if (restFromLastYear.compareTo(BigDecimal.ZERO) <= 0) {
        if (availableVacationDays.subtract(neededVacationDays).compareTo(BigDecimal.ZERO) < 0) {
          enoughDaysLeft = false;
        }
      } else {
        if (availableVacationDays.subtract(neededVacationDays.subtract(restFromLastYear))
            .compareTo(BigDecimal.ZERO) < 0) {
          enoughDaysLeft = false;
        }
      }
    }

    if (enoughDaysLeft == false) {
      form.error(I18nHelper.getLocalizedMessage("vacation.validate.notEnoughVacationDaysLeft"));
    }
  }

  @Override
  public FormComponent<?>[] getDependentFormComponents()
  {
    return dependentFormComponents;
  }
}

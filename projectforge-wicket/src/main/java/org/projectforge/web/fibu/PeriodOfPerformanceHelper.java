package org.projectforge.web.fibu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.IModel;
import org.projectforge.business.fibu.PeriodOfPerformanceType;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

class PeriodOfPerformanceHelper
{
  private final List<DropDownChoice<PeriodOfPerformanceType>> performanceDropDowns = new ArrayList<>();

  private final List<DatePanel> datePanels = new ArrayList<>();

  private DatePanel fromDatePanel;

  private DatePanel endDatePanel;

  public void onRefreshPositions()
  {
    performanceDropDowns.clear();
    datePanels.clear();
  }

  public void createPeriodOfPerformanceFields(final FieldsetPanel fs, final IModel<Date> periodOfPerformanceBeginModel,
      final IModel<Date> periodOfPerformanceEndModel)
  {
    final BooleanSupplier isAnyPerformanceTypeSeeAboveSelected = () -> performanceDropDowns.stream()
        .map(FormComponent::getRawInput) // need to use getRawInput here instead of getModelObject, because model is not updated at time of validation
        .anyMatch(PeriodOfPerformanceType.SEEABOVE.name()::equals);

    fromDatePanel = new DatePanel(fs.newChildId(), periodOfPerformanceBeginModel, DatePanelSettings.get().withTargetType(java.sql.Date.class),
        isAnyPerformanceTypeSeeAboveSelected);
    fs.add(fromDatePanel);

    fs.add(new DivTextPanel(fs.newChildId(), "-"));

    endDatePanel = new DatePanel(fs.newChildId(), periodOfPerformanceEndModel, DatePanelSettings.get().withTargetType(java.sql.Date.class),
        isAnyPerformanceTypeSeeAboveSelected);
    fs.add(endDatePanel);
  }

  public void createPositionsPeriodOfPerformanceFields(final FieldsetPanel fs, final IModel<PeriodOfPerformanceType> periodOfPerformanceTypeModel,
      final IModel<Date> periodOfPerformanceBeginModel, final IModel<Date> periodOfPerformanceEndModel,
      final Component... additionalComponentsToToggleVisibility)
  {
    final List<Component> componentsToToggleVisibility = new ArrayList<>();

    // drop down
    final LabelValueChoiceRenderer<PeriodOfPerformanceType> performanceTypeRenderer = new LabelValueChoiceRenderer<>(fs, PeriodOfPerformanceType.values());
    final DropDownChoice<PeriodOfPerformanceType> performanceTypeDropDown = new DropDownChoice<>(fs.getDropDownChoiceId(), periodOfPerformanceTypeModel,
        performanceTypeRenderer.getValues(), performanceTypeRenderer);
    performanceTypeDropDown.add(new AjaxFormComponentUpdatingBehavior("onchange")
    {
      @Override
      protected void onUpdate(final AjaxRequestTarget target)
      {
        // update visibility
        final boolean visible = hasOwnPeriodOfPerformance(performanceTypeDropDown);
        for (final Component ajaxPosTarget : componentsToToggleVisibility) {
          ajaxPosTarget.setVisible(visible);
          target.add(ajaxPosTarget);
        }
      }
    });
    performanceTypeDropDown.setRequired(true);
    performanceTypeDropDown.setOutputMarkupPlaceholderTag(true);
    fs.add(performanceTypeDropDown);
    performanceDropDowns.add(performanceTypeDropDown);

    final BooleanSupplier hasOwnPeriodOfPerformanceSupplier = () -> hasOwnPeriodOfPerformance(performanceTypeDropDown);

    // from date
    final DatePanel fromDatePanel = new DatePanel(fs.newChildId(), periodOfPerformanceBeginModel, DatePanelSettings.get().withTargetType(java.sql.Date.class),
        hasOwnPeriodOfPerformanceSupplier);
    fromDatePanel.getDateField().setOutputMarkupPlaceholderTag(true);
    fs.add(fromDatePanel);
    componentsToToggleVisibility.add(fromDatePanel.getDateField());
    datePanels.add(fromDatePanel);

    // "-" label
    final DivTextPanel minusTextPanel = new DivTextPanel(fs.newChildId(), "-");
    minusTextPanel.getLabel4Ajax().setOutputMarkupPlaceholderTag(true);
    fs.add(minusTextPanel);
    componentsToToggleVisibility.add(minusTextPanel.getLabel4Ajax());

    // end date
    final DatePanel endDatePanel = new DatePanel(fs.newChildId(), periodOfPerformanceEndModel, DatePanelSettings.get().withTargetType(java.sql.Date.class),
        hasOwnPeriodOfPerformanceSupplier);
    endDatePanel.getDateField().setOutputMarkupPlaceholderTag(true);
    fs.add(endDatePanel);
    componentsToToggleVisibility.add(endDatePanel.getDateField());
    datePanels.add(endDatePanel);

    // additional components
    componentsToToggleVisibility.addAll(Arrays.asList(additionalComponentsToToggleVisibility));

    // set initial visibility
    final boolean visible = hasOwnPeriodOfPerformance(performanceTypeDropDown);
    for (final Component component : componentsToToggleVisibility) {
      component.setVisible(visible);
    }
  }

  public IFormValidator createValidator()
  {
    return new IFormValidator()
    {
      @Override
      public FormComponent<?>[] getDependentFormComponents()
      {
        return datePanels.toArray(new DatePanel[datePanels.size()]);
      }

      @Override
      public void validate(final Form<?> form)
      {
        final Date performanceFromDate = fromDatePanel.getDateField().getConvertedInput();
        final Date performanceEndDate = endDatePanel.getDateField().getConvertedInput();
        if (performanceFromDate == null || performanceEndDate == null) {
          return;
        } else if (performanceEndDate.before(performanceFromDate) == true) {
          endDatePanel.error(form.getString("error.endDateBeforeBeginDate"));
        }

        final FormComponent<?>[] dependentFormComponents = getDependentFormComponents();

        for (int i = 0; i < dependentFormComponents.length - 1; i += 2) {
          final Date posPerformanceFromDate = ((DatePanel) dependentFormComponents[i]).getDateField().getConvertedInput();
          final Date posPerformanceEndDate = ((DatePanel) dependentFormComponents[i + 1]).getDateField().getConvertedInput();
          if (posPerformanceFromDate == null || posPerformanceEndDate == null) {
            continue;
          }
          if (posPerformanceEndDate.before(posPerformanceFromDate) == true) {
            dependentFormComponents[i + 1].error(form.getString("error.endDateBeforeBeginDate"));
          }
          if (posPerformanceFromDate.before(performanceFromDate) == true) {
            dependentFormComponents[i + 1].error(form.getString("error.posFromDateBeforeFromDate"));
          }
        }
      }
    };
  }

  private boolean hasOwnPeriodOfPerformance(final DropDownChoice<PeriodOfPerformanceType> performanceChoice)
  {
    return PeriodOfPerformanceType.OWN == performanceChoice.getModelObject();
  }
}

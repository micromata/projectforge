package org.projectforge.web.fibu;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.business.fibu.AuftragDO;
import org.projectforge.business.fibu.AuftragsPositionDO;
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

  private final Map<Short, List<Component>> positionComponentsToToggleVisibility = new HashMap<>();

  private DatePanel fromDatePanel;

  private DatePanel endDatePanel;

  public void onRefreshPositions()
  {
    performanceDropDowns.clear();
    datePanels.clear();
    positionComponentsToToggleVisibility.clear();
  }

  public void createPeriodOfPerformanceFields(final FieldsetPanel fs, final AuftragDO auftrag)
  {
    final BooleanSupplier isAnyPerformanceTypeSeeAboveSelected = () -> performanceDropDowns.stream()
        .map(FormComponent::getRawInput) // had to use getRawInput here instead of getModelObject, because it did not work well
        .anyMatch(PeriodOfPerformanceType.SEEABOVE.name()::equals);

    fromDatePanel = new DatePanel(fs.newChildId(), new PropertyModel<>(auftrag, "periodOfPerformanceBegin"),
        DatePanelSettings.get().withTargetType(java.sql.Date.class), isAnyPerformanceTypeSeeAboveSelected);
    fs.add(fromDatePanel);

    fs.add(new DivTextPanel(fs.newChildId(), "-"));

    endDatePanel = new DatePanel(fs.newChildId(), new PropertyModel<>(auftrag, "periodOfPerformanceEnd"),
        DatePanelSettings.get().withTargetType(java.sql.Date.class), isAnyPerformanceTypeSeeAboveSelected);
    fs.add(endDatePanel);
  }

  // TODO CT: check PropertyModels
  public void createPositionsPeriodOfPerformanceFields(final FieldsetPanel fs, final AuftragsPositionDO position)
  {
    final LabelValueChoiceRenderer<PeriodOfPerformanceType> performanceChoiceRenderer = new LabelValueChoiceRenderer<>(fs, PeriodOfPerformanceType.values());
    final DropDownChoice<PeriodOfPerformanceType> performanceChoice = new DropDownChoice<>(fs.getDropDownChoiceId(),
        new PropertyModel<>(position, "periodOfPerformanceType"), performanceChoiceRenderer.getValues(), performanceChoiceRenderer);

    final short posNumber = position.getNumber();
    performanceChoice.add(new AjaxFormComponentUpdatingBehavior("onchange")
    {
      @Override
      protected void onUpdate(final AjaxRequestTarget target)
      {
        // update visibility
        final boolean visible = hasOwnPeriodOfPerformance(performanceChoice);
        for (final Component ajaxPosTarget : positionComponentsToToggleVisibility.get(posNumber)) {
          ajaxPosTarget.setVisible(visible);
          target.add(ajaxPosTarget);
        }
      }
    });
    performanceChoice.setOutputMarkupPlaceholderTag(true);
    fs.add(performanceChoice);
    performanceDropDowns.add(performanceChoice);

    final BooleanSupplier hasOwnPeriodOfPerformanceSupplier = () -> hasOwnPeriodOfPerformance(performanceChoice);
    final List<Component> componentsToToggleVisibility = new ArrayList<>();
    positionComponentsToToggleVisibility.put(posNumber, componentsToToggleVisibility);

    final DatePanel fromDatePanel = new DatePanel(fs.newChildId(), new PropertyModel<>(position, "periodOfPerformanceBegin"),
        DatePanelSettings.get().withTargetType(java.sql.Date.class), hasOwnPeriodOfPerformanceSupplier);
    fromDatePanel.getDateField().setOutputMarkupPlaceholderTag(true);
    fs.add(fromDatePanel);
    componentsToToggleVisibility.add(fromDatePanel.getDateField());
    datePanels.add(fromDatePanel);

    final DivTextPanel minusTextPanel = new DivTextPanel(fs.newChildId(), "-");
    minusTextPanel.getLabel4Ajax().setOutputMarkupPlaceholderTag(true);
    fs.add(minusTextPanel);
    componentsToToggleVisibility.add(minusTextPanel.getLabel4Ajax());

    final DatePanel endDatePanel = new DatePanel(fs.newChildId(), new PropertyModel<>(position, "periodOfPerformanceEnd"),
        DatePanelSettings.get().withTargetType(java.sql.Date.class), hasOwnPeriodOfPerformanceSupplier);
    endDatePanel.getDateField().setOutputMarkupPlaceholderTag(true);
    fs.add(endDatePanel);
    componentsToToggleVisibility.add(endDatePanel.getDateField());
    datePanels.add(endDatePanel);

    // set initial visibility
    final boolean visible = hasOwnPeriodOfPerformance(performanceChoice);
    for (final Component component : componentsToToggleVisibility) {
      component.setVisible(visible);
    }
  }

  public void addToPositionComponentsToToggleVisibility(final short posNumber, final Component component)
  {
    final DropDownChoice<PeriodOfPerformanceType> lastPerformanceChoice = performanceDropDowns.get(performanceDropDowns.size() - 1);
    final boolean visible = hasOwnPeriodOfPerformance(lastPerformanceChoice);
    component.setVisible(visible);
    positionComponentsToToggleVisibility.get(posNumber).add(component);
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

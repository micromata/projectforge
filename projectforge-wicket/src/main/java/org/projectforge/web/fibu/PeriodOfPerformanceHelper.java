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

package org.projectforge.web.fibu;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.IModel;
import org.projectforge.business.fibu.PeriodOfPerformanceType;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.LocalDateModel;
import org.projectforge.web.wicket.components.LocalDatePanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

class PeriodOfPerformanceHelper implements Serializable {
    class MyBooleanSeeAboveSupplier implements BooleanSupplier, Serializable {
        @Override
        public boolean getAsBoolean() {
            for (FormComponent dropdown : performanceDropDowns) {
                String rawInput = dropdown.getRawInput();
                if (PeriodOfPerformanceType.SEEABOVE.name().equals(rawInput)) {
                    return true;
                }
            }
            return false;
        }
    }

    class MyBooleanSeeOwnPerformanceSupplier implements BooleanSupplier, Serializable {
        private DropDownChoice<PeriodOfPerformanceType> performanceTypeDropDown;
        public MyBooleanSeeOwnPerformanceSupplier(DropDownChoice<PeriodOfPerformanceType> performanceTypeDropDown) {
            this.performanceTypeDropDown = performanceTypeDropDown;
        }

        @Override
        public boolean getAsBoolean() {
            return hasOwnPeriodOfPerformance(performanceTypeDropDown);
        }
    }

    @Serial
    private static final long serialVersionUID = -1L;

    private final List<DropDownChoice<PeriodOfPerformanceType>> performanceDropDowns = new ArrayList<>();

    private final List<LocalDatePanel> datePanels = new ArrayList<>();

    private LocalDatePanel fromDatePanel;

    private LocalDatePanel endDatePanel;

    public void onRefreshPositions() {
        performanceDropDowns.clear();
        datePanels.clear();
    }

    public void createPeriodOfPerformanceFields(final FieldsetPanel fs, final IModel<LocalDate> periodOfPerformanceBeginModel, final IModel<LocalDate> periodOfPerformanceEndModel) {
        fromDatePanel = new LocalDatePanel(fs.newChildId(), new LocalDateModel(periodOfPerformanceBeginModel));
        MyBooleanSeeAboveSupplier isAnyPerformanceTypeSeeAboveSelected = new MyBooleanSeeAboveSupplier();
        fromDatePanel.setRequiredSupplier(isAnyPerformanceTypeSeeAboveSelected);
        fs.add(fromDatePanel);

        fs.add(new DivTextPanel(fs.newChildId(), "-"));

        endDatePanel = new LocalDatePanel(fs.newChildId(), new LocalDateModel(periodOfPerformanceEndModel));
        fromDatePanel.setRequiredSupplier(isAnyPerformanceTypeSeeAboveSelected);
        fs.add(endDatePanel);
    }

    public void createPositionsPeriodOfPerformanceFields(final FieldsetPanel fs, final IModel<PeriodOfPerformanceType> periodOfPerformanceTypeModel,
                                                         final IModel<LocalDate> periodOfPerformanceBeginModel, final IModel<LocalDate> periodOfPerformanceEndModel,
                                                         final Component... additionalComponentsToToggleVisibility) {
        final List<Component> componentsToToggleVisibility = new ArrayList<>();

        // drop down
        final LabelValueChoiceRenderer<PeriodOfPerformanceType> performanceTypeRenderer = new LabelValueChoiceRenderer<>(fs, PeriodOfPerformanceType.values());
        final DropDownChoice<PeriodOfPerformanceType> performanceTypeDropDown = new DropDownChoice<>(fs.getDropDownChoiceId(), periodOfPerformanceTypeModel,
                performanceTypeRenderer.getValues(), performanceTypeRenderer);
        performanceTypeDropDown.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
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

        final MyBooleanSeeOwnPerformanceSupplier hasOwnPeriodOfPerformanceSupplier = new MyBooleanSeeOwnPerformanceSupplier(performanceTypeDropDown);

        // from date
        final LocalDatePanel fromDatePanel = new LocalDatePanel(fs.newChildId(), new LocalDateModel(periodOfPerformanceBeginModel));
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
        final LocalDatePanel endDatePanel = new LocalDatePanel(fs.newChildId(), new LocalDateModel(periodOfPerformanceEndModel));
        endDatePanel.setRequiredSupplier(hasOwnPeriodOfPerformanceSupplier);
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

    public IFormValidator createValidator() {
        return new IFormValidator() {
            @Override
            public FormComponent<?>[] getDependentFormComponents() {
                return datePanels.toArray(new LocalDatePanel[0]);
            }

            @Override
            public void validate(final Form<?> form) {
                final LocalDate performanceFromDate = fromDatePanel.getConvertedInputAsLocalDate();
                final LocalDate performanceEndDate = endDatePanel.getConvertedInputAsLocalDate();
                if (performanceFromDate == null || performanceEndDate == null) {
                    return;
                } else if (performanceEndDate.isBefore(performanceFromDate)) {
                    endDatePanel.error(form.getString("error.endDateBeforeBeginDate"));
                }

                final FormComponent<?>[] dependentFormComponents = getDependentFormComponents();

                for (int i = 0; i < dependentFormComponents.length - 1; i += 2) {
                    final LocalDate posPerformanceFromDate = ((LocalDatePanel) dependentFormComponents[i]).getConvertedInputAsLocalDate();
                    final LocalDate posPerformanceEndDate = ((LocalDatePanel) dependentFormComponents[i + 1]).getConvertedInputAsLocalDate();
                    if (posPerformanceFromDate == null || posPerformanceEndDate == null) {
                        continue;
                    }
                    if (posPerformanceEndDate.isBefore(posPerformanceFromDate)) {
                        dependentFormComponents[i + 1].error(form.getString("error.endDateBeforeBeginDate"));
                    }
                    if (posPerformanceFromDate.isBefore(performanceFromDate)) {
                        dependentFormComponents[i + 1].error(form.getString("error.posFromDateBeforeFromDate"));
                    }
                }
            }
        };
    }

    private boolean hasOwnPeriodOfPerformance(final DropDownChoice<PeriodOfPerformanceType> performanceChoice) {
        return PeriodOfPerformanceType.OWN == performanceChoice.getModelObject();
    }
}

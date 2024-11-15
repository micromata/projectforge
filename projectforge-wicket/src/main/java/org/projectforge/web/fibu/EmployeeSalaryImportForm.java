/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.web.core.importstorage.AbstractImportForm;
import org.projectforge.web.core.importstorage.ImportFilter;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DropDownChoicePanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.FileUploadPanel;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class EmployeeSalaryImportForm extends AbstractImportForm<ImportFilter, EmployeeSalaryImportPage, EmployeeSalaryImportStoragePanel> {
    private static final List<Integer> MONTH_INTEGERS = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

    private static final List<Integer> years = new LinkedList<>();

    static {
        for (int i = 2001; i <= Year.now().getValue(); i++) {
            years.add(i);
        }
    }

    FileUploadField fileUploadField;

    private Integer selectedYear = getDefaultYear();

    private Integer selectedMonth = getDefaultMonth();

    private DropDownChoicePanel<Integer> dropDownMonth;

    private DropDownChoicePanel<Integer> dropDownYear;

    public EmployeeSalaryImportForm(final EmployeeSalaryImportPage parentPage) {
        super(parentPage);
    }

    @SuppressWarnings("serial")
    @Override
    protected void init() {
        super.init();

        gridBuilder.newGridPanel();

        // Date DropDowns
        final FieldsetPanel fsMonthYear = gridBuilder.newFieldset(I18nHelper.getLocalizedMessage("date.month_year"));

        dropDownMonth = new DropDownChoicePanel<>(fsMonthYear.newChildId(),
                new DropDownChoice<>(DropDownChoicePanel.WICKET_ID, new PropertyModel<>(this, "selectedMonth"), MONTH_INTEGERS)
        );
        dropDownMonth.setRequired(true);
        fsMonthYear.add(dropDownMonth);

        dropDownYear = new DropDownChoicePanel<>(fsMonthYear.newChildId(),
                new DropDownChoice<>(DropDownChoicePanel.WICKET_ID, new PropertyModel<>(this, "selectedYear"), years));
        dropDownYear.setRequired(true);
        fsMonthYear.add(dropDownYear);

        // upload buttons
        {
            final FieldsetPanel fs = gridBuilder.newFieldset(getString("file"), "*.xls");
            fileUploadField = new FileUploadField(FileUploadPanel.WICKET_ID);
            fs.add(new FileUploadPanel(fs.newChildId(), fileUploadField));
            fs.add(new SingleButtonPanel(fs.newChildId(), new Button(SingleButtonPanel.WICKET_ID) {
                @Override
                public void onSubmit() {
                    final boolean success = parentPage.doImport(selectedYear, Month.of(selectedMonth));
                    if (success) {
                        setDateDropDownsEnabled(false);
                    }
                }
            }, getString("upload"), SingleButtonPanel.NORMAL).setTooltip(getString("common.import.upload.tooltip")));
            addClearButton(fs);
        }

        addImportFilterRadio(gridBuilder);

        // preview of the imported data
        gridBuilder.newGridPanel();
        final DivPanel panel = gridBuilder.getPanel();
        storagePanel = new EmployeeSalaryImportStoragePanel(panel.newChildId(), parentPage, importFilter);
        panel.add(storagePanel);
    }

    void setDateDropDownsEnabled(boolean enabled) {
        dropDownMonth.setEnabled(enabled);
        dropDownYear.setEnabled(enabled);
    }

    public Integer getSelectedYear() {
        return selectedYear;
    }

    public void setSelectedYear(Integer selectedYear) {
        this.selectedYear = selectedYear;
    }

    public Integer getSelectedMonth() {
        return selectedMonth;
    }

    public void setSelectedMonth(Integer selectedMonth) {
        this.selectedMonth = selectedMonth;
    }

    protected Integer getDefaultYear() {
        return LocalDate.now().minusMonths(1).getYear();
    }

    protected Integer getDefaultMonth() {
        return LocalDate.now().minusMonths(1).getMonth().getValue();
    }
}

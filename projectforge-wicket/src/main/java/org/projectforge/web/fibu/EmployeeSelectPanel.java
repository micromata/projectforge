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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.business.fibu.EmployeeCache;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.business.fibu.OldKostFormatter;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.service.UserXmlPreferencesService;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.RecentQueue;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.wicket.AbstractSelectPanel;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This panel shows the actual employee and buttons for select/unselect employee.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class EmployeeSelectPanel extends AbstractSelectPanel<EmployeeDO> {
    private static final long serialVersionUID = -9161889503240264619L;

    private static final String USER_PREF_KEY_RECENT_EMPLOYEES = "EmployeeSelectPanel:recentEmployees";

    private static final String[] SEARCH_FIELDS = {"user.username", "user.firstname", "user.lastname"};

    private RecentQueue<String> recentEmployees;

    private PFAutoCompleteTextField<EmployeeDO> employeeTextField;

    // Only used for detecting changes:
    private EmployeeDO currentEmployee;

    /**
     * Label is assumed as "fibu.employee" translation.
     *
     * @param id
     * @param model
     * @param caller
     * @param selectProperty
     */
    public EmployeeSelectPanel(final String id, final IModel<EmployeeDO> model, final ISelectCallerPage caller,
                               final String selectProperty) {
        this(id, model, null, caller, selectProperty);
    }

    /**
     * @param id
     * @param model
     * @param label          Only needed for validation messages (feed back).
     * @param caller
     * @param selectProperty
     */
    @SuppressWarnings("serial")
    public EmployeeSelectPanel(final String id, final IModel<EmployeeDO> model, final String label,
                               final ISelectCallerPage caller,
                               final String selectProperty) {
        super(id, model, caller, selectProperty);
        employeeTextField = new PFAutoCompleteTextField<EmployeeDO>("employeeField", getModel()) {
            @Override
            protected List<EmployeeDO> getChoices(final String input) {
                return getFilteredEmployeeDOs(input);
            }

            @Override
            protected List<String> getRecentUserInputs() {
                return getRecentEmployees().getRecentList();
            }

            @Override
            protected String formatLabel(final EmployeeDO employee) {
                if (employee == null) {
                    return "";
                }
                return formatEmployee(employee);
            }

            @Override
            protected String formatValue(final EmployeeDO employee) {
                if (employee == null || employee.getUser() == null) {
                    return "";
                }
                return employee.getUser().getUsername() + ": " + employee.getUser().getFullname();
            }

            @Override
            protected String getTooltip() {
                final EmployeeDO employee = getModel().getObject();
                if (employee == null) {
                    return null;
                }
                return OldKostFormatter.format(employee.getKost1()) + ": " + employee.getUser().getFullname();
            }

            @Override
            public void convertInput() {
                final EmployeeDO employee = (EmployeeDO) getConverter(getType()).convertToObject(getInput(), getLocale());
                setConvertedInput(employee);
                if (employee != null && (currentEmployee == null || employee.getId() != currentEmployee.getId())) {
                    getRecentEmployees().append(formatEmployee(employee));
                }
                currentEmployee = employee;
            }

            @SuppressWarnings({"rawtypes", "unchecked"})
            @Override
            public IConverter getConverter(final Class type) {
                return new IConverter() {
                    @Override
                    public Object convertToObject(final String value, final Locale locale) {
                        if (StringUtils.isEmpty(value) == true) {
                            getModel().setObject(null);
                            return null;
                        }
                        final int ind = value.indexOf(": ");
                        final String username = ind >= 0 ? value.substring(0, ind) : value;
                        final PFUserDO user = UserGroupCache.getInstance().getUser(username);
                        if (user == null) {
                            error(getString("fibu.employee.panel.error.employeeNotFound"));
                            return null;
                        }
                        final EmployeeDO employee = WicketSupport.get(EmployeeCache.class).getEmployeeByUserId(user.getId());
                        if (employee == null) {
                            error(getString("fibu.employee.panel.error.employeeNotFound"));
                            return null;
                        }
                        getModel().setObject(employee);
                        return employee;
                    }

                    @Override
                    public String convertToString(final Object value, final Locale locale) {
                        if (value == null) {
                            return "";
                        }
                        final EmployeeDO employee = (EmployeeDO) value;
                        return employee.getUser().getUsername() + ": " + employee.getUser().getFullname();
                    }
                };
            }
        };
        currentEmployee = getModelObject();
        employeeTextField.enableTooltips().withLabelValue(true).withMatchContains(true).withMinChars(2)
                .withAutoSubmit(false).withWidth(400);
        employeeTextField.setLabel(new Model<String>() {
            @Override
            public String getObject() {
                if (label != null) {
                    return label;
                } else {
                    return getString("fibu.employee");
                }
            }
        });
        add(employeeTextField);
    }

    private List<EmployeeDO> getFilteredEmployeeDOs(String input) {
        final BaseSearchFilter filter = new BaseSearchFilter();
        filter.setSearchFields(SEARCH_FIELDS);
        filter.setSearchString(input);
        final List<EmployeeDO> list = WicketSupport.get(EmployeeDao.class).select(filter);
        List<EmployeeDO> resultList = new ArrayList<>(list);
        for (EmployeeDO employeeDO : list) {
            if (employeeDO.getAustrittsDatum() != null && LocalDate.now().isAfter(employeeDO.getAustrittsDatum())) {
                resultList.remove(resultList.indexOf(employeeDO));
            }
        }
        return resultList;
    }

    /**
     * @see org.projectforge.web.wicket.AbstractSelectPanel#setFocus()
     */
    @Override
    public EmployeeSelectPanel setFocus() {
        employeeTextField.add(WicketUtils.setFocus());
        return this;
    }

    @Override
    public EmployeeSelectPanel init() {
        super.init();
        return this;
    }

    /**
     * @see org.projectforge.web.wicket.AbstractSelectPanel#getClassModifierComponent()
     */
    @Override
    public Component getClassModifierComponent() {
        return employeeTextField;
    }

    public EmployeeSelectPanel withAutoSubmit(final boolean autoSubmit) {
        employeeTextField.withAutoSubmit(autoSubmit);
        return this;
    }

    @Override
    public void convertInput() {
        setConvertedInput(getModelObject());
    }

    @SuppressWarnings("unchecked")
    private RecentQueue<String> getRecentEmployees() {
        var userPreferencesService = WicketSupport.get(UserXmlPreferencesService.class);
        if (this.recentEmployees == null) {
            this.recentEmployees = (RecentQueue<String>) userPreferencesService.getEntry(USER_PREF_KEY_RECENT_EMPLOYEES);
        }
        if (this.recentEmployees == null) {
            this.recentEmployees = new RecentQueue<String>();
            userPreferencesService.putEntry(USER_PREF_KEY_RECENT_EMPLOYEES, this.recentEmployees, true);
        }
        return this.recentEmployees;
    }

    private String formatEmployee(final EmployeeDO employee) {
        if (employee == null) {
            return "";
        }
        return employee.getUser().getUsername()
                + " ("
                + employee.getUser().getFullname()
                + ": "
                + OldKostFormatter.format(employee.getKost1())
                + ")";
    }
}

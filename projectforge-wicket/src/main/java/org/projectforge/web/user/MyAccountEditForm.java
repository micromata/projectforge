/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.user;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.group.service.GroupService;
import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.user.UserAuthenticationsService;
import org.projectforge.business.user.UserTokenType;
import org.projectforge.business.user.UserXmlPreferencesDao;
import org.projectforge.business.user.service.UserService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.dialog.ModalDialog;
import org.projectforge.web.fibu.EmployeeEditForm;
import org.projectforge.web.teamcal.admin.TeamCalsProvider;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.flowlayout.AjaxIconButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.IconType;
import org.slf4j.Logger;
import org.wicketstuff.select2.Select2MultiChoice;

import java.util.Arrays;
import java.util.Collection;

public class MyAccountEditForm extends AbstractEditForm<PFUserDO, MyAccountEditPage> {
  private static final long serialVersionUID = 4137560623244324454L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MyAccountEditForm.class);

  boolean invalidateAllStayLoggedInSessions;

  @SpringBean
  private ConfigurationService configurationService;

  @SpringBean
  private UserService userService;

  @SpringBean
  private EmployeeService employeeService;

  private EmployeeDO employeeData;

  @SpringBean
  private GroupService groupService;

  @SpringBean
  private TeamCalCache teamCalCache;

  @SpringBean
  private UserXmlPreferencesDao userXmlPreferencesDao;

  @SpringBean
  private UserAuthenticationsService userAuthenticationsService;

  private Collection<TeamCalDO> teamCalRestWhiteList;

  private ModalDialog userAccessLogEntriesDialog;
  private String userAccessLogEntries = "";
  private DivTextPanel userAccessLogEntriesTextPanel;

  public MyAccountEditForm(final MyAccountEditPage parentPage, final PFUserDO data) {
    super(parentPage, data);
  }

  public static void createAddressData(final GridBuilder gridBuilder, EmployeeDO data) {
    gridBuilder.newSubSplitPanel(GridSize.COL33);
    gridBuilder.newFormHeading("");
    EmployeeEditForm.generateStreetZipCityFields(gridBuilder, data);

    gridBuilder.newSubSplitPanel(GridSize.COL33);
    gridBuilder.newFormHeading("");
    EmployeeEditForm.generateCountryStateFields(gridBuilder, data);
  }

  @Override
  protected void init() {
    super.init();
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // User
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("user")).suppressLabelForWarning();
      fs.add(new DivTextPanel(fs.newChildId(), data.getUsername()).setStrong());
    }
    UserEditForm.createFirstName(gridBuilder, data);
    UserEditForm.createLastName(gridBuilder, data);
    addTokenRow(UserTokenType.CALENDAR_REST);
    if (configurationService.isDAVServicesAvailable()) {
      addTokenRow(UserTokenType.DAV_TOKEN);
    }
    addTokenRow(UserTokenType.REST_CLIENT);
    FieldsetPanel fs = gridBuilder.newFieldset(getString("user.assignedGroups")).suppressLabelForWarning();
    fs.add(new DivTextPanel(fs.newChildId(), groupService.getGroupnames(data.getId())));

    gridBuilder.newSplitPanel(GridSize.COL50);
    fs = UserEditForm.createLastLoginAndDeleteAllStayLogins(gridBuilder, data, userAuthenticationsService, this);
    addInfoButton(fs, UserTokenType.STAY_LOGGED_IN_KEY);

    UserEditForm.createLocale(gridBuilder, data);
    UserEditForm.createDateFormat(gridBuilder, data);
    UserEditForm.createExcelDateFormat(gridBuilder, data);
    UserEditForm.createTimeNotation(gridBuilder, data);
    UserEditForm.createTimeZone(gridBuilder, data);
    UserEditForm.createPhoneIds(gridBuilder, data);
    UserEditForm.createMEBPhoneNumbers(gridBuilder, data);

    gridBuilder.newSplitPanel(GridSize.COL100);

    // CALENDAR WHITE LIST
    final FieldsetPanel fieldSet = gridBuilder.newFieldset(getString("user.myAccount.teamcalwhitelist"));
    this.teamCalRestWhiteList = teamCalCache.getAllFullAccessCalendars();
    Integer[] teamCalBlackListIds = userXmlPreferencesDao
            .getDeserializedUserPreferencesByUserId(ThreadLocalUserContext.getUserId(), TeamCalDO.Companion.getTEAMCALRESTBLACKLIST(), Integer[].class);
    if (teamCalBlackListIds != null && teamCalBlackListIds.length > 0) {
      Arrays.stream(teamCalBlackListIds).forEach(calId -> teamCalRestWhiteList.remove(teamCalCache.getCalendar(calId)));
    }

    final Select2MultiChoice<TeamCalDO> calendars = new Select2MultiChoice<>(
            fieldSet.getSelect2MultiChoiceId(),
            new PropertyModel<Collection<TeamCalDO>>(this, "teamCalRestWhiteList"),
            new TeamCalsProvider(teamCalCache, true));
    calendars.setMarkupId("calenders").setOutputMarkupId(true);
    fieldSet.add(calendars);

    employeeData = employeeService.getEmployeeByUserId(data.getId());

    // If this user has no employee object then th employee form part must not be displayed.
    if (employeeData != null) {
      gridBuilder.newSplitPanel(GridSize.COL100, true).newFormHeading(getString("fibu.employee"));

      createAddressData(gridBuilder, employeeData);
      EmployeeEditForm.createBirthdayPanel(gridBuilder, employeeData);

      gridBuilder.newSubSplitPanel(GridSize.COL33);
      gridBuilder.newFormHeading(getString("fibu.employee.bankAccountData"));
      EmployeeEditForm.createBankingDetails(gridBuilder, employeeData);
    }

    gridBuilder.newGridPanel();
    UserEditForm.createDescription(gridBuilder, data);
    UserEditForm.createSshPublicKey(gridBuilder, data);
    addUserAccessLogEntriesDialog();
  }

  private void addTokenRow(UserTokenType tokenType) {
    final FieldsetPanel fs = UserEditForm.createAuthenticationToken(gridBuilder, data, userAuthenticationsService, this, tokenType);
    addInfoButton(fs, tokenType);
  }

  private void addInfoButton(FieldsetPanel fs, UserTokenType tokenType) {
    AjaxIconButtonPanel showInfoButton = new AjaxIconButtonPanel(fs.newChildId(), IconType.WRENCH, fs.getString("user.authenticationToken.button.showUsage.tooltip")) {
      /**
       * @see org.projectforge.web.wicket.flowlayout.AjaxIconButtonPanel#onSubmit(org.apache.wicket.ajax.AjaxRequestTarget)
       */
      @Override
      protected void onSubmit(final AjaxRequestTarget target) {
        userAccessLogEntries = userAuthenticationsService.getUserAccessLogEntries(tokenType).asText("<br/>", true);
        userAccessLogEntriesTextPanel.getLabel4Ajax().modelChanged();
        target.add(userAccessLogEntriesTextPanel.getLabel4Ajax());
        userAccessLogEntriesDialog.open(target);
      }
    };
    showInfoButton.getButton().setOutputMarkupPlaceholderTag(true);
    fs.add(showInfoButton);
  }

  @Override
  public void updateButtonVisibility() {
    super.updateButtonVisibility();
    createButtonPanel.setVisible(false);
    updateButtonPanel.setVisible(true);
    deleteButtonPanel.setVisible(false);
    markAsDeletedButtonPanel.setVisible(false);
    undeleteButtonPanel.setVisible(false);
  }

  @Override
  protected Logger getLogger() {
    return log;
  }

  public EmployeeDO getEmployeeData() {
    return employeeData;
  }

  public Collection<TeamCalDO> getTeamCalRestWhiteList() {
    return teamCalRestWhiteList;
  }

  protected void addUserAccessLogEntriesDialog() {
    userAccessLogEntriesDialog = new ModalDialog(parentPage.newModalDialogId()) {
      @Override
      public void init() {
        setTitle(getString("user.authenticationToken.button.showUsage"));
        init(new Form<String>(getFormId()));
        {
          final FieldsetPanel fs = gridBuilder.newFieldset(getString("user.authenticationToken.button.showUsage")).setLabelSide(false);
          userAccessLogEntriesTextPanel = new DivTextPanel(fs.newChildId(), new Model<String>() {
            @Override
            public String getObject() {
              return MyAccountEditForm.this.userAccessLogEntries;
            }
          });
          userAccessLogEntriesTextPanel.getLabel4Ajax().setEscapeModelStrings(false);
          fs.add(userAccessLogEntriesTextPanel);
        }
      }
    };
    userAccessLogEntriesDialog.setBigWindow().setOutputMarkupId(true);
    parentPage.add(userAccessLogEntriesDialog);
    userAccessLogEntriesDialog.init();

  }
}

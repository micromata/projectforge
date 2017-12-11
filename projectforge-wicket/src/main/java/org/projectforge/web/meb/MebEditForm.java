/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.web.meb;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hibernate.Hibernate;
import org.projectforge.business.meb.MebEntryDO;
import org.projectforge.business.meb.MebEntryStatus;
import org.projectforge.business.orga.PostType;
import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.jira.JiraConfig;
import org.projectforge.jira.JiraIssueType;
import org.projectforge.jira.JiraProject;
import org.projectforge.web.URLHelper;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.FavoritesChoicePanel;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;
import org.slf4j.Logger;

public class MebEditForm extends AbstractEditForm<MebEntryDO, MebEditPage>
{
  private static final long serialVersionUID = -1447905028243511191L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MebEditForm.class);

  private static final String USER_PREF_KEY_JIRA_PROJECT = "meb.edit.recentJiraProject";

  private static final String USER_PREF_KEY_JIRA_ISSUE_TYPE = "meb.edit.recentJiraIssueType";

  private Integer jiraIssueType;

  private FavoritesChoicePanel<JiraProject, JiraProject> jiraProjectChoice;

  private final JiraConfig jiraConfig = ConfigXml.getInstance().getJiraConfig();

  public MebEditForm(final MebEditPage parentPage, final MebEntryDO data)
  {
    super(parentPage, data);
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    gridBuilder.newGridPanel();
    {
      // Date
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("date")).suppressLabelForWarning();
      fs.add(new DivTextPanel(fs.newChildId(), DateTimeFormatter.instance().getFormattedDateTime(data.getDate())));
    }
    {
      // Owner
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("meb.owner"));
      PFUserDO owner = data.getOwner();
      if (Hibernate.isInitialized(owner) == false) {
        owner = getTenantRegistry().getUserGroupCache().getUser(owner.getId());
        data.setOwner(owner);
      }
      final UserSelectPanel userSelectPanel = new UserSelectPanel(fs.newChildId(), new PropertyModel<PFUserDO>(data, "owner"), parentPage,
          "ownerId");
      userSelectPanel.setRequired(true);
      fs.add(userSelectPanel);
      userSelectPanel.init();
    }
    {
      // Owner
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("meb.sender")).suppressLabelForWarning();
      fs.add(new DivTextPanel(fs.newChildId(), data.getSender()));
    }
    {
      // DropDownChoice status
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("status"));
      final LabelValueChoiceRenderer<PostType> statusChoiceRenderer = new LabelValueChoiceRenderer<PostType>(this, MebEntryStatus.values());
      final DropDownChoice<PostType> statusChoice = new DropDownChoice<PostType>(fs.getDropDownChoiceId(), new PropertyModel<PostType>(
          data, "status"), statusChoiceRenderer.getValues(), statusChoiceRenderer);
      statusChoice.setNullValid(false);
      statusChoice.setRequired(true);
      fs.add(statusChoice);
    }
    {
      // Message
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("meb.message"));
      fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<String>(data, "message")));
    }
    {
      // Actions
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("meb.actions")).suppressLabelForWarning();
      fs.add(new SingleButtonPanel(fs.newChildId(), new Button(SingleButtonPanel.WICKET_ID, new Model<String>("createTimesheet")) {
        @Override
        public final void onSubmit()
        {
          parentPage.createTimesheet();
        }
      }, getString("timesheet.title.add"), SingleButtonPanel.NORMAL));

      // DropDownChoice favorites
      jiraProjectChoice = new FavoritesChoicePanel<JiraProject, JiraProject>(fs.newChildId(), UserPrefArea.JIRA_PROJECT) {
        @Override
        protected void select(final JiraProject favorite)
        {
          if (StringUtils.isNotEmpty(this.selected) == true) {
            parentPage.putUserPrefEntry(USER_PREF_KEY_JIRA_PROJECT, this.selected, true);
          }
        }

        @Override
        protected JiraProject getCurrentObject()
        {
          return null;
        }

        @Override
        protected JiraProject newFavoriteInstance(final JiraProject currentObject)
        {
          return new JiraProject();
        }
      };
      jiraProjectChoice.setClearSelectionAfterSelection(false).setNullKey("jira.chooseProject");
      fs.add(jiraProjectChoice);
      final DropDownChoice<String> choice = jiraProjectChoice.init();
      choice.setNullValid(false);
      List<JiraIssueType> issueTypes;
      if (jiraConfig != null && jiraConfig.getIssueTypes() != null) {
        issueTypes = jiraConfig.getIssueTypes();
      } else {
        issueTypes = new ArrayList<JiraIssueType>();
      }
      // DropDownChoice issueType
      final LabelValueChoiceRenderer<JiraIssueType> typeChoiceRenderer = new LabelValueChoiceRenderer<JiraIssueType>(issueTypes);
      @SuppressWarnings({ "rawtypes", "unchecked"})
      final DropDownChoice typeChoice = new DropDownChoice(fs.getDropDownChoiceId(), new PropertyModel(this, "jiraIssueType"),
          typeChoiceRenderer.getValues(), typeChoiceRenderer) {
        @Override
        protected boolean wantOnSelectionChangedNotifications()
        {
          return true;
        }

        @Override
        protected void onSelectionChanged(final Object newSelection)
        {
          if (newSelection != null && newSelection instanceof Integer) {
            parentPage.putUserPrefEntry(USER_PREF_KEY_JIRA_ISSUE_TYPE, newSelection, true);
            // refresh();
          }
        }
      };
      final Integer recentJiraIssueType = (Integer) parentPage.getUserPrefEntry(Integer.class, USER_PREF_KEY_JIRA_ISSUE_TYPE);
      if (recentJiraIssueType != null) {
        this.jiraIssueType = recentJiraIssueType;
      }
      typeChoice.setNullValid(false);
      fs.add(typeChoice);

      final AjaxButton createJiraIssueButton = new AjaxButton(SingleButtonPanel.WICKET_ID, new Model<String>("createJIRAIssue")) {
        @Override
        public void onSubmit(final AjaxRequestTarget target, final Form< ? > form)
        {
          // ...create result page, get the url path to it...
          target.appendJavaScript("window.open('" + buildCreateJiraIssueUrl() + "','newWindow');");
        }

        /**
         * @see org.apache.wicket.ajax.markup.html.form.AjaxButton#onError(org.apache.wicket.ajax.AjaxRequestTarget,
         *      org.apache.wicket.markup.html.form.Form)
         */
        @Override
        protected void onError(final AjaxRequestTarget target, final Form< ? > form)
        {
        }
      };
      WicketUtils.addTooltip(createJiraIssueButton, getString("tooltip.popups.mustBeAllowed"));
      fs.add(new SingleButtonPanel(fs.newChildId(), createJiraIssueButton, getString("meb.actions.createJIRAIssue"), SingleButtonPanel.NORMAL));
      if (jiraConfig == null || StringUtils.isEmpty(jiraConfig.getCreateIssueUrl()) == true) {
        jiraProjectChoice.setVisible(false);
        typeChoice.setVisible(false);
        // jiraCreateIssueLink.setVisible(false);
        createJiraIssueButton.setVisible(false);
      } else {
        final String recentJiraProjectFavorite = (String) parentPage.getUserPrefEntry(String.class, USER_PREF_KEY_JIRA_PROJECT);
        if (recentJiraProjectFavorite != null) {
          jiraProjectChoice.setSelected(recentJiraProjectFavorite);
        }
      }
    }
  }

  private String buildCreateJiraIssueUrl()
  {
    if (jiraConfig == null || jiraConfig.getCreateIssueUrl() == null) {
      return "JIRA not configured.";
    }
    final JiraProject jiraProject = jiraProjectChoice.getCurrentFavorite();
    return jiraConfig.getCreateIssueUrl()
        + "?pid="
        + (jiraProject != null ? jiraProject.getPid() : null)
        + "&issuetype="
        + (jiraIssueType != null ? jiraIssueType : 3)
        + "&priority=4&reporter="
        + URLHelper.encode(getUser().getJiraUsernameOrUsername())
        + "&description="
        + URLHelper.encode(getData().getMessage());
  }

  public Integer getJiraIssueType()
  {
    return jiraIssueType;
  }

  public void setJiraIssueType(final Integer jiraIssueType)
  {
    this.jiraIssueType = jiraIssueType;
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}

/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.todo;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import org.hibernate.Hibernate;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.user.UserPrefDao;
import org.projectforge.common.i18n.Priority;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.UserPrefDO;
import org.projectforge.web.dialog.ModalDialog;
import org.projectforge.web.task.TaskSelectPanel;
import org.projectforge.web.user.NewGroupSelectPanel;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.*;
import org.projectforge.web.wicket.flowlayout.*;
import org.slf4j.Logger;

import java.util.Date;

public class ToDoEditForm extends AbstractEditForm<ToDoDO, ToDoEditPage>
{
  private static final long serialVersionUID = -6208809585214296635L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ToDoEditForm.class);

  @SpringBean
  private UserPrefDao userPrefDao;

  @SpringBean
  private ConfigurationService configurationService;

  protected boolean saveAsTemplate, sendNotification = true, sendShortMessage;

  @SuppressWarnings("unused")
  private String templateName; // Used by Wicket

  private ModalDialog closeToDoDialog;

  private MaxLengthTextArea commentTextArea, closeToDoDialogCommentTextArea;

  protected NewGroupSelectPanel groupSelectPanel;

  public ToDoEditForm(final ToDoEditPage parentPage, final ToDoDO data)
  {
    super(parentPage, data);
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    gridBuilder.newGridPanel();
    if (isNew()) {
      // Favorites
      final String[] templateNames = userPrefDao.getPrefNames(ToDoPlugin.USER_PREF_AREA);
      if (templateNames != null && templateNames.length > 0) {
        // DropDownChoice templates
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("templates"));
        final LabelValueChoiceRenderer<String> templateNamesChoiceRenderer = new LabelValueChoiceRenderer<>();
        templateNamesChoiceRenderer.addValue("", getString("userPref.template.select"));
        for (final String name : templateNames) {
          templateNamesChoiceRenderer.addValue(name, name);
        }
        final DropDownChoice<String> templateNamesChoice = new DropDownChoice<String>(fs.getDropDownChoiceId(),
            new PropertyModel<>(
                this, "templateName"),
            templateNamesChoiceRenderer.getValues(), templateNamesChoiceRenderer)
        {
          @Override
          protected boolean wantOnSelectionChangedNotifications()
          {
            return true;
          }

          @Override
          protected CharSequence getDefaultChoice(final String selected)
          {
            return "";
          }

          @SuppressWarnings({ "unchecked", "rawtypes" })
          @Override
          protected void onSelectionChanged(final String newSelection)
          {
            if (StringUtils.isNotEmpty(newSelection)) {
              // Fill fields with selected template values:
              final UserPrefDO userPref = userPrefDao.getUserPref(ToDoPlugin.USER_PREF_AREA, newSelection);
              if (userPref != null) {
                userPrefDao.fillFromUserPrefParameters(userPref, data);
              }
              templateName = "";
              // Mark all form components as model changed.
              visitFormComponents(new IVisitor()
              {
                @Override
                public void component(final Object object, final IVisit visit)
                {
                  final FormComponent<?> fc = (FormComponent<?>) object;
                  fc.modelChanged();
                  visit.dontGoDeeper();
                }
              });
            }
          }
        };
        templateNamesChoice.setNullValid(true);
        fs.add(templateNamesChoice);
      }
    }

    {
      // Subject
      final FieldsetPanel fs = gridBuilder.newFieldset(ToDoDO.class, "subject");
      final RequiredMaxLengthTextField subject = new RequiredMaxLengthTextField(fs.getTextFieldId(),
          new PropertyModel<>(data,
              "subject"));
      if (isNew()) {
        // Only focus for new to-do's:
        subject.add(WicketUtils.setFocus());
      }
      fs.add(subject);
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // ToDo type
      final FieldsetPanel fs = gridBuilder.newFieldset(ToDoDO.class, "type");
      final LabelValueChoiceRenderer<ToDoType> typeChoiceRenderer = new LabelValueChoiceRenderer<>(this,
          ToDoType.values());
      fs.addDropDownChoice(new PropertyModel<>(data, "type"), typeChoiceRenderer.getValues(),
          typeChoiceRenderer)
          .setNullValid(true);
    }
    {
      // Status
      final FieldsetPanel fs = gridBuilder.newFieldset(ToDoDO.class, "status");
      final LabelValueChoiceRenderer<ToDoStatus> statusChoiceRenderer = new LabelValueChoiceRenderer<>(this,
          ToDoStatus.values());
      fs.addDropDownChoice(new PropertyModel<>(data, "status"), statusChoiceRenderer.getValues(),
          statusChoiceRenderer)
          .setNullValid(true);
    }
    {
      // Due date
      final FieldsetPanel fs = gridBuilder.newFieldset(ToDoDO.class, "dueDate");
      fs.add(new DatePanel(fs.newChildId(), new PropertyModel<>(data, "dueDate"),
          DatePanelSettings.get().withTargetType(
              java.sql.Date.class)));
    }

    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Priority
      final FieldsetPanel fs = gridBuilder.newFieldset(ToDoDO.class, "priority");
      final LabelValueChoiceRenderer<Priority> priorityChoiceRenderer = new LabelValueChoiceRenderer<>(this,
          Priority.values());
      fs.addDropDownChoice(new PropertyModel<>(data, "priority"), priorityChoiceRenderer.getValues(),
          priorityChoiceRenderer)
          .setNullValid(true);
    }
    {
      // Assignee
      final FieldsetPanel fs = gridBuilder.newFieldset(ToDoDO.class, "assignee");
      PFUserDO assignee = data.getAssignee();
      if (!Hibernate.isInitialized(assignee)) {
        assignee = getTenantRegistry().getUserGroupCache().getUser(assignee.getId());
        data.setAssignee(assignee);
      }
      final UserSelectPanel assigneeUserSelectPanel = new UserSelectPanel(fs.newChildId(),
          new PropertyModel<>(data, "assignee"),
          parentPage, "assigneeId");
      fs.add(assigneeUserSelectPanel);
      assigneeUserSelectPanel.setRequired(true);
      assigneeUserSelectPanel.init();
    }
    {
      // Reporter
      final FieldsetPanel fs = gridBuilder.newFieldset(ToDoDO.class, "reporter");
      PFUserDO reporter = data.getReporter();
      if (!Hibernate.isInitialized(reporter)) {
        reporter = getTenantRegistry().getUserGroupCache().getUser(reporter.getId());
        data.setReporter(reporter);
      }
      final UserSelectPanel reporterUserSelectPanel = new UserSelectPanel(fs.newChildId(),
          new PropertyModel<>(data, "reporter"),
          parentPage, "reporterId");
      fs.add(reporterUserSelectPanel);
      reporterUserSelectPanel.init();
    }
    gridBuilder.newGridPanel();
    {
      // Task
      final FieldsetPanel fs = gridBuilder.newFieldset(ToDoDO.class, "task");
      final TaskSelectPanel taskSelectPanel = new TaskSelectPanel(fs, new PropertyModel<>(data, "task"),
          parentPage, "taskId");
      fs.add(taskSelectPanel);
      taskSelectPanel.init();
      fs.addHelpIcon(new ResourceModel("plugins.todo.task.tooltip.title"),
          new ResourceModel("plugins.todo.task.tooltip.content"));
    }
    {
      // Group
      final FieldsetPanel fs = gridBuilder.newFieldset(ToDoDO.class, "group");
      groupSelectPanel = new NewGroupSelectPanel(fs.newChildId(), new PropertyModel<>(data, "group"),
          parentPage, "groupId");
      fs.add(groupSelectPanel);
      fs.setLabelFor(groupSelectPanel);
      fs.addHelpIcon(new ResourceModel("plugins.todo.group.tooltip.title"),
          new ResourceModel("plugins.todo.group.tooltip.content"));
      groupSelectPanel.init();
    }
    {
      // Description
      final FieldsetPanel fs = gridBuilder.newFieldset(ToDoDO.class, "description");
      fs.add(new MaxLengthTextArea(fs.getTextAreaId(), new PropertyModel<>(data, "description"))).setAutogrow();
    }
    {
      // Comment
      final FieldsetPanel fs = gridBuilder.newFieldset(ToDoDO.class, "comment");
      commentTextArea = new MaxLengthTextArea(fs.getTextAreaId(), new PropertyModel<>(data, "comment"));
      fs.add(commentTextArea).setAutogrow();
    }
    {
      // Options
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("label.options")).suppressLabelForWarning();
      final DivPanel checkBoxButton = fs.addNewCheckBoxButtonDiv();
      if (configurationService.isSendMailConfigured()) {
        checkBoxButton
            .add(new CheckBoxButton(checkBoxButton.newChildId(), new PropertyModel<>(this, "sendNotification"),
                getString("label.sendEMailNotification")).setTooltip(getString("plugins.todo.notification.tooltip")));
      }
      // if (ConfigXml.getInstance().isSmsConfigured() == true) {
      // checkBoxPanel.add(new CheckBoxPanel(checkBoxPanel.newChildId(), new PropertyModel<Boolean>(this, "sendShortMessage"),
      // getString("label.sendShortMessage")));
      // }
      checkBoxButton
          .add(new CheckBoxButton(checkBoxButton.newChildId(), new PropertyModel<>(this, "saveAsTemplate"),
              getString("userPref.saveAsTemplate")));
    }
    if (!isNew()
        && getData().getStatus() != ToDoStatus.CLOSED
        && !getData().isDeleted()
        && getBaseDao().hasLoggedInUserUpdateAccess(getData(), getData(), false)) {
      // Close button:
      final AjaxButton closeButton = new AjaxButton(ButtonPanel.BUTTON_ID, this)
      {
        @Override
        protected void onSubmit(final AjaxRequestTarget target, final Form<?> form)
        {
          // repaint the feedback panel so that it is hidden:
          target.add(((ToDoEditForm) form).getFeedbackPanel());
          getData().setComment(commentTextArea.getConvertedInput());
          closeToDoDialogCommentTextArea.modelChanged();
          target.add(closeToDoDialogCommentTextArea);
          closeToDoDialog.open(target);
          // Focus doesn't yet work:
          // + "$('#"
          // + closeToDoDialog.getMainContainerMarkupId()
          // + "').on('shown', function () { $('"
          // + closeToDialogCommentTextArea.getMarkupId()
          // + "').focus(); })");
        }

        @Override
        protected void onError(final AjaxRequestTarget target, final Form<?> form)
        {
          target.add(((ToDoEditForm) form).getFeedbackPanel());
        }
      };
      final SingleButtonPanel closeButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), closeButton,
          getString("plugins.todo.button.close"));
      actionButtons.add(2, closeButtonPanel);
      addCloseToDoDialog();
    }
  }

  @SuppressWarnings("serial")
  private void addCloseToDoDialog()
  {
    closeToDoDialog = new ModalDialog(parentPage.newModalDialogId())
    {

      @Override
      public void init()
      {
        setTitle(getString("plugins.todo.closeDialog.heading"));
        init(new Form<String>(getFormId()));
        {
          final FieldsetPanel fs = gridBuilder.newFieldset(ToDoDO.class, "comment");
          closeToDoDialogCommentTextArea = new MaxLengthTextArea(TextAreaPanel.WICKET_ID,
              new PropertyModel<>(data, "comment"),
              commentTextArea.getMaxLength());
          closeToDoDialogCommentTextArea.setOutputMarkupId(true).add(AttributeModifier.replace("tabindex", "-1"));
          fs.add(new TextAreaPanel(fs.newChildId(), closeToDoDialogCommentTextArea));
          WicketUtils.setHeight(closeToDoDialogCommentTextArea, 20);
        }
      }

      /**
       * @see org.projectforge.web.dialog.ModalDialog#onCloseButtonSubmit(org.apache.wicket.ajax.AjaxRequestTarget)
       */
      @Override
      protected boolean onCloseButtonSubmit(final AjaxRequestTarget target)
      {
        getData().setStatus(ToDoStatus.CLOSED);
        parentPage.updateAndClose();
        return true;
      }
    };
    parentPage.add(closeToDoDialog);
    closeToDoDialog.setCloseButtonLabel(getString("plugins.todo.button.close")).init();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

}

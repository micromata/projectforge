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

package org.projectforge.plugins.skillmatrix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.projectforge.business.group.service.GroupService;
import org.projectforge.business.user.GroupDao;
import org.projectforge.business.user.GroupsComparator;
import org.projectforge.business.user.UserDao;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.web.common.MultiChoiceListHelper;
import org.projectforge.web.user.GroupsWicketProvider;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.RequiredMaxLengthTextField;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.wicketstuff.select2.Select2MultiChoice;

/**
 * This is the edit formular page.
 *
 * @author Werner Feder (werner.feder@t-online.de)
 */
public class TrainingEditForm extends AbstractEditForm<TrainingDO, TrainingEditPage>
{

  private static final long serialVersionUID = 359682752123823685L;

  private static final Logger log = Logger.getLogger(TrainingEditForm.class);

  @SpringBean
  private TrainingDao trainingDao;

  @SpringBean
  private SkillDao skillDao;

  @SpringBean
  private UserDao userDao;

  @SpringBean
  GroupDao groupDao;

  @SpringBean
  GroupService groupService;

  private final FormComponent<?>[] dependentFormComponents = new FormComponent[1];

  private TextField<String> valuesRating;

  private TextField<String> valuesCertificate;

  MultiChoiceListHelper<GroupDO> fullAccessGroupsListHelper, readonlyAccessGroupsListHelper;

  public static final String PARAM_TRAINING_ID = "trainingId";

  /**
   * @param parentPage
   * @param data
   */
  public TrainingEditForm(final TrainingEditPage parentPage, final TrainingDO data)
  {
    super(parentPage, data);
  }

  @SuppressWarnings("serial")
  @Override
  public void init()
  {
    super.init();

    gridBuilder.newSplitPanel(GridSize.COL50);

    // Skill
    FieldsetPanel fs = gridBuilder.newFieldset(TrainingDO.class, "skill");
    final SkillSelectPanel parentSelectPanel = new SkillSelectPanel(fs, new PropertyModel<SkillDO>(data, "skill"),
        parentPage, "skillId");
    fs.add(parentSelectPanel);
    parentSelectPanel.setRequired(true);
    parentSelectPanel.init();

    // Title of training
    fs = gridBuilder.newFieldset(TrainingDO.class, "title");
    final RequiredMaxLengthTextField titleField = new RequiredMaxLengthTextField(fs.getTextFieldId(),
        new PropertyModel<String>(data,
            "title"));
    fs.add(titleField);
    dependentFormComponents[0] = titleField;
    WicketUtils.setFocus(titleField);

    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Full access groups
      fs = gridBuilder.newFieldset(getString("plugins.teamcal.fullAccess"),
          getString("plugins.teamcal.access.groups"));
      final Collection<GroupDO> fullAccessGroups = groupService
          .getSortedGroups(getData().getFullAccessGroupIds());
      fullAccessGroupsListHelper = new MultiChoiceListHelper<GroupDO>().setComparator(new GroupsComparator())
          .setFullList(
              groupService.getSortedGroups());
      if (fullAccessGroups != null) {
        for (final GroupDO group : fullAccessGroups) {
          fullAccessGroupsListHelper.addOriginalAssignedItem(group).assignItem(group);
        }
      }
      final Select2MultiChoice<GroupDO> groups = new Select2MultiChoice<GroupDO>(fs.getSelect2MultiChoiceId(),
          new PropertyModel<Collection<GroupDO>>(this.fullAccessGroupsListHelper, "assignedItems"),
          new GroupsWicketProvider(groupService));
      groups.add(new IValidator<Object>()
      {
        @Override
        public void validate(final IValidatable<Object> validatable)
        {
          @SuppressWarnings("unchecked")
          final ArrayList<GroupDO> groups = (ArrayList<GroupDO>) validatable.getValue();
          final Collection<Integer> curUserGroupIds = userDao.getAssignedGroups(ThreadLocalUserContext.getUser());

          boolean isInUserGroups = false;
          for (final GroupDO group : groups) {
            if (curUserGroupIds.contains(group.getId()) == true) {
              isInUserGroups = true;
              break;
            }
          }
          if (isInUserGroups == false) {
            final ValidationError validationError = new ValidationError()
                .addKey("plugins.skillmatrix.skilltraining.error.nousergroup");
            validatable.error(validationError);
          }
        }

      });
      groups.setRequired(true);
      fs.addHelpIcon(getString("plugins.skillmatrix.skilltraining.fullaccess"));
      fs.add(groups);
    }

    {
      // Read-only access groups
      fs = gridBuilder.newFieldset(getString("plugins.teamcal.readonlyAccess"),
          getString("plugins.teamcal.access.groups"));
      final Collection<GroupDO> readOnlyAccessGroups = groupService
          .getSortedGroups(getData().getReadOnlyAccessGroupIds());
      readonlyAccessGroupsListHelper = new MultiChoiceListHelper<GroupDO>().setComparator(new GroupsComparator())
          .setFullList(
              groupService.getSortedGroups());
      if (readOnlyAccessGroups != null) {
        for (final GroupDO group : readOnlyAccessGroups) {
          readonlyAccessGroupsListHelper.addOriginalAssignedItem(group).assignItem(group);
        }
      }
      final Select2MultiChoice<GroupDO> groups = new Select2MultiChoice<GroupDO>(fs.getSelect2MultiChoiceId(),
          new PropertyModel<Collection<GroupDO>>(this.readonlyAccessGroupsListHelper, "assignedItems"),
          new GroupsWicketProvider(groupService));
      fs.addHelpIcon(getString("plugins.skillmatrix.skilltraining.readonlyaccess"));
      fs.add(groups);
    }

    gridBuilder.newGridPanel();
    {
      // Description
      fs = gridBuilder.newFieldset(TrainingDO.class, "description");
      fs.add(new MaxLengthTextArea(fs.getTextAreaId(), new PropertyModel<String>(data, "description"))).setAutogrow();
    }

    {
      // startDate
      fs = gridBuilder.newFieldset(TrainingDO.class, "startDate");
      fs.add(new DatePanel(fs.newChildId(), new PropertyModel<Date>(data, "startDate"),
          DatePanelSettings.get().withTargetType(
              java.sql.Date.class)));
    }

    {
      // EndDate
      fs = gridBuilder.newFieldset(TrainingDO.class, "endDate");
      fs.add(new DatePanel(fs.newChildId(), new PropertyModel<Date>(data, "endDate"),
          DatePanelSettings.get().withTargetType(
              java.sql.Date.class)));
    }

    {
      // Rating
      fs = gridBuilder.newFieldset(TrainingDO.class, "rating");
      valuesRating = new RequiredMaxLengthTextField(fs.getTextFieldId(), new PropertyModel<String>(data, "rating"));
      fs.addHelpIcon(getString("plugins.marketing.addressCampaign.values.format"));
      fs.add(valuesRating);
      fs.addAlertIcon(getString("plugins.skillmatrix.skilltraining.edit.warning.doNotChangeValues"));
      valuesRating.setRequired(false);
      valuesRating.add(new IValidator<String>()
      {
        @Override
        public void validate(final IValidatable<String> validatable)
        {
          if (TrainingDO.getValuesArray(validatable.getValue()) == null) {
            valuesRating.error(getString("plugins.skillmatrix.skilltraining.values.invalidFormat"));
          }
        }
      });
    }

    {
      // Certificate
      fs = gridBuilder.newFieldset(TrainingDO.class, "certificate");
      valuesCertificate = new RequiredMaxLengthTextField(fs.getTextFieldId(),
          new PropertyModel<String>(data, "certificate"));
      fs.addHelpIcon(getString("plugins.marketing.addressCampaign.values.format"));
      fs.add(valuesCertificate);
      fs.addAlertIcon(getString("plugins.skillmatrix.skilltraining.edit.warning.doNotChangeValues"));
      valuesCertificate.setRequired(false);
      valuesCertificate.add(new IValidator<String>()
      {
        @Override
        public void validate(final IValidatable<String> validatable)
        {
          if (TrainingDO.getValuesArray(validatable.getValue()) == null) {
            valuesCertificate.error(getString("plugins.skillmatrix.skilltraining.values.invalidFormat"));
          }
        }
      });
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditForm#getLogger()
   */
  @Override
  protected Logger getLogger()
  {
    return log;
  }

}

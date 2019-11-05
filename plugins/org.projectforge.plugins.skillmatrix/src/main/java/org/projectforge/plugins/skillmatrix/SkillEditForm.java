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

package org.projectforge.plugins.skillmatrix;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.group.service.GroupService;
import org.projectforge.business.user.GroupsComparator;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.web.common.MultiChoiceListHelper;
import org.projectforge.web.user.GroupsWicketProvider;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.bootstrap.GridType;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.RequiredMaxLengthTextField;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.select2.Select2MultiChoice;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author Billy Duong (b.duong@micromata.de)
 */
public class SkillEditForm extends AbstractEditForm<SkillDO, SkillEditPage>
{
  private static final long serialVersionUID = 7795854215943696332L;

  private static final Logger log = LoggerFactory.getLogger(SkillEditForm.class);

  @SpringBean
  private SkillDao skillDao;

  @SpringBean
  private GroupService groupService;

  @SpringBean
  private UserRightService userRights;

  protected MultiChoiceListHelper<GroupDO> fullAccessGroupsListHelper, readOnlyAccessGroupsListHelper,
      trainingAccessGroupsListHelper;

  protected SkillRight skillRight;

  private Collection<Component> ajaxTargets;

  /**
   * @param parentPage
   * @param data
   */
  public SkillEditForm(final SkillEditPage parentPage, final SkillDO data)
  {
    super(parentPage, data);
  }

  @Override
  public void init()
  {
    super.init();

    skillRight = (SkillRight) userRights.getRight(SkillmatrixPluginUserRightId.PLUGIN_SKILL_MATRIX_SKILL);
    ajaxTargets = new HashSet<>();

    gridBuilder.newSplitPanel(GridSize.COL50, GridType.CONTAINER);
    // Title of skill
    FieldsetPanel fs = gridBuilder.newFieldset(SkillDO.class, "title");
    final RequiredMaxLengthTextField titleField = new RequiredMaxLengthTextField(fs.getTextFieldId(),
        new PropertyModel<>(data,
            "title"));
    fs.add(titleField);
    WicketUtils.setFocus(titleField);

    gridBuilder.newSplitPanel(GridSize.COL50, GridType.CONTAINER);
    // Parent
    fs = gridBuilder.newFieldset(SkillDO.class, "parent");
    final SkillSelectPanel parentSelectPanel = new SkillSelectPanel(fs, new PropertyModel<>(data, "parent"),
        parentPage, "parentId")
    {
      private static final long serialVersionUID = 8355684523726816059L;

      /**
       * @see org.projectforge.plugins.skillmatrix.SkillSelectPanel#onModelSelected(org.apache.wicket.ajax.AjaxRequestTarget,
       *      org.projectforge.plugins.skillmatrix.SkillDO)
       */
      @Override
      protected void onModelSelected(final AjaxRequestTarget target, final SkillDO skillDo)
      {
        super.onModelSelected(target, skillDo);
        if (ajaxTargets != null) {
          for (final Component ajaxTarget : ajaxTargets) {
            target.add(ajaxTarget);
          }
        }
      }
    };

    fs.add(parentSelectPanel);
    parentSelectPanel.init();
    fs.getFieldset().setOutputMarkupId(true);
    if (getSkillTree().isRootNode(data)) {
      fs.setVisible(false);
    } else {
      parentSelectPanel.setRequired(true);
    }

    gridBuilder.newSplitPanel(GridSize.COL50, GridType.CONTAINER);
    // Full access groups
    fs = gridBuilder.newFieldset(getString("plugins.teamcal.fullAccess"), getString("plugins.teamcal.access.groups"));
    final Collection<GroupDO> fullAccessGroups = groupService
        .getSortedGroups(getData().getFullAccessGroupIds());
    fullAccessGroupsListHelper = new MultiChoiceListHelper<GroupDO>().setComparator(new GroupsComparator()).setFullList(
        groupService.getSortedGroups());
    if (fullAccessGroups != null) {
      for (final GroupDO group : fullAccessGroups) {
        fullAccessGroupsListHelper.addOriginalAssignedItem(group).assignItem(group);
      }
    }
    final Select2MultiChoice<GroupDO> groupsFullAccess = new Select2MultiChoice<>(fs.getSelect2MultiChoiceId(),
        new PropertyModel<>(this.fullAccessGroupsListHelper, "assignedItems"),
        new GroupsWicketProvider(groupService));
    fs.add(groupsFullAccess);

    // Full access groups inherited rights
    fs = gridBuilder.newFieldset("", getString("plugins.skillmatrix.skill.inherited"));
    @SuppressWarnings("serial")
    final DivTextPanel labelFull = new DivTextPanel(fs.newChildId(), new Model<String>()
    {
      /**
       * @see org.apache.wicket.model.Model#getObject()
       */
      @Override
      public String getObject()
      {
        if (getData().getParent() != null) {
          return getGroupnames(skillRight.getFullAccessGroupIds(getData().getParent()));
        }
        return "";
      }
    });
    fs.suppressLabelForWarning();
    fs.add(labelFull);
    ajaxTargets.add(labelFull.getLabel4Ajax());

    gridBuilder.newSplitPanel(GridSize.COL50, GridType.CONTAINER);
    {
      // Read-only access groups
      fs = gridBuilder.newFieldset(getString("plugins.teamcal.readonlyAccess"),
          getString("plugins.teamcal.access.groups"));
      final Collection<GroupDO> readOnlyAccessGroups = groupService
          .getSortedGroups(getData().getReadOnlyAccessGroupIds());
      readOnlyAccessGroupsListHelper = new MultiChoiceListHelper<GroupDO>().setComparator(new GroupsComparator())
          .setFullList(
              groupService.getSortedGroups());
      if (readOnlyAccessGroups != null) {
        for (final GroupDO group : readOnlyAccessGroups) {
          readOnlyAccessGroupsListHelper.addOriginalAssignedItem(group).assignItem(group);
        }
      }
      final Select2MultiChoice<GroupDO> groups = new Select2MultiChoice<>(fs.getSelect2MultiChoiceId(),
          new PropertyModel<>(this.readOnlyAccessGroupsListHelper, "assignedItems"),
          new GroupsWicketProvider(groupService));
      fs.add(groups);

      // Read-only access groups inherited rights
      fs = gridBuilder.newFieldset("", getString("plugins.skillmatrix.skill.inherited"));
      @SuppressWarnings("serial")
      final DivTextPanel labelReadOnly = new DivTextPanel(fs.newChildId(), new Model<String>()
      {
        /**
         * @see org.apache.wicket.model.Model#getObject()
         */
        @Override
        public String getObject()
        {
          if (getData().getParent() != null) {
            return getGroupnames(skillRight.getReadOnlyAccessGroupIds(getData().getParent()));
          }
          return "";
        }
      });
      fs.add(labelReadOnly);
      fs.suppressLabelForWarning();
      ajaxTargets.add(labelReadOnly.getLabel4Ajax());
    }

    gridBuilder.newSplitPanel(GridSize.COL50, GridType.CONTAINER);
    {
      // Training access groups
      fs = gridBuilder.newFieldset(getString("plugins.skillmatrix.skill.trainingAccess"),
          getString("plugins.teamcal.access.groups"));
      final Collection<GroupDO> trainingAccessGroups = groupService
          .getSortedGroups(getData().getTrainingGroupsIds());
      trainingAccessGroupsListHelper = new MultiChoiceListHelper<GroupDO>().setComparator(new GroupsComparator())
          .setFullList(
              groupService.getSortedGroups());
      if (trainingAccessGroups != null) {
        for (final GroupDO group : trainingAccessGroups) {
          trainingAccessGroupsListHelper.addOriginalAssignedItem(group).assignItem(group);
        }
      }
      final Select2MultiChoice<GroupDO> groups = new Select2MultiChoice<>(fs.getSelect2MultiChoiceId(),
          new PropertyModel<>(this.trainingAccessGroupsListHelper, "assignedItems"),
          new GroupsWicketProvider(groupService));
      fs.add(groups);

      // Training access groups inherited rights
      fs = gridBuilder.newFieldset("", getString("plugins.skillmatrix.skill.inherited"));
      @SuppressWarnings("serial")
      final DivTextPanel labelTraining = new DivTextPanel(fs.newChildId(), new Model<String>()
      {
        /**
         * @see org.apache.wicket.model.Model#getObject()
         */
        @Override
        public String getObject()
        {
          if (getData().getParent() != null) {
            return getGroupnames(skillRight.getTrainingAccessGroupIds(getData().getParent()));
          }
          return "";
        }
      });
      fs.add(labelTraining);
      fs.suppressLabelForWarning();
      ajaxTargets.add(labelTraining.getLabel4Ajax());
    }

    gridBuilder.newGridPanel();
    {
      // Description
      fs = gridBuilder.newFieldset(SkillDO.class, "description");
      fs.add(new MaxLengthTextArea(fs.getTextAreaId(), new PropertyModel<>(data, "description"))).setAutogrow();
    }
    {
      // Comment
      fs = gridBuilder.newFieldset(SkillDO.class, "comment");
      fs.add(new MaxLengthTextArea(fs.getTextAreaId(), new PropertyModel<>(data, "comment"))).setAutogrow();
    }
    {
      // Rateable
      gridBuilder.newFieldset(SkillDO.class, "rateable").addCheckBox(new PropertyModel<>(data, "rateable"),
          null);
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

  public SkillTree getSkillTree()
  {
    return skillDao.getSkillTree();
  }

  private String getGroupnames(final Integer[] ids)
  {
    if (ids == null || ids.length == 0) {
      return "";
    }
    String s = "";
    for (final Integer id : ids) {
      s += groupService.getGroupname(id) + ", ";
    }
    return s.substring(0, s.lastIndexOf(", "));
  }
}

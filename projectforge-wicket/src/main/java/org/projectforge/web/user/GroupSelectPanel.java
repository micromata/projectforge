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

package org.projectforge.web.user;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hibernate.Hibernate;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractSelectPanel;
import org.projectforge.web.wicket.WebConstants;
import org.projectforge.web.wicket.components.TooltipImage;

/**
 * This panel show the actual group and buttons for select/unselect group.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class GroupSelectPanel extends AbstractSelectPanel<GroupDO>
{
  private static final long serialVersionUID = -7114401036341110814L;

  private boolean defaultFormProcessing = false;

  /**
   * @param id
   * @param model
   * @param caller
   * @param selectProperty
   */
  public GroupSelectPanel(final String id, final IModel<GroupDO> model, final ISelectCallerPage caller, final String selectProperty)
  {
    super(id, model, caller, selectProperty);
  }

  /**
   * Should be called before init() method. If true, then the validation will be done after submitting.
   *
   * @param defaultFormProcessing
   */
  public void setDefaultFormProcessing(final boolean defaultFormProcessing)
  {
    this.defaultFormProcessing = defaultFormProcessing;
  }

  @Override
  @SuppressWarnings("serial")
  public GroupSelectPanel init()
  {
    super.init();
    final Label groupAsStringLabel = new Label("groupAsString", new Model<String>()
    {
      @Override
      public String getObject()
      {
        final GroupDO group = getModelObject();
        Hibernate.initialize(group);
        if (group != null) {
          return group.getName();
        }
        return "";
      }
    });
    add(groupAsStringLabel);
    final SubmitLink selectButton = new SubmitLink("select")
    {
      @Override
      public void onSubmit()
      {
        setResponsePage(new GroupListPage(caller, selectProperty));
      }
    };
    selectButton.setDefaultFormProcessing(defaultFormProcessing);
    add(selectButton);
    selectButton.add(new TooltipImage("selectHelp", WebConstants.IMAGE_GROUP_SELECT, getString("tooltip.selectGroup")));
    final SubmitLink unselectButton = new SubmitLink("unselect")
    {
      @Override
      public void onSubmit()
      {
        caller.unselect(selectProperty);
      }

      @Override
      public boolean isVisible()
      {
        return isRequired() == false && getModelObject() != null;
      }
    };
    unselectButton.setDefaultFormProcessing(defaultFormProcessing);
    add(unselectButton);
    unselectButton.add(new TooltipImage("unselectHelp", WebConstants.IMAGE_GROUP_UNSELECT, getString("tooltip.unselectGroup")));
    return this;
  }

  @Override
  public void convertInput()
  {
    setConvertedInput(getModelObject());
  }
}

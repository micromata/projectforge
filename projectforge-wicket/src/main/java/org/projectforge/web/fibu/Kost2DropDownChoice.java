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

package org.projectforge.web.fibu;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.INullAcceptingValidator;
import org.projectforge.business.fibu.KostFormatter;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public abstract class Kost2DropDownChoice extends DropDownChoice<Integer>
{
  private static final long serialVersionUID = 7812878062066655023L;

  private transient TaskTree taskTree;

  private Kost2DO kost2;

  private Integer taskId;

  private List<Kost2DO> kost2List;

  @SuppressWarnings("serial")
  public Kost2DropDownChoice(final String componentId, final Kost2DO kost2,
      final Integer taskId)
  {
    super(componentId);
    setModel(new Model<Integer>()
    {
      @Override
      public Integer getObject()
      {
        return getKost2Id();
      }

      @Override
      public void setObject(final Integer kost2Id)
      {
        setKost2Id(kost2Id);
      }
    });
    this.kost2 = kost2;
    this.taskId = taskId;
    refreshChoiceRenderer();
    setNullValid(true);
    add((INullAcceptingValidator<Integer>) validatable -> {
      final Integer value = validatable.getValue();
      if (value != null && value >= 0) {
        return;
      }
      if (CollectionUtils.isNotEmpty(kost2List) == true) {
        // Kost2 available but not selected.
        error("timesheet.error.kost2Required");
      }
    });
  }

  public boolean hasEntries()
  {
    return CollectionUtils.isNotEmpty(kost2List);
  }

  protected abstract void setKost2Id(final Integer kost2Id);

  public void setTaskId(final Integer taskId)
  {
    if (NumberHelper.isEqual(this.taskId, taskId) == true) {
      // Nothing to do.
      return;
    }
    this.taskId = taskId;
    refreshChoiceRenderer();
  }

  public Kost2DO getKost2()
  {
    return kost2;
  }

  public Integer getKost2Id()
  {
    if (kost2 == null) {
      return null;
    }
    return kost2.getId();
  }

  private void refreshChoiceRenderer()
  {
    final LabelValueChoiceRenderer<Integer> kost2ChoiceRenderer = new LabelValueChoiceRenderer<Integer>();
    kost2List = getTaskTree().getKost2List(taskId);
    if (kost2List != null && kost2List.size() == 1) {
      // Es ist genau ein Eintrag. Deshalb selektieren wir diesen auch:
      final Integer kost2Id = kost2List.get(0).getId();
      setKost2Id(kost2Id);
      this.modelChanged();
    }
    if (CollectionUtils.isEmpty(kost2List) == true) {
      setKost2Id(null); // No kost2 list given, therefore set also kost2 to null.
    } else {
      for (final Kost2DO kost2 : kost2List) {
        kost2ChoiceRenderer.addValue(kost2.getId(), KostFormatter.formatForSelection(kost2));
      }
    }
    setChoiceRenderer(kost2ChoiceRenderer);
    setChoices(kost2ChoiceRenderer.getValues());
  }

  private TaskTree getTaskTree()
  {
    if (taskTree == null) {
      taskTree = TaskTreeHelper.getTaskTree();
    }
    return taskTree;
  }
}

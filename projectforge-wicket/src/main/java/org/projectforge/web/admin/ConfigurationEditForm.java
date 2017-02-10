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

package org.projectforge.web.admin;

import java.math.BigDecimal;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.configuration.ConfigurationType;
import org.projectforge.framework.configuration.entities.ConfigurationDO;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.task.TaskSelectPanel;
import org.projectforge.web.teamcal.admin.TeamCalsProvider;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.components.TimeZonePanel;
import org.projectforge.web.wicket.converter.BigDecimalPercentConverter;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.Select2SingleChoicePanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;

import com.vaynberg.wicket.select2.Select2Choice;

public class ConfigurationEditForm extends AbstractEditForm<ConfigurationDO, ConfigurationEditPage>
{
  public ConfigurationEditForm(final ConfigurationEditPage parentPage, final ConfigurationDO data)
  {
    super(parentPage, data);
  }

  private static final long serialVersionUID = 6156899763199729949L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ConfigurationEditForm.class);

  private TaskDO task;

  private TeamCalDO calendar;

  @SpringBean
  private TaskDao taskDao;

  @SpringBean
  private TeamCalCache teamCalCache;

  @Override
  @SuppressWarnings("serial")
  protected void init()
  {
    super.init();
    gridBuilder.newGridPanel();
    {
      // Parameter name
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("administration.configuration.parameter"))
          .suppressLabelForWarning();
      fs.add(new DivTextPanel(fs.newChildId(), getString(data.getI18nKey())));
    }
    FormComponent<?> valueField = null;
    {
      // Parameter value
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("administration.configuration.value"));
      if (data.getConfigurationType() == ConfigurationType.INTEGER) {
        final TextField<Integer> textField = new TextField<Integer>(InputPanel.WICKET_ID,
            new PropertyModel<Integer>(data, "intValue"));
        fs.add(textField);
        valueField = textField;
      } else if (data.getConfigurationType() == ConfigurationType.PERCENT) {
        final MinMaxNumberField<BigDecimal> numberField = new MinMaxNumberField<BigDecimal>(InputPanel.WICKET_ID,
            new PropertyModel<BigDecimal>(data, "floatValue"), BigDecimal.ZERO, NumberHelper.HUNDRED)
        {
          /**
           * @see org.projectforge.web.wicket.components.MinMaxNumberField#getConverter(java.lang.Class)
           */
          @SuppressWarnings({ "rawtypes", "unchecked" })
          @Override
          public IConverter getConverter(final Class type)
          {
            return new BigDecimalPercentConverter(true);
          }

          ;
        };
        fs.add(numberField);
        valueField = numberField;
      } else if (data.getConfigurationType() == ConfigurationType.STRING) {
        final MaxLengthTextField textField = new MaxLengthTextField(InputPanel.WICKET_ID,
            new PropertyModel<String>(data, "stringValue"));
        if (ConfigurationParam.CALENDAR_DOMAIN.getI18nKey().equals(data.getI18nKey()) == true) {
          textField.setRequired(true);
          textField.add(new IValidator<String>()
          {
            @Override
            public void validate(final IValidatable<String> validatable)
            {
              if (Configuration.isDomainValid(validatable.getValue()) == false) {
                textField.error(getString("validation.error.generic"));
              }
            }
          });
        }
        fs.add(textField);
        valueField = textField;
      } else if (data.getConfigurationType() == ConfigurationType.TEXT) {
        final MaxLengthTextArea textArea = new MaxLengthTextArea(TextAreaPanel.WICKET_ID,
            new PropertyModel<String>(data, "stringValue"));
        fs.add(textArea);
        valueField = textArea;
      } else if (data.getConfigurationType() == ConfigurationType.BOOLEAN) {
        fs.addCheckBox(new PropertyModel<Boolean>(data, "booleanValue"), null);
      } else if (data.getConfigurationType() == ConfigurationType.TIME_ZONE) {
        final TimeZonePanel timeZonePanel = new TimeZonePanel(fs.newChildId(),
            new PropertyModel<TimeZone>(data, "timeZone"));
        fs.add(timeZonePanel);
        valueField = timeZonePanel.getTextField();
      } else if (data.getConfigurationType() == ConfigurationType.TASK) {
        if (data.getTaskId() != null) {
          this.task = taskDao.getById(data.getTaskId());
        }
        final TaskSelectPanel taskSelectPanel = new TaskSelectPanel(fs, new PropertyModel<TaskDO>(this, "task"),
            parentPage, "taskId");
        fs.add(taskSelectPanel);
        taskSelectPanel.init();
      } else if (data.getConfigurationType() == ConfigurationType.CALENDAR) {
        if (data.getCalendarId() != null) {
          this.calendar = teamCalCache.getCalendar(data.getCalendarId());
        }
        final Select2Choice<TeamCalDO> teamCalSelectSelect = new Select2Choice<>(
            Select2SingleChoicePanel.WICKET_ID,
            new PropertyModel<TeamCalDO>(this, "calendar"),
            new TeamCalsProvider(teamCalCache));
        fs.add(new Select2SingleChoicePanel<TeamCalDO>(fs.newChildId(), teamCalSelectSelect));
      } else {
        throw new UnsupportedOperationException(
            "Parameter of type '" + data.getConfigurationType() + "' not supported.");
      }
      if (valueField != null) {
        WicketUtils.setFocus(valueField);
      }
    }
    {
      // Description
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("description")).suppressLabelForWarning();
      fs.add(new DivTextPanel(fs.newChildId(),
          getString("administration.configuration.param." + data.getParameter() + ".description")));
    }
  }

  public TaskDO getTask()
  {
    return task;
  }

  public void setTask(final TaskDO task)
  {
    this.task = task;
    if (task != null) {
      data.setTaskId(task.getId());
    } else {
      data.setTaskId(null);
    }
  }

  public void setTask(final Integer taskId)
  {
    if (taskId != null) {
      setTask(taskDao.getById(taskId));
    } else {
      setTask((TaskDO) null);
    }
  }

  public TeamCalDO getCalendar()
  {
    return calendar;
  }

  public void setCalendar(final TeamCalDO calendar)
  {
    this.calendar = calendar;
    if (calendar != null) {
      data.setCalendarId(calendar.getId());
    } else {
      data.setCalendarId(null);
    }
  }

  public void setCalendar(final Integer calendarId)
  {
    if (calendarId != null) {
      setCalendar(teamCalCache.getCalendar(calendarId));
    } else {
      setCalendar((TeamCalDO) null);
    }
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}

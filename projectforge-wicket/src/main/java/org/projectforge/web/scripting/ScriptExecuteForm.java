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

package org.projectforge.web.scripting;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.scripting.ScriptDO;
import org.projectforge.business.scripting.ScriptParameter;
import org.projectforge.business.scripting.ScriptParameterType;
import org.projectforge.business.scripting.xstream.RecentScriptCalls;
import org.projectforge.business.scripting.xstream.ScriptCallData;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.user.UserDao;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.calendar.QuickSelectPanel;
import org.projectforge.web.task.TaskSelectPanel;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.wicket.AbstractStandardForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;

public class ScriptExecuteForm extends AbstractStandardForm<ScriptDO, ScriptExecutePage>
{
  private static final long serialVersionUID = -8371629527384652778L;

  @SpringBean
  private TaskDao taskDao;

  @SpringBean
  private UserDao userDao;

  protected ScriptDO data;

  protected List<ScriptParameter> scriptParameters;

  protected DatePanel[] datePanel1 = new DatePanel[5];

  protected DatePanel[] datePanel2 = new DatePanel[5];

  protected FieldsetPanel parameterFieldsets[];

  private DivPanel fieldSetsPanel;

  protected QuickSelectPanel[] quickSelectPanel = new QuickSelectPanel[5];

  protected boolean refresh;

  protected RecentScriptCalls recentScriptCalls;

  public ScriptExecuteForm(final ScriptExecutePage parentPage, final ScriptDO data)
  {
    super(parentPage);
    this.data = data;
    loadParameters();
  }

  private void loadParameters()
  {
    scriptParameters = new ArrayList<ScriptParameter>();
    addParameter(data.getParameter1Name(), data.getParameter1Type());
    addParameter(data.getParameter2Name(), data.getParameter2Type());
    addParameter(data.getParameter3Name(), data.getParameter3Type());
    addParameter(data.getParameter4Name(), data.getParameter4Type());
    addParameter(data.getParameter5Name(), data.getParameter5Type());
    addParameter(data.getParameter6Name(), data.getParameter6Type());
  }

  private void addParameter(final String parameterName, final ScriptParameterType type)
  {
    if (StringUtils.isNotBlank(parameterName) == true && type != null) {
      scriptParameters.add(new ScriptParameter(parameterName, type));
    }
  }

  private void prefillParameters()
  {
    final RecentScriptCalls recents = parentPage.getRecentScriptCalls();
    final ScriptCallData scriptCallData = recents.getScriptCallData(data.getName());
    if (scriptCallData != null && scriptCallData.getScriptParameter() != null) {
      for (final ScriptParameter recentParameter : scriptCallData.getScriptParameter()) {
        for (final ScriptParameter parameter : scriptParameters) {
          if (StringUtils.equals(parameter.getParameterName(), recentParameter.getParameterName()) == true) {
            if (parameter.getType() == recentParameter.getType()) {
              // Copy only if type matches
              if (parameter.getType() == ScriptParameterType.TASK) {
                final TaskDO task = taskDao.getById(recentParameter.getIntValue());
                parameter.setTask(task);
              } else if (parameter.getType() == ScriptParameterType.USER) {
                final PFUserDO user = userDao.getById(recentParameter.getIntValue());
                parameter.setUser(user);
              } else {
                parameter.setValue(recentParameter.getValue());
              }
            }
            break;
          } // if parameterNames are equal.
        } // for script parameters
      } // for recent parameters.
    } // if scriptCallData is given
  }

  @Override
  @SuppressWarnings("serial")
  protected void init()
  {
    super.init();
    prefillParameters();
    gridBuilder.newGridPanel();
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("scripting.script.name")).suppressLabelForWarning();
      fs.add(new DivTextPanel(fs.newChildId(), data.getName()));
    }
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("description")).suppressLabelForWarning();
      fs.add(new DivTextPanel(fs.newChildId(), data.getDescription()));
    }
    {
      addCancelButton(new Button(SingleButtonPanel.WICKET_ID, new Model<String>("cancel"))
      {
        @Override
        public final void onSubmit()
        {
          parentPage.cancel();
        }
      });
      final Button executeButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("execute"))
      {
        @Override
        public final void onSubmit()
        {
          parentPage.execute();
        }
      };
      final SingleButtonPanel executeButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), executeButton,
          getString("execute"),
          SingleButtonPanel.DEFAULT_SUBMIT);
      actionButtons.add(executeButtonPanel);
      setDefaultButton(executeButton);
    }
    refreshParametersView();
  }

  @Override
  public void onBeforeRender()
  {
    if (refresh == true) {
      data = parentPage.loadScript();
      loadParameters();
      prefillParameters();
      refreshParametersView();
      refresh = false;
    }
    super.onBeforeRender();
  }

  protected void refreshParametersView()
  {
    if (parameterFieldsets != null) {
      for (final FieldsetPanel parameterFieldset : parameterFieldsets) {
        if (parameterFieldset != null) {
          fieldSetsPanel.remove(parameterFieldset);
        }
      }
    }
    parameterFieldsets = new FieldsetPanel[scriptParameters.size()];
    int index = 0;
    boolean focusSet = false;
    fieldSetsPanel = gridBuilder.getPanel();
    for (final ScriptParameter parameter : scriptParameters) {
      final FieldsetPanel fs = gridBuilder.newFieldset(StringUtils.capitalize(parameter.getParameterName()),
          getString("scripting.script.parameter") + " " + (index + 1));
      parameterFieldsets[index] = fs;
      InputPanel inputPanel = null;
      if (parameter.getType() == ScriptParameterType.INTEGER) {
        inputPanel = fs
            .add(new TextField<Integer>(fs.getTextFieldId(), new PropertyModel<Integer>(parameter, "intValue")));
      } else if (parameter.getType() == ScriptParameterType.STRING) {
        inputPanel = fs
            .add(new TextField<String>(fs.getTextFieldId(), new PropertyModel<String>(parameter, "stringValue")));
      } else if (parameter.getType() == ScriptParameterType.DECIMAL) {
        inputPanel = fs.add(
            new TextField<BigDecimal>(fs.getTextFieldId(), new PropertyModel<BigDecimal>(parameter, "decimalValue")));
      } else if (parameter.getType() == ScriptParameterType.DATE
          || parameter.getType() == ScriptParameterType.TIME_PERIOD) {
        final String property = parameter.getType() == ScriptParameterType.TIME_PERIOD ? "timePeriodValue.fromDate"
            : "dateValue";
        datePanel1[index] = new DatePanel(fs.newChildId(), new PropertyModel<Date>(parameter, property));
        fs.add(datePanel1[index]);
        if (parameter.getType() == ScriptParameterType.TIME_PERIOD) {
          fs.add(new DivTextPanel(fs.newChildId(), " - "));
          datePanel2[index] = new DatePanel(fs.newChildId(),
              new PropertyModel<Date>(parameter, "timePeriodValue.toDate"));
          fs.add(datePanel2[index]);
          quickSelectPanel[index] = new QuickSelectPanel(fs.newChildId(), parentPage, "quickSelect:" + index,
              datePanel1[index]);
          fs.add(quickSelectPanel[index]);
          quickSelectPanel[index].init();
        }
      } else if (parameter.getType() == ScriptParameterType.TASK) {
        final TaskSelectPanel taskSelectPanel = new TaskSelectPanel(fs, new PropertyModel<TaskDO>(parameter, "task"),
            parentPage, "taskId:"
                + index);
        fs.add(taskSelectPanel);
        taskSelectPanel.init();
        taskSelectPanel.setRequired(true);
      } else if (parameter.getType() == ScriptParameterType.USER) {
        final UserSelectPanel userSelectPanel = new UserSelectPanel(fs.newChildId(),
            new PropertyModel<PFUserDO>(parameter, "user"),
            parentPage, "userId:" + index);
        fs.add(userSelectPanel);
        userSelectPanel.init();
        userSelectPanel.setRequired(true);
      } else {
        throw new UnsupportedOperationException("Parameter type: " + parameter.getType() + " not supported.");
      }
      if (focusSet == false) {
        if (inputPanel != null) {
          WicketUtils.setFocus(inputPanel.getField());
          focusSet = true;
        }
      }
      index++;
    }
    refresh = false;
  }
}

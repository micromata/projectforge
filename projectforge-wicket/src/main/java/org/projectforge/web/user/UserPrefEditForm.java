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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.user.UserPrefAreaRegistry;
import org.projectforge.business.user.UserPrefDao;
import org.projectforge.common.i18n.I18nEnum;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.UserPrefDO;
import org.projectforge.framework.persistence.user.entities.UserPrefEntryDO;
import org.projectforge.web.fibu.Kost2DropDownChoice;
import org.projectforge.web.fibu.NewCustomerSelectPanel;
import org.projectforge.web.fibu.NewProjektSelectPanel;
import org.projectforge.web.task.TaskSelectPanel;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.components.RequiredMaxLengthTextField;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class UserPrefEditForm extends AbstractEditForm<UserPrefDO, UserPrefEditPage>
{
  private static final long serialVersionUID = 6647201995353615498L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserPrefEditForm.class);

  @SpringBean
  private UserPrefDao userPrefDao;

  private boolean parameterCreated;

  protected Map<String, Component> dependentsMap = new HashMap<String, Component>();

  /**
   * @param parent Needed for i18n
   * @param bean is used for creating a PropertyModel.
   * @param propertyName is used as property name of the property model.
   * @return
   */
  public static DropDownChoice<UserPrefArea> createAreaDropdownChoice(final Component parent, final String id,
      final Object bean,
      final String propertyName, final boolean nullValid)
  {
    // DropDownChoice area
    final LabelValueChoiceRenderer<UserPrefArea> areaChoiceRenderer = createAreaChoiceRenderer(parent);
    final DropDownChoice<UserPrefArea> areaDropDownChoice = new DropDownChoice<UserPrefArea>(id,
        new PropertyModel<UserPrefArea>(bean,
            propertyName),
        areaChoiceRenderer.getValues(), areaChoiceRenderer);
    areaDropDownChoice.setNullValid(nullValid);
    return areaDropDownChoice;
  }

  public static LabelValueChoiceRenderer<UserPrefArea> createAreaChoiceRenderer(final Component parent)
  {
    // DropDownChoice area
    final LabelValueChoiceRenderer<UserPrefArea> areaChoiceRenderer = new LabelValueChoiceRenderer<UserPrefArea>();
    for (final UserPrefArea area : UserPrefAreaRegistry.instance()
        .getOrderedEntries(ThreadLocalUserContext.getLocale())) {
      areaChoiceRenderer.addValue(area, parent.getString("userPref.area." + area.getKey()));
    }
    return areaChoiceRenderer;
  }

  public UserPrefEditForm(final UserPrefEditPage parentPage, final UserPrefDO data)
  {
    super(parentPage, data);
  }

  @Override
  @SuppressWarnings("serial")
  protected void init()
  {
    super.init();

    /* GRID 50% - BLOCK */
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Name
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("userPref.name"));
      final RequiredMaxLengthTextField name = new RequiredMaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<String>(data, "name"));
      name.add(new IValidator<String>()
      {
        @Override
        public void validate(final IValidatable<String> validatable)
        {
          if (data.getAreaObject() == null) {
            return;
          }
          final String value = validatable.getValue();
          if (parentPage.userPrefDao.doesParameterNameAlreadyExist(data.getId(), data.getUser(), data.getAreaObject(),
              value)) {
            name.error(getString("userPref.error.nameDoesAlreadyExist"));
          }
        }
      });
      name.add(WicketUtils.setFocus());
      fs.add(new InputPanel(fs.newChildId(), name));
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // User
      data.setUser(ThreadLocalUserContext.getUser());
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("user")).suppressLabelForWarning();
      fs.add(new DivTextPanel(fs.newChildId(), data.getUser().getFullname()));
    }
    gridBuilder.newGridPanel();
    {
      // Area
      final FieldsetPanel fieldset = new FieldsetPanel(gridBuilder.getPanel(), getString("userPref.area"))
      {
        @Override
        public boolean isVisible()
        {
          // Show area only if given, otherwise the drop down choice for area is shown.
          return data.getAreaObject() != null;
        }
      }.suppressLabelForWarning();
      fieldset.add(new DivTextPanel(fieldset.newChildId(), new Model<String>()
      {
        @Override
        public String getObject()
        {
          if (data.getAreaObject() != null) {
            return getString(data.getAreaObject().getI18nKey());
          } else {
            return "";
          }
        }
      }));
    }
    if (isNew() == true && data.getAreaObject() == null) {
      // Area choice
      final FieldsetPanel fieldset = new FieldsetPanel(gridBuilder.getPanel(), getString("userPref.area"))
      {
        @Override
        public boolean isVisible()
        {
          // Show area only if given, otherwise the drop down choice for area is shown.
          return data.getAreaObject() == null;
        }
      };
      final LabelValueChoiceRenderer<UserPrefArea> areaChoiceRenderer = createAreaChoiceRenderer(this);
      final DropDownChoice<UserPrefArea> areaDropDownChoice = new DropDownChoice<UserPrefArea>(
          fieldset.getDropDownChoiceId(),
          new PropertyModel<UserPrefArea>(data, "areaObject"), areaChoiceRenderer.getValues(), areaChoiceRenderer)
      {
        @Override
        protected boolean wantOnSelectionChangedNotifications()
        {
          return true;
        }

        @Override
        protected void onSelectionChanged(final UserPrefArea newSelection)
        {
          if (newSelection != null && parameterCreated == false) {
            // create repeater children:
            createParameterRepeaterChildren();
          }
        }
      };
      areaDropDownChoice.setNullValid(true);
      areaDropDownChoice.setRequired(true);
      fieldset.add(areaDropDownChoice);
    } else {
      createParameterRepeaterChildren();
    }
  }

  @SuppressWarnings("serial")
  void createParameterRepeaterChildren()
  {
    if (parameterCreated == true) {
      log.error("Could not add parameters twice. Internal error. Double submit of DropDownChoice?");
      return;
    }
    parameterCreated = true;
    if (data.getAreaObject() == null) {
      log.warn("Could not create ParameterRepeater because UserPrefArea is not given.");
      return;
    }
    if (isNew() == true && data.getUserPrefEntries() == null) {
      parentPage.userPrefDao.addUserPrefParameters(data, data.getAreaObject());
    }
    if (data.getUserPrefEntries() != null) {
      for (final UserPrefEntryDO param : data.getSortedUserPrefEntries()) {
        final FieldsetPanel fs = gridBuilder
            .newFieldset(param.getI18nKey() != null ? getString(param.getI18nKey()) : param.getParameter())
            .suppressLabelForWarning();
        if (StringUtils.isNotEmpty(param.getTooltipI18nKey()) == true) {
          fs.addHelpIcon(getString(param.getTooltipI18nKey()));
        }
        parentPage.userPrefDao.updateParameterValueObject(param);
        if (PFUserDO.class.isAssignableFrom(param.getType()) == true) {
          final UserSelectPanel userSelectPanel = new UserSelectPanel(fs.newChildId(),
              new UserPrefPropertyModel<PFUserDO>(userPrefDao,
                  param, "valueAsObject"),
              parentPage, param.getParameter());
          if (data.getAreaObject() == UserPrefArea.USER_FAVORITE) {
            userSelectPanel.setShowFavorites(false);
          }
          fs.add(userSelectPanel);
          userSelectPanel.init();
        } else if (TaskDO.class.isAssignableFrom(param.getType()) == true) {
          final TaskSelectPanel taskSelectPanel = new TaskSelectPanel(fs,
              new UserPrefPropertyModel<TaskDO>(userPrefDao, param,
                  "valueAsObject"),
              parentPage, param.getParameter());
          if (data.getAreaObject() == UserPrefArea.TASK_FAVORITE) {
            taskSelectPanel.setShowFavorites(false);
          }
          fs.add(taskSelectPanel);
          taskSelectPanel.init();
        } else if (GroupDO.class.isAssignableFrom(param.getType()) == true) {
          final NewGroupSelectPanel groupSelectPanel = new NewGroupSelectPanel(fs.newChildId(),
              new UserPrefPropertyModel<GroupDO>(
                  userPrefDao, param, "valueAsObject"),
              parentPage, param.getParameter());
          fs.add(groupSelectPanel);
          groupSelectPanel.init();
        } else if (Kost2DO.class.isAssignableFrom(param.getType()) == true) {
          final UserPrefEntryDO taskParam = data.getUserPrefEntry(param.getDependsOn());
          Integer taskId = null;
          if (taskParam == null) {
            log.error(
                "Annotation for Kost2DO types should have a valid dependsOn annotation. Task param not found for: "
                    + param);
          } else {
            final TaskDO task = (TaskDO) taskParam.getValueAsObject();
            if (task != null) {
              taskId = task.getId();
            }
          }
          final Kost2DropDownChoice kost2DropDownChoice = new Kost2DropDownChoice(fs.getDropDownChoiceId(),
              (Kost2DO) param.getValueAsObject(), taskId)
          {
            @Override
            protected void setKost2Id(final Integer kost2Id)
            {
              param.setValue(String.valueOf(kost2Id));
            }
          };
          fs.add(kost2DropDownChoice);
          dependentsMap.put(param.getParameter(), kost2DropDownChoice);
        } else if (ProjektDO.class.isAssignableFrom(param.getType()) == true) {
          final NewProjektSelectPanel projektSelectPanel = new NewProjektSelectPanel(fs.newChildId(),
              new UserPrefPropertyModel<ProjektDO>(
                  userPrefDao, param, "valueAsObject"),
              parentPage, param.getParameter());
          if (data.getAreaObject() == UserPrefArea.PROJEKT_FAVORITE) {
            projektSelectPanel.setShowFavorites(false);
          }
          fs.add(projektSelectPanel);
          projektSelectPanel.init();
        } else if (KundeDO.class.isAssignableFrom(param.getType()) == true) {
          final NewCustomerSelectPanel kundeSelectPanel = new NewCustomerSelectPanel(fs.newChildId(),
              new UserPrefPropertyModel<KundeDO>(
                  userPrefDao, param, "valueAsObject"),
              null, parentPage, param.getParameter());
          if (data.getAreaObject() == UserPrefArea.KUNDE_FAVORITE) {
            kundeSelectPanel.setShowFavorites(false);
          }
          fs.add(kundeSelectPanel);
          kundeSelectPanel.init();
        } else if (param.isMultiline() == true) {
          int maxLength = param.getMaxLength();
          if (maxLength <= 0 || UserPrefEntryDO.MAX_STRING_VALUE_LENGTH < maxLength) {
            maxLength = UserPrefEntryDO.MAX_STRING_VALUE_LENGTH;
          }
          fs.add(new TextAreaPanel(fs.newChildId(), new MaxLengthTextArea(TextAreaPanel.WICKET_ID,
              new PropertyModel<String>(param, "value"), maxLength)));
        } else if (I18nEnum.class.isAssignableFrom(param.getType()) == true) {
          final LabelValueChoiceRenderer<I18nEnum> choiceRenderer = new LabelValueChoiceRenderer<I18nEnum>(this,
              (I18nEnum[]) param
                  .getType().getEnumConstants());
          final DropDownChoice<I18nEnum> choice = new DropDownChoice<I18nEnum>(fs.getDropDownChoiceId(),
              new UserPrefPropertyModel<I18nEnum>(userPrefDao, param, "valueAsObject"), choiceRenderer.getValues(),
              choiceRenderer);
          choice.setNullValid(true);
          fs.add(choice);
        } else {
          Integer maxLength = param.getMaxLength();
          if (maxLength == null || maxLength <= 0 || UserPrefEntryDO.MAX_STRING_VALUE_LENGTH < maxLength) {
            maxLength = UserPrefEntryDO.MAX_STRING_VALUE_LENGTH;
          }
          final MaxLengthTextField textField = new MaxLengthTextField(InputPanel.WICKET_ID,
              new PropertyModel<String>(param, "value"),
              maxLength);
          textField.setRequired(param.isRequired());
          fs.add(new InputPanel(fs.newChildId(), textField));
        }
      }
    }
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  void setData(final UserPrefDO data)
  {
    this.data = data;
  }

  private class UserPrefPropertyModel<T> extends PropertyModel<T>
  {
    private static final long serialVersionUID = 6644505091461853375L;

    private final UserPrefDao userPrefDao;

    private final UserPrefEntryDO userPrefEntry;

    public UserPrefPropertyModel(final UserPrefDao userPrefDao, final UserPrefEntryDO userPrefEntry,
        final String expression)
    {
      super(userPrefEntry, expression);
      this.userPrefDao = userPrefDao;
      this.userPrefEntry = userPrefEntry;
    }

    @Override
    public void setObject(final T object)
    {
      super.setObject(object);
      userPrefDao.setValueObject(userPrefEntry, object);
    };
  }
}

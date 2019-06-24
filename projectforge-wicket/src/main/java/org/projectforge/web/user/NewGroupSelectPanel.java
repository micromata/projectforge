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

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.business.user.GroupDao;
import org.projectforge.business.user.service.UserXmlPreferencesService;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.utils.RecentQueue;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractSelectPanel;
import org.projectforge.web.wicket.WebConstants;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;
import org.projectforge.web.wicket.components.TooltipImage;
import org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel;

/**
 * This panel shows the actual group.
 *
 * @author Werner Feder (werner.feder@t-online.de)
 */
public class NewGroupSelectPanel extends AbstractSelectPanel<GroupDO> implements ComponentWrapperPanel
{

  private static final long serialVersionUID = -2461567910550650952L;

  private static final String USER_PREF_KEY_RECENT_GROUPS = "GroupSelectPanel:recentGroups";

  private boolean defaultFormProcessing = false;

  @SpringBean
  private GroupDao groupDao;

  @SpringBean
  private UserXmlPreferencesService userPreferencesService;

  private RecentQueue<String> recentGroups;

  private final PFAutoCompleteTextField<GroupDO> groupTextField;

  // Only used for detecting changes:
  private GroupDO currentGoup;

  /**
   * @param id
   * @param model
   * @param caller
   * @param selectProperty
   */
  public NewGroupSelectPanel(final String id, final IModel<GroupDO> model, final ISelectCallerPage caller,
      final String selectProperty)
  {
    this(id, model, null, caller, selectProperty);
  }

  /**
   * @param id
   * @param model
   * @param caller
   * @param selectProperty
   */
  @SuppressWarnings("serial")
  public NewGroupSelectPanel(final String id, final IModel<GroupDO> model, final String label,
      final ISelectCallerPage caller,
      final String selectProperty)
  {
    super(id, model, caller, selectProperty);
    groupTextField = new PFAutoCompleteTextField<GroupDO>("groupField", getModel())
    {
      @Override
      protected List<GroupDO> getChoices(final String input)
      {
        final BaseSearchFilter filter = new BaseSearchFilter();
        filter.setSearchFields("id", "name");
        filter.setSearchString(input);
        final List<GroupDO> list = groupDao.getList(filter);
        return list;
      }

      @Override
      protected List<String> getRecentUserInputs()
      {
        return getRecentCustomers().getRecents();
      }

      @Override
      protected String formatLabel(final GroupDO group)
      {
        if (group == null) {
          return "";
        }
        return group.getName();
      }

      @Override
      protected String formatValue(final GroupDO group)
      {
        if (group == null) {
          return "";
        }
        return group.getName();
      }

      @Override
      public void convertInput()
      {
        final GroupDO group = getConverter(getType()).convertToObject(getInput(), getLocale());
        setConvertedInput(group);
        if (group != null && (currentGoup == null || group.getId() != currentGoup.getId())) {
          getRecentCustomers().append(group.getName());
        }
        currentGoup = group;
      }

      /**
       * @see org.apache.wicket.Component#getConverter(java.lang.Class)
       */

      @SuppressWarnings({ "unchecked", "rawtypes" })
      @Override
      public <C> IConverter<C> getConverter(final Class<C> type)
      {
        return new IConverter()
        {
          @Override
          public Object convertToObject(final String value, final Locale locale)
          {
            if (StringUtils.isEmpty(value) == true) {
              getModel().setObject(null);
              return null;
            }
            // ### FORMAT ###
            final GroupDO group = groupDao.getByName(value);
            if (group == null) {
              error(getString("panel.error.groupNotFound"));
            }
            getModel().setObject(group);
            return group;
          }

          @Override
          public String convertToString(final Object value, final Locale locale)
          {
            if (value == null) {
              return "";
            }
            final GroupDO group = (GroupDO) value;
            return formatLabel(group);
          }
        };
      }
    };
    currentGoup = getModelObject();
    groupTextField.enableTooltips().withLabelValue(true).withMatchContains(true).withMinChars(2).withAutoSubmit(false); //.withWidth(400);
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

  @SuppressWarnings("serial")
  @Override
  public NewGroupSelectPanel init()
  {
    super.init();
    add(groupTextField);
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
    unselectButton
        .add(new TooltipImage("unselectHelp", WebConstants.IMAGE_GROUP_UNSELECT, getString("tooltip.unselectGroup")));
    return this;
  }

  public NewGroupSelectPanel withAutoSubmit(final boolean autoSubmit)
  {
    groupTextField.withAutoSubmit(autoSubmit);
    return this;
  }

  @Override
  public Component getWrappedComponent()
  {
    return groupTextField;
  }

  @Override
  public void convertInput()
  {
    setConvertedInput(getModelObject());
  }

  @SuppressWarnings("unchecked")
  private RecentQueue<String> getRecentCustomers()
  {
    if (this.recentGroups == null) {
      this.recentGroups = (RecentQueue<String>) userPreferencesService.getEntry(USER_PREF_KEY_RECENT_GROUPS);
    }
    if (this.recentGroups == null) {
      this.recentGroups = new RecentQueue<String>();
      userPreferencesService.putEntry(USER_PREF_KEY_RECENT_GROUPS, this.recentGroups, true);
    }
    return this.recentGroups;
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getComponentOutputId()
   */
  @Override
  public String getComponentOutputId()
  {
    groupTextField.setOutputMarkupId(true);
    return groupTextField.getMarkupId();
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getFormComponent()
   */
  @Override
  public FormComponent<?> getFormComponent()
  {
    return groupTextField;
  }

  public PFAutoCompleteTextField<GroupDO> getTextField()
  {
    return groupTextField;
  }

}

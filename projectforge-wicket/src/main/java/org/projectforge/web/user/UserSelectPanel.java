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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.user.UserDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.RecentQueue;
import org.projectforge.web.CSSColor;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractSelectPanel;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;
import org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel;
import org.projectforge.web.wicket.flowlayout.IconPanel;
import org.projectforge.web.wicket.flowlayout.IconType;

/**
 * This panel shows the actual user and buttons for select/unselect user.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class UserSelectPanel extends AbstractSelectPanel<PFUserDO> implements ComponentWrapperPanel
{
  private static final long serialVersionUID = -7114401036341110814L;

  private static final String USER_PREF_KEY_RECENT_USERS = "UserSelectPanel:recentUsers";

  private boolean defaultFormProcessing = false;

  @SpringBean
  private UserDao userDao;

  private RecentQueue<String> recentUsers;

  private final PFAutoCompleteTextField<PFUserDO> userTextField;

  // Only used for detecting changes:
  private PFUserDO currentUser;

  private boolean showSelectMeButton = true;

  /**
   * @param id
   * @param model
   * @param caller
   * @param selectProperty
   */
  @SuppressWarnings("serial")
  public UserSelectPanel(final String id, final IModel<PFUserDO> model, final ISelectCallerPage caller,
      final String selectProperty)
  {
    super(id, model, caller, selectProperty);
    userTextField = new PFAutoCompleteTextField<PFUserDO>("userField", getModel())
    {
      @Override
      protected List<PFUserDO> getChoices(final String input)
      {
        final BaseSearchFilter filter = new BaseSearchFilter();
        filter.setSearchFields("username", "firstname", "lastname", "email");
        filter.setSearchString(input);
        final List<PFUserDO> list = userDao.getList(filter);
        return list;
      }

      @Override
      protected List<String> getRecentUserInputs()
      {
        return getRecentUsers().getRecents();
      }

      @Override
      protected String formatLabel(final PFUserDO user)
      {
        if (user == null) {
          return "";
        }
        return formatUser(user, true);
      }

      @Override
      protected String formatValue(final PFUserDO user)
      {
        if (user == null) {
          return "";
        }
        return formatUser(user, false);
      }

      @Override
      protected String getTooltip()
      {
        final PFUserDO user = getModel().getObject();
        if (user == null) {
          return null;
        }
        return user.getFullname() + ", " + user.getEmail();
      }

      @Override
      public void convertInput()
      {
        final PFUserDO user = getConverter(getType()).convertToObject(getInput(), getLocale());
        setConvertedInput(user);
        if (user != null && (currentUser == null || user.getId() != currentUser.getId())) {
          getRecentUsers().append(formatUser(user, true));
        }
        currentUser = user;
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
            final int ind = value.indexOf(" (");
            final String username = ind >= 0 ? value.substring(0, ind) : value;
            final PFUserDO user = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache()
                .getUser(username);
            if (user == null) {
              userTextField.error(getString("user.panel.error.usernameNotFound"));
            }
            getModel().setObject(user);
            return user;
          }

          @Override
          public String convertToString(final Object value, final Locale locale)
          {
            if (value == null) {
              return "";
            }
            final PFUserDO user = (PFUserDO) value;
            return user.getUsername();
          }

        };
      }
    };
    currentUser = getModelObject();
    userTextField.enableTooltips().withLabelValue(true).withMatchContains(true).withMinChars(2).withAutoSubmit(false)
        .withWidth(400);
  }

  /**
   * @see org.apache.wicket.markup.html.form.FormComponent#setLabel(org.apache.wicket.model.IModel)
   */
  @Override
  public UserSelectPanel setLabel(final IModel<String> labelModel)
  {
    userTextField.setLabel(labelModel);
    super.setLabel(labelModel);
    return this;
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

  /**
   * Must be called befor {@link #init()}. If false then the select-me-button is never displayed (default is true).
   *
   * @param showSelectMeButton the showSelectMeButton to set
   * @return this for chaining.
   */
  public UserSelectPanel setShowSelectMeButton(final boolean showSelectMeButton)
  {
    this.showSelectMeButton = showSelectMeButton;
    return this;
  }

  @Override
  @SuppressWarnings("serial")
  public UserSelectPanel init()
  {
    super.init();

    add(userTextField);
    final AbstractLink selectMeLink;
    if (userTextField.getSettings().isAutoSubmit() == true) {
      selectMeLink = new SubmitLink("selectMe")
      {
        @Override
        public void onSubmit()
        {
          caller.select(selectProperty, ThreadLocalUserContext.getUserId());
          markTextFieldModelAsChanged();
        }

        @Override
        public boolean isVisible()
        {
          // Is visible if no user is given or the given user is not the current logged in user.
          final PFUserDO user = UserSelectPanel.this.getModelObject();
          return showSelectMeButton == true
              && (user == null || user.getId().equals(ThreadLocalUserContext.getUser().getId()) == false);
        }
      };
      ((SubmitLink) selectMeLink).setDefaultFormProcessing(defaultFormProcessing);
    } else {
      selectMeLink = new AjaxLink<Void>("selectMe")
      {
        @Override
        public void onClick(final AjaxRequestTarget target)
        {
          UserSelectPanel.this.setModelObject(ThreadLocalUserContext.getUser());
          markTextFieldModelAsChanged();
          target.add(this, userTextField); // For hiding entry.
        }

        /**
         * @see org.apache.wicket.Component#isVisible()
         */
        @Override
        public boolean isVisible()
        {
          // Is visible if no user is given or the given user is not the current logged in user.
          final PFUserDO user = UserSelectPanel.this.getModelObject();
          return showSelectMeButton == true
              && (user == null || user.getId().equals(ThreadLocalUserContext.getUser().getId()) == false);
        }
      };
      selectMeLink.setOutputMarkupId(true);
    }
    add(selectMeLink);
    selectMeLink
        .add(new IconPanel("selectMeHelp", IconType.USER, getString("tooltip.selectMe")).setColor(CSSColor.GREEN));
    return this;
  }

  public void markTextFieldModelAsChanged()
  {
    userTextField.modelChanged();
    final PFUserDO user = getModelObject();
    if (user != null) {
      getRecentUsers().append(formatUser(user, true));
    }
  }

  public UserSelectPanel withAutoSubmit(final boolean autoSubmit)
  {
    userTextField.withAutoSubmit(autoSubmit);
    return this;
  }

  @Override
  public Component getWrappedComponent()
  {
    return userTextField;
  }

  @Override
  public void convertInput()
  {
    setConvertedInput(getModelObject());
  }

  @SuppressWarnings("unchecked")
  private RecentQueue<String> getRecentUsers()
  {
    if (this.recentUsers == null) {
      this.recentUsers = (RecentQueue<String>) UserPreferencesHelper.getEntry(USER_PREF_KEY_RECENT_USERS);
    }
    if (this.recentUsers == null) {
      this.recentUsers = new RecentQueue<String>();
      UserPreferencesHelper.putEntry(USER_PREF_KEY_RECENT_USERS, this.recentUsers, true);
    }
    return this.recentUsers;
  }

  private String formatUser(final PFUserDO user, final boolean showEmail)
  {
    if (user == null) {
      return "";
    }
    if (showEmail == true) {
      // PLEASE NOTE: If you change the format don't forget to change the format above (search ### FORMAT ###)
      return user.getUsername() + " (" + user.getFullname() + ", " + user.getEmail() + ")";
    } else {
      // PLEASE NOTE: If you change the format don't forget to change the format above (search ### FORMAT ###)
      return user.getUsername() + " (" + user.getFullname() + ")";
    }
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getComponentOutputId()
   */
  @Override
  public String getComponentOutputId()
  {
    userTextField.setOutputMarkupId(true);
    return userTextField.getMarkupId();
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getFormComponent()
   */
  @Override
  public FormComponent<?> getFormComponent()
  {
    return userTextField;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractSelectPanel#setFocus()
   */
  @Override
  public AbstractSelectPanel<PFUserDO> setFocus()
  {
    WicketUtils.setFocus(this.userTextField);
    return this;
  }
}

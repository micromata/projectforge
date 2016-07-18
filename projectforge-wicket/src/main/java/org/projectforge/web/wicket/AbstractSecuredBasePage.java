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

package org.projectforge.web.wicket;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.multitenancy.TenantRegistry;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.user.UserXmlPreferencesCache;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.i18n.MessageParam;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.LoginPage;
import org.projectforge.web.session.MySession;
import org.projectforge.web.user.ChangePasswordPage;
import org.projectforge.web.user.UserPreferencesHelper;

/**
 * All pages with required login should be derived from this page.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public abstract class AbstractSecuredBasePage extends AbstractUnsecureBasePage
{
  private static final long serialVersionUID = 3225994698301133706L;

  @SpringBean
  protected transient UserXmlPreferencesCache userXmlPreferencesCache;

  @SpringBean
  protected transient AccessChecker accessChecker;

  private transient TenantRegistry tenantRegistry;

  public AbstractSecuredBasePage(final PageParameters parameters)
  {
    super(parameters);
    if (getUser() == null) {
      throw new RestartResponseException(LoginPage.class);
    }
    if (isAccess4restrictedUsersAllowed() == false && getUser().isRestrictedUser() == true) {
      throw new RestartResponseException(ChangePasswordPage.class);
    }
  }

  /**
   * @see MySession#getUser()
   */
  @Override
  public PFUserDO getUser()
  {
    return getMySession().getUser();
  }

  /**
   * @see MySession#getUserId()
   */
  @Override
  protected Integer getUserId()
  {
    final PFUserDO user = getUser();
    return user != null ? user.getId() : null;
  }

  /**
   * Stores the given value for the current user.
   * 
   * @param key
   * @param value
   * @param persistent If true, the object will be persisted in the database.
   * @see UserXmlPreferencesCache#putEntry(Integer, String, Object, boolean)
   */
  public void putUserPrefEntry(final String key, final Object value, final boolean persistent)
  {
    UserPreferencesHelper.putEntry(key, value, persistent);
  }

  /**
   * Gets the stored user preference entry.
   * 
   * @param key
   * @return Return a persistent object with this key, if existing, or if not a volatile object with this key, if
   *         existing, otherwise null;
   * @see UserPreferencesHelper#getEntry(String)
   */
  public Object getUserPrefEntry(final String key)
  {
    return UserPreferencesHelper.getEntry(key);
  }

  /**
   * Gets the stored user preference entry.
   * 
   * @param key
   * @param expectedType Checks the type of the user pref entry (if found) and returns only this object if the object is
   *          from the expected type, otherwise null is returned.
   * @return Return a persistent object with this key, if existing, or if not a volatile object with this key, if
   *         existing, otherwise null;
   * @see UserPreferencesHelper#getEntry(String)
   */
  public Object getUserPrefEntry(final Class<?> expectedType, final String key)
  {
    return UserPreferencesHelper.getEntry(expectedType, key);
  }

  /**
   * Removes the entry under the given key.
   * 
   * @param key
   * @return The removed entry if found.
   */
  public Object removeUserPrefEntry(final String key)
  {
    return UserPreferencesHelper.removeEntry(key);
  }

  /**
   * @see UserXmlPreferencesCache#flushToDB(Integer)
   */
  public void flushUserPrefToDB()
  {
    userXmlPreferencesCache.flushToDB(getUser().getId());
  }

  /**
   * Forces to flush all user preferences to database.
   */
  public void flushAllUserPrefsToDB()
  {
    userXmlPreferencesCache.forceReload();
  }

  /**
   * AccessChecker instantiated by IOC.
   */
  public AccessChecker getAccessChecker()
  {
    return this.accessChecker;
  }

  /**
   * No it isn't.
   * 
   * @see org.projectforge.web.wicket.AbstractUnsecureBasePage#thisIsAnUnsecuredPage()
   */
  @Override
  protected void thisIsAnUnsecuredPage()
  {
    // It's OK.
    throw new UnsupportedOperationException();
  }

  /**
   * @param i18nKey key of the message
   * @param msgParams localized and non-localized message params.
   * @param params non localized message params (used if no msgParams given).
   * @return The params for the localized message if exist (prepared for using with MessageFormat), otherwise params
   *         will be returned.
   */
  public String translateParams(final String i18nKey, final MessageParam[] msgParams, final Object[] params)
  {
    if (msgParams == null) {
      return getLocalizedMessage(i18nKey, params);
    }
    final Object[] args = new Object[msgParams.length];
    for (int i = 0; i < msgParams.length; i++) {
      if (msgParams[i] == null) {
        args[i] = "";
      } else if (msgParams[i].isI18nKey() == true) {
        args[i] = getString(msgParams[i].getI18nKey());
      } else {
        args[i] = msgParams[i];
      }
    }
    return getLocalizedMessage(i18nKey, args);
  }

  /**
   * @param i18nKey key of the message
   * @param msgParams localized and non-localized message params.
   * @param params non localized message params (used if no msgParams given).
   * @return The params for the localized message if exist (prepared for using with MessageFormat), otherwise params
   *         will be returned.
   */
  public String translateParams(final UserException ex)
  {
    return translateParams(ex.getI18nKey(), ex.getMsgParams(), ex.getParams());
  }

  /**
   * Default is false, therefore the access of this page is not allowed for restricted users by default.
   * 
   * @return the access4restrictedUsersAllowed
   */
  public boolean isAccess4restrictedUsersAllowed()
  {
    return false;
  }

  /**
   * For getting caches etc.
   * 
   * @return The current tenantRegistry also for systems without tenants configured.
   */
  protected TenantRegistry getTenantRegistry()
  {
    if (tenantRegistry == null) {
      tenantRegistry = TenantRegistryMap.getInstance().getTenantRegistry();
    }
    return tenantRegistry;
  }
}

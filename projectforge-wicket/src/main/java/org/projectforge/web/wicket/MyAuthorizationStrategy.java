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

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.authorization.Action;
import org.apache.wicket.authorization.IAuthorizationStrategy;
import org.apache.wicket.authorization.IUnauthorizedComponentInstantiationListener;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.component.IRequestableComponent;
import org.projectforge.web.LoginPage;
import org.projectforge.web.mobile.AbstractSecuredMobilePage;
import org.projectforge.web.mobile.LoginMobilePage;
import org.projectforge.web.session.MySession;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class MyAuthorizationStrategy implements IAuthorizationStrategy, IUnauthorizedComponentInstantiationListener
{
  public boolean isActionAuthorized(final Component component, final Action action)
  {
    return true;
  }

  /**
   * @see org.apache.wicket.authorization.IAuthorizationStrategy#isInstantiationAuthorized(java.lang.Class)
   */
  @Override
  public <T extends IRequestableComponent> boolean isInstantiationAuthorized(final Class<T> componentClass)
  {
    if (WebPage.class.isAssignableFrom(componentClass) == true) {
      if (MySession.get().isAuthenticated() == true) {
        return true;
      }
      if (AbstractSecuredBasePage.class.isAssignableFrom(componentClass) == true
          || AbstractSecuredBasePage.class.isAssignableFrom(componentClass) == true
          || AbstractSecuredMobilePage.class.isAssignableFrom(componentClass) == true) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void onUnauthorizedInstantiation(final Component component)
  {
    if (MySession.get().isMobileUserAgent() == true) {
      throw new RestartResponseAtInterceptPageException(LoginMobilePage.class);
    } else {
      throw new RestartResponseAtInterceptPageException(LoginPage.class);
    }
  }

}

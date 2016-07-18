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

package org.projectforge.web.core.menuconfig;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.string.StringValue;
import org.projectforge.web.FavoritesMenu;
import org.projectforge.web.Menu;

/**
 * @author Dennis Hilpmann (d.hilpmann.extern@micromata.de)
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class MenuConfig extends Panel
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(MenuConfig.class);

  private static final long serialVersionUID = 7330216552642637127L;

  private final WebMarkupContainer configureLink;

  private final AbstractDefaultAjaxBehavior configureBehavior;

  /**
   * @param id
   */
  @SuppressWarnings("serial")
  public MenuConfig(final String id, final Menu menu, final FavoritesMenu favoritesMenu)
  {
    super(id);
    configureLink = new WebMarkupContainer("configureLink");
    add(configureLink);
    configureBehavior = new AbstractDefaultAjaxBehavior() {
      @Override
      protected void respond(final AjaxRequestTarget target)
      {
        final Request request = RequestCycle.get().getRequest();
        final StringValue configuration = request.getPostParameters().getParameterValue("configuration");
        final String xml = configuration.toString("");
        if (log.isDebugEnabled() == true) {
          log.debug(xml);
        }
        favoritesMenu.readFromXml(xml);
        favoritesMenu.storeAsUserPref();
      }
    };
    add(configureBehavior);
    add(new MenuConfigContent("content", menu));
  }

  @Override
  protected void onBeforeRender()
  {
    super.onBeforeRender();
    configureLink.add(AttributeModifier.replace("data-callback", "" + configureBehavior.getCallbackUrl()));
  }
}

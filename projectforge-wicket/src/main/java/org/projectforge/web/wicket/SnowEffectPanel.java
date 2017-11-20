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

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.user.UserXmlPreferencesDao;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;

/**
 * Div panel for snow effect
 *
 * @author F. Blumenstein
 */
public class SnowEffectPanel extends Panel
{

  @SpringBean
  private UserXmlPreferencesDao userXmlPreferencesDao;

  /**
   * Constructor the panel
   *
   * @param id    component id
   * @param model model for contact
   */
  public SnowEffectPanel(final String id)
  {
    super(id);
    add(new Button("remove", new Model<>("X")).add(new AjaxEventBehavior("click")
    {
      @Override
      protected void onEvent(final AjaxRequestTarget target)
      {
        setResponsePage(getPage().getPageClass(), new PageParameters().add("snowEffectEnable", false));
      }
    }));
    add(new Button("removePermanent", new Model<>("XX")).add(new AjaxEventBehavior("click")
    {
      @Override
      protected void onEvent(final AjaxRequestTarget target)
      {
        userXmlPreferencesDao.saveOrUpdate(ThreadLocalUserContext.getUserId(), "disableSnowEffectPermant", Boolean.FALSE, true);
        setResponsePage(getPage().getPageClass());
      }
    }));
  }
}
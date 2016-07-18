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

package org.projectforge.web.mobile;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.entities.AbstractBaseDO;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.address.AddressMobileEditPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.mobileflowlayout.MobileGridBuilder;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public abstract class AbstractMobileViewPage<O extends AbstractBaseDO<Integer>, D extends BaseDao<O>>
    extends AbstractSecuredMobilePage
{
  private static final long serialVersionUID = -209609626070007134L;

  protected O data;

  protected MobileGridBuilder gridBuilder;

  protected AbstractMobileViewPage(final PageParameters parameters)
  {
    super(parameters);
    final Integer id = WicketUtils.getAsInteger(parameters, AbstractEditPage.PARAMETER_KEY_ID);
    data = null;
    if (NumberHelper.greaterZero(id) == true) {
      data = getBaseDao().getById(id);
    }
    if (data == null) {
      // Create empty address for avoiding NPE...
      data = getBaseDao().newInstance();
      getLogger().error("Oups, no object id given. Can't display object.");
      setResponsePage(getListPageClass());
      return;
    }
    final RepeatingView flowfields = new RepeatingView("flowfields");
    pageContainer.add(flowfields);
    gridBuilder = new MobileGridBuilder(flowfields);
  }

  @Override
  protected void addTopRightButton()
  {
    final PageParameters params = new PageParameters();
    params.add(AbstractEditPage.PARAMETER_KEY_ID, data.getId());
    headerContainer
        .add(new JQueryButtonPanel(TOP_RIGHT_BUTTON_ID, JQueryButtonType.CHECK, AddressMobileEditPage.class, params,
            getString("edit")));
  }

  protected abstract Class<? extends AbstractSecuredMobilePage> getListPageClass();

  protected abstract D getBaseDao();

  protected abstract Logger getLogger();
}

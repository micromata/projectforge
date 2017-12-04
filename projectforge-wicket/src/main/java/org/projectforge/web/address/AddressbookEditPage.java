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

package org.projectforge.web.address;

import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.address.AddressbookDO;
import org.projectforge.business.address.AddressbookDao;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.projectforge.web.wicket.EditPage;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Florian Blumenstein
 */
@EditPage(defaultReturnPage = AddressbookListPage.class)
public class AddressbookEditPage extends AbstractEditPage<AddressbookDO, AddressbookEditForm, AddressbookDao>
    implements ISelectCallerPage
{
  private static final Logger log = LoggerFactory.getLogger(AddressbookEditPage.class);

  private static final long serialVersionUID = -3352981712347771662L;

  @SpringBean
  private AddressbookDao addressbookDao;

  /**
   * @param parameters
   * @param i18nPrefix
   */
  public AddressbookEditPage(final PageParameters parameters)
  {
    super(parameters, "addressbook");
    init();
    addTopMenuPanel();
  }

  @SuppressWarnings("serial")
  private void addTopMenuPanel()
  {
    if (isNew() == false) {
      final Integer id = form.getData().getId();
      ContentMenuEntryPanel menu = new ContentMenuEntryPanel(getNewContentMenuChildId(),
          new Link<Void>(ContentMenuEntryPanel.LINK_ID)
          {
            @Override
            public void onClick()
            {
              final AddressListPage addressListPage = new AddressListPage(
                  new PageParameters().add(AddressListPage.PARAM_ADDRESSBOOKS,
                      String.valueOf(id)));
              setResponsePage(addressListPage);
            }
          }, getString("addressbook.addresses"));
      addContentMenuEntry(menu);
    }
  }

  @Override
  public AbstractSecuredBasePage onSaveOrUpdate()
  {
    addressbookDao.setFullAccessUsers(getData(), form.fullAccessUsersListHelper.getAssignedItems());
    addressbookDao.setReadonlyAccessUsers(getData(), form.readonlyAccessUsersListHelper.getAssignedItems());
    addressbookDao.setFullAccessGroups(getData(), form.fullAccessGroupsListHelper.getAssignedItems());
    addressbookDao.setReadonlyAccessGroups(getData(), form.readonlyAccessGroupsListHelper.getAssignedItems());
    return super.onSaveOrUpdate();
  }

  /**
   * @see ISelectCallerPage#select(String, Integer)
   */
  @Override
  public void select(final String property, final Object selectedValue)
  {
    log.error("Property '" + property + "' not supported for selection.");
  }

  /**
   * @see ISelectCallerPage#unselect(String)
   */
  @Override
  public void unselect(final String property)
  {
    log.error("Property '" + property + "' not supported for unselection.");
  }

  /**
   * @see ISelectCallerPage#cancelSelection(String)
   */
  @Override
  public void cancelSelection(final String property)
  {
    log.error("Property '" + property + "' not supported for cancelling.");
  }

  /**
   * @see AbstractEditPage#getBaseDao()
   */
  @Override
  protected AddressbookDao getBaseDao()
  {
    return addressbookDao;
  }

  /**
   * @see AbstractEditPage#getLogger()
   */
  @Override
  protected Logger getLogger()
  {
    return log;
  }

  /**
   * @see AbstractEditPage#newEditForm(AbstractEditPage,
   * org.projectforge.core.AbstractBaseDO)
   */
  @Override
  protected AddressbookEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final AddressbookDO data)
  {
    return new AddressbookEditForm(this, data);
  }
}

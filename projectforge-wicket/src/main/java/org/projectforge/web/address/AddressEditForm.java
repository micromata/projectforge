/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.address.AddressDO;
import org.projectforge.framework.i18n.InternalErrorException;
import org.projectforge.web.wicket.AbstractEditForm;
import org.slf4j.Logger;

public class AddressEditForm extends AbstractEditForm<AddressDO, AddressEditPage> {
  private static final long serialVersionUID = 3881031215413525517L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AddressEditForm.class);

  public AddressEditForm(final AddressEditPage parentPage, final AddressDO data) {
    super(parentPage, data);
  }

  @Override
  protected void init() {
    throw new InternalErrorException("This edit page isn't available anymore.");
  }

  @Override
  protected Logger getLogger() {
    return log;
  }
}

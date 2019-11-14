/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.poll;

import org.projectforge.framework.persistence.api.BaseDao;
import org.springframework.stereotype.Repository;

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
@Repository
public class PollDao extends BaseDao<PollDO> {
  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[]{"owner.username", "owner.firstname",
          "owner.lastname"};

  /**
   * @param clazz
   */
  protected PollDao() {
    super(PollDO.class);
    userRightId = PollPluginUserRightId.PLUGIN_POLL;
  }

  @Override
  public String[] getAdditionalSearchFields() {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#newInstance()
   */
  @Override
  public PollDO newInstance() {
    return new PollDO();
  }

}

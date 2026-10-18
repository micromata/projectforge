/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.marketing;

import kotlin.Pair;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.framework.persistence.api.BaseDao;
import org.springframework.stereotype.Service;

/**
 * @author Kai Reinhard
 */
@Service
public class AddressCampaignDao extends BaseDao<AddressCampaignDO> {

  public AddressCampaignDao() {
    super(AddressCampaignDO.class);
    userRightId = MarketingPluginUserRightId.PLUGIN_MARKETING_ADDRESS_CAMPAIGN;
  }

  @Override
  public AddressCampaignDO newInstance() {
    return new AddressCampaignDO();
  }

  /**
   * Whether another campaign not marked as deleted already has this title, ignoring case and surrounding
   * blanks: two campaigns of the same name can't be told apart where a campaign is chosen.
   *
   * @param id The id of the campaign being saved, null for a new one (which then has no "other").
   */
  @SuppressWarnings("unchecked") // Generic varargs of the query parameters.
  public boolean doesTitleAlreadyExist(final String title, final Long id) {
    final String normalized = StringUtils.trimToNull(title);
    if (normalized == null) {
      return false;
    }
    final String query = "select c.id from AddressCampaignDO c where c.deleted = false and lower(trim(c.title)) = :title";
    final Pair<String, Object> titleParam = new Pair<>("title", normalized.toLowerCase());
    if (id == null) {
      return !persistenceService.executeQuery(query, Long.class, titleParam).isEmpty();
    }
    return !persistenceService.executeQuery(query + " and c.id <> :id", Long.class, titleParam, new Pair<>("id", id))
        .isEmpty();
  }
}

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.projectforge.business.group.service.GroupService;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.utils.NumberHelper;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;

public class GroupsWicketProvider extends ChoiceProvider<GroupDO>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(GroupsWicketProvider.class);

  private static final long serialVersionUID = 6228672635966093252L;

  transient UserGroupCache userGroupCache;

  transient GroupService groupService;

  private int pageSize = 20;

  public GroupsWicketProvider(GroupService groupService)
  {
    this.groupService = groupService;
  }

  /**
   * @param pageSize the pageSize to set
   * @return this for chaining.
   */
  public GroupsWicketProvider setPageSize(final int pageSize)
  {
    this.pageSize = pageSize;
    return this;
  }

  @Override
  public String getDisplayValue(final GroupDO choice)
  {
    return choice.getName();
  }

  @Override
  public String getIdValue(final GroupDO choice)
  {
    return String.valueOf(choice.getId());
  }

  @Override
  public void query(String term, final int page, final Response<GroupDO> response)
  {
    final Collection<GroupDO> sortedGroups = groupService.getSortedGroups();
    final List<GroupDO> result = new ArrayList<GroupDO>();
    term = term.toLowerCase();

    final int offset = page * pageSize;

    int matched = 0;
    boolean hasMore = false;
    for (final GroupDO group : sortedGroups) {
      if (result.size() == pageSize) {
        hasMore = true;
        break;
      }
      if (group.getName().toLowerCase().contains(term) == true) {
        matched++;
        if (matched > offset) {
          result.add(group);
        }
      }
    }
    response.addAll(result);
    response.setHasMore(hasMore);
  }

  @Override
  public Collection<GroupDO> toChoices(final Collection<String> ids)
  {
    final List<GroupDO> list = new ArrayList<GroupDO>();
    if (ids == null) {
      return list;
    }
    for (final String str : ids) {
      final Integer groupId = NumberHelper.parseInteger(str);
      if (groupId == null) {
        continue;
      }
      final GroupDO group = groupService.getGroup(groupId);
      if (group != null) {
        list.add(group);
      }
    }
    return list;
  }

}
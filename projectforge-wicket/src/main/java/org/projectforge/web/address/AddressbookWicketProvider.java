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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.address.AddressbookComparator;
import org.projectforge.business.address.AddressbookDO;
import org.projectforge.business.address.AddressbookDao;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.utils.NumberHelper;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;

public class AddressbookWicketProvider extends ChoiceProvider<AddressbookDO>
{
  private static final long serialVersionUID = -7219524032951522997L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AddressbookWicketProvider.class);

  private int pageSize = 20;

  private final AddressbookComparator abComparator = new AddressbookComparator();

  private AddressbookDao addressbookDao;

  public AddressbookWicketProvider(AddressbookDao addressbookDao)
  {
    this.addressbookDao = addressbookDao;
  }

  /**
   * @param abIds
   * @return
   */
  public List<String> getAddressbookNames(final String abIds)
  {
    if (StringUtils.isEmpty(abIds) == true) {
      return null;
    }
    final int[] ids = StringHelper.splitToInts(abIds, ",", false);
    final List<String> list = new ArrayList<String>();
    for (final int id : ids) {
      final AddressbookDO ab = addressbookDao.internalGetById(id);
      if (ab != null) {
        list.add(ab.getTitle());
      } else {
        log.warn("AddressbookDO with id '" + id + "' not found. abIds string was: " + abIds);
      }
    }
    return list;
  }

  /**
   * @param abIds
   * @return
   */
  public Collection<AddressbookDO> getSortedAddressbooks(final String abIds)
  {
    if (StringUtils.isEmpty(abIds) == true) {
      return null;
    }
    Collection<AddressbookDO> sortedAddressbooks = new TreeSet<AddressbookDO>(abComparator);
    final int[] ids = StringHelper.splitToInts(abIds, ",", false);
    for (final int id : ids) {
      final AddressbookDO ab = addressbookDao.internalGetById(id);
      if (ab != null) {
        sortedAddressbooks.add(ab);
      } else {
        log.warn("AddressbookDO with id '" + id + "' not found. abIds string was: " + abIds);
      }
    }
    return sortedAddressbooks;
  }

  public String getAddressbookIds(final Collection<AddressbookDO> addressbooks)
  {
    final StringBuffer buf = new StringBuffer();
    boolean first = true;
    for (final AddressbookDO ab : addressbooks) {
      if (ab.getId() != null) {
        first = StringHelper.append(buf, first, String.valueOf(ab.getId()), ",");
      }
    }
    return buf.toString();
  }

  public Collection<AddressbookDO> getSortedAddressbooks()
  {
    final Collection<AddressbookDO> allAddressbooks = getAddressbookList();
    Collection<AddressbookDO> sortedAddressbooks = new TreeSet<AddressbookDO>(abComparator);
    for (final AddressbookDO ab : allAddressbooks) {
      if (ab.isDeleted() == false) {
        sortedAddressbooks.add(ab);
      }
    }
    return sortedAddressbooks;
  }

  private Collection<AddressbookDO> getAddressbookList()
  {
    return addressbookDao.getAllAddressbooksWithFullAccess();
  }

  /**
   * @param pageSize the pageSize to set
   * @return this for chaining.
   */
  public AddressbookWicketProvider setPageSize(final int pageSize)
  {
    this.pageSize = pageSize;
    return this;
  }

  /**
   * @see TextChoiceProvider#getDisplayText(Object)
   */
  @Override
  public String getDisplayValue(final AddressbookDO choice)
  {
    return choice.getTitle();
  }

  /**
   * @see TextChoiceProvider#getId(Object)
   */
  @Override
  public String getIdValue(final AddressbookDO choice)
  {
    return String.valueOf(choice.getId());
  }

  /**
   * @see com.vaynberg.wicket.select2.ChoiceProvider#query(String, int, Response)
   */
  @Override
  public void query(String term, final int page, final Response<AddressbookDO> response)
  {
    final Collection<AddressbookDO> sortedCals = getSortedAddressbooks();
    final List<AddressbookDO> result = new ArrayList<>();
    term = term != null ? term.toLowerCase() : "";

    final int offset = page * pageSize;

    int matched = 0;
    boolean hasMore = false;
    for (final AddressbookDO ab : sortedCals) {
      if (result.size() == pageSize) {
        hasMore = true;
        break;
      }
      final String title = ab.getTitle();
      if (title != null && title.toLowerCase().contains(term) == true) {
        matched++;
        if (matched > offset) {
          result.add(ab);
        }
      }
    }
    response.addAll(result);
    response.setHasMore(hasMore);
  }

  /**
   * @see com.vaynberg.wicket.select2.ChoiceProvider#toChoices(Collection)
   */
  @Override
  public Collection<AddressbookDO> toChoices(final Collection<String> ids)
  {
    final List<AddressbookDO> list = new ArrayList<>();
    if (ids == null) {
      return list;
    }
    for (final String str : ids) {
      final Integer abId = NumberHelper.parseInteger(str);
      if (abId == null) {
        continue;
      }
      final AddressbookDO ab = addressbookDao.internalGetById(abId);
      if (ab != null) {
        list.add(ab);
      }
    }
    return list;
  }
}
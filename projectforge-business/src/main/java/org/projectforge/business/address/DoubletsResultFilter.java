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

package org.projectforge.business.address;

import org.jetbrains.annotations.NotNull;
import org.projectforge.framework.persistence.api.impl.CustomResultFilter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DoubletsResultFilter implements CustomResultFilter<AddressDO> {
  final Set<String> fullnames = new HashSet<>();
  final Set<String> doubletFullnames = new HashSet<>();
  final List<AddressDO> all = new ArrayList<>(); // Already processed addresses to add doublets.
  final Set<Integer> addedDoublets = new HashSet<>();

  @Override
  public boolean match(@NotNull List<AddressDO> list, @NotNull AddressDO element) {
    if (element.isDeleted()) {
      return false;
    }
    final String fullname = AddressDao.getNormalizedFullname(element);
    if (fullnames.contains(fullname)) {
      doubletFullnames.add(fullname);
      for (final AddressDO adr : all) {
        if (addedDoublets.contains(adr.getId())) {
          continue; // Already added.
        } else if (doubletFullnames.contains(AddressDao.getNormalizedFullname(adr))) {
          list.add(adr);
          addedDoublets.add(adr.getId()); // Mark this address as already added.
        }
      }
      return true;
    }
    all.add(element);
    fullnames.add(fullname);
    return false;
  }
}

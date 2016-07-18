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

package org.projectforge.business.ldap;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Stores DN and id (if given) in sets to detect either an object is in the set or not in later operations. This is used by
 * {@link LdapDao#createOrUpdate(SetOfAllLdapObjects, LdapObject, Object...)}.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class SetOfAllLdapObjects
{
  Set<String> setOfDNs = new HashSet<String>();

  Set<Serializable> setOfIds = new HashSet<Serializable>();

  public void add(final LdapObject< ? > obj)
  {
    setOfDNs.add(obj.getDn());
    if (obj.getId() != null) {
      setOfIds.add(obj.getId());
    }
  }

  public boolean contains(final LdapObject< ? > obj, final String dn)
  {
    if (obj.getId() != null && setOfIds.contains(obj.getId()) == true) {
      return true;
    }
    if (setOfDNs.contains(dn) == true) {
      return true;
    }
    return false;
  }
}

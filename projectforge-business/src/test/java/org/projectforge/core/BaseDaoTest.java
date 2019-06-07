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

package org.projectforge.core;

import org.junit.jupiter.api.Test;
import org.projectforge.framework.persistence.jpa.impl.HibernateSearchFilterUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BaseDaoTest
{
  @Test
  public void modifySearchString()
  {
    assertEquals("hallo*", HibernateSearchFilterUtils.modifySearchString("hallo"));
    assertEquals("hallo* ProjectForge*", HibernateSearchFilterUtils.modifySearchString("hallo ProjectForge"));
    assertEquals("ha1lo* ProjectForge*", HibernateSearchFilterUtils.modifySearchString("ha1lo ProjectForge"));
    assertEquals("k.reinhard@projectforge*", HibernateSearchFilterUtils.modifySearchString("k.reinhard@projectforge"));
    assertEquals("email:k.reinhard@projectforge",
        HibernateSearchFilterUtils.modifySearchString("email:k.reinhard@projectforge"));
    assertEquals("hallo", HibernateSearchFilterUtils.modifySearchString("'hallo"));
    assertEquals("title:hallo", HibernateSearchFilterUtils.modifySearchString("'title:hallo"));
    assertEquals("hallo* AND test* NOT hurz* OR test*",
        HibernateSearchFilterUtils.modifySearchString("hallo AND test NOT hurz OR test"));
    assertEquals("hallo* AND 2008-11-21 NOT hurz* OR test*",
        HibernateSearchFilterUtils.modifySearchString("hallo AND 2008-11-21 NOT hurz OR test"));
    assertEquals("-hallo", HibernateSearchFilterUtils.modifySearchString("-hallo"));
    assertEquals("+hallo", HibernateSearchFilterUtils.modifySearchString("+hallo"));
    assertEquals("+hallo", HibernateSearchFilterUtils.modifySearchString("+hallo"));
    assertEquals("h+a-llo", HibernateSearchFilterUtils.modifySearchString("h+a-llo"));
    assertEquals("hu-melder", HibernateSearchFilterUtils.modifySearchString("hu-melder"));
    assertEquals("*h+a-llo*", HibernateSearchFilterUtils.modifySearchString("*h+a-llo*"));
  }
}

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

package org.projectforge.plugins.memo;

import org.junit.jupiter.api.Test;
import org.projectforge.continuousdb.Table;
import org.projectforge.continuousdb.TableAttribute;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MemoTableTest
{
  @Test
  public void createTable()
  {
    assertEquals("T_PLUGIN_MEMO", new Table(MemoDO.class).getName());

    final Table table = new Table(MemoDO.class);
    assertEquals("T_PLUGIN_MEMO", table.getName());
    table.addAttributes("id");
    final TableAttribute attr = table.getAttributes().get(0);
    assertEquals("id", attr.getProperty());
    assertEquals("If id is returned then BeanHelper has returned bridged method of interface.", "pk", attr.getName());
  }
}

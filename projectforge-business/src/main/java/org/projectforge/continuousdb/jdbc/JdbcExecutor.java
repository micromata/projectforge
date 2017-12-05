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

package org.projectforge.continuousdb.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public abstract class JdbcExecutor
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JdbcExecutor.class);

  protected abstract Object execute(PreparedStatement stmt) throws SQLException;

  private final DataSource dataSource;

  public JdbcExecutor(final DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  public Object execute(final String sql, final boolean ignoreErrors, final Object... args)
  {
    Connection con = null;
    PreparedStatement stmt = null;
    Object result = null;
    try {
      try {
        con = dataSource.getConnection();
        stmt = con.prepareStatement(sql);
        if (args != null && args.length > 0) {
          for (int i = 0; i < args.length; i++) {
            stmt.setObject(i + 1, args[i]);
          }
        }
        result = execute(stmt);
        return result;
      } catch (final SQLException e) {
        if (ignoreErrors == false) {
          throw new RuntimeException(e);
        }
        log.error("Exception encountered " + e, e);
        return null;
      }
    } finally {
      boolean hasErrors = false;
      if (stmt != null) {
        try {
          stmt.close();
        } catch (final Exception e) {
          hasErrors = true;
          log.error("Exception encountered " + e, e);
        }
      }
      if (con != null) {
        try {
          con.close();
        } catch (final Exception e) {
          hasErrors = true;
          log.error("Exception encountered " + e, e);
        }
      }
      if (hasErrors == true) {
        throw new RuntimeException();
      }
    }
  }

}

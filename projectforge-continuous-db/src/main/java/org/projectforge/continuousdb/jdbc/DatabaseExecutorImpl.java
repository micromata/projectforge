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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.sql.DataSource;

import org.projectforge.continuousdb.DatabaseExecutor;
import org.projectforge.continuousdb.DatabaseResultRow;

/**
 * Using plain jdbc for executing jdbc commands. DON'T USE THIS CLASS FOR PRODUCTION! This class is only for
 * demonstration purposes, because there is no connection pooling and connections may loose!!!!!
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class DatabaseExecutorImpl implements DatabaseExecutor
{
  private DataSource dataSource;

  @Override
  public DataSource getDataSource()
  {
    return dataSource;
  }

  @Override
  public void setDataSource(final DataSource datasource)
  {
    this.dataSource = datasource;
  }

  @Override
  public void execute(final String sql, final boolean ignoreErrors)
  {
    final JdbcExecutor jdbc = new JdbcExecutor(dataSource)
    {
      @Override
      protected Object execute(final PreparedStatement stmt) throws SQLException
      {
        stmt.execute();
        return null;
      }
    };
    jdbc.execute(sql, ignoreErrors);
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<DatabaseResultRow> query(final String sql, final Object... args)
  {
    final JdbcExecutor jdbc = new JdbcExecutor(dataSource)
    {
      @Override
      protected Object execute(final PreparedStatement stmt) throws SQLException
      {
        final List<DatabaseResultRow> list = new LinkedList<DatabaseResultRow>();
        ResultSet rs = null;
        try {
          rs = stmt.executeQuery();
          while (rs.next() == true) {
            final DatabaseResultRow row = new DatabaseResultRowImpl();
            list.add(row);
            final ResultSetMetaData metaData = rs.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
              final int type = metaData.getColumnType(i);
              final String name = metaData.getColumnName(i);
              final Object value = rs.getObject(i);
              final DatabaseResultRowEntryImpl entry = new DatabaseResultRowEntryImpl(type, name, value);
              row.add(entry);
            }
          }
          return list;
        } finally {
          if (rs != null) {
            rs.close();
          }
        }
      }
    };
    final Object obj = jdbc.execute(sql, false, args);
    return (List<DatabaseResultRow>) obj;
  }

  @Override
  public int queryForInt(final String sql, final Object... args)
  {
    final JdbcExecutor jdbc = new JdbcExecutor(dataSource)
    {
      @Override
      protected Object execute(final PreparedStatement stmt) throws SQLException
      {
        ResultSet rs = null;
        try {
          rs = stmt.executeQuery();
          if (rs.next() == false) {
            throw new RuntimeException("No result set: " + sql);
          }
          return rs.getInt(1);
        } finally {
          if (rs != null) {
            rs.close();
          }
        }
      }
    };
    final Object obj = jdbc.execute(sql, false, args);
    return (Integer) obj;
  }

  @Override
  public int update(final String sql, final Object... args)
  {
    final JdbcExecutor jdbc = new JdbcExecutor(dataSource)
    {
      @Override
      protected Object execute(final PreparedStatement stmt) throws SQLException
      {
        return stmt.executeUpdate();
      }
    };
    final Object obj = jdbc.execute(sql, false, args);
    return (Integer) obj;
  }
}

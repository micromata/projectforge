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

package org.projectforge.continuousdb;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import org.apache.commons.collections.CollectionUtils;
import org.projectforge.framework.persistence.database.DatabaseUpdateService;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class SchemaGenerator
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SchemaGenerator.class);

  private final DatabaseUpdateService dao;

  private final List<Table> tables = new LinkedList<Table>();

  public SchemaGenerator(final DatabaseUpdateService dao)
  {
    this.dao = dao;
  }

  /**
   * @param tables
   * @return this for chaining.
   */
  public SchemaGenerator add(final Table... tables)
  {
    if (tables != null) {
      for (final Table table : tables) {
        this.tables.add(table);
      }
    }
    return this;
  }

  public SchemaGenerator createSchema()
  {
    prepareOneToMany();
    prepareManyToMany();
    prepareSuperTables();
    for (final Table table : tables) {
      final Table superTable = table.getSuperTable();
      if (superTable != null) {
        if (dao.doExist(superTable) == true) {
          continue;
        }
        dao.createTable(superTable);
      } else if (dao.doExist(table) == false) {
        dao.createTable(table);
      }
    }
    return this;
  }

  void prepareSuperTables()
  {
    for (final Table table : tables) {
      final Table superTable = table.getSuperTable();
      if (superTable == null) {
        continue;
      }
      // Add additional attributes:
      if (CollectionUtils.isEmpty(table.getAttributes()) == true) {
        continue;
      }
      for (final TableAttribute attr : table.getAttributes()) {
        if (superTable.getAttributeByName(attr.getName()) != null) {
          // Attribute does already exist in super class.
          continue;
        }
        superTable.addAttribute(attr);
      }
    }
  }

  void prepareOneToMany()
  {
    for (final Table table : tables) {
      final List<TableAttribute> newAttrs = new LinkedList<TableAttribute>();
      for (final TableAttribute attr : table.getAttributes()) {
        if (attr.getType().isIn(TableAttributeType.SET, TableAttributeType.LIST) == true) {
          final OrderColumn orderColumn = attr.getAnnotation(OrderColumn.class);
          if (orderColumn != null) {
            final String name = orderColumn.name().length() > 0 ? orderColumn.name() : attr.getName() + "_ORDER";
            newAttrs.add(new TableAttribute(name, TableAttributeType.INT));
          }
          final OneToMany oneToMany = attr.getAnnotation(OneToMany.class);
          if (oneToMany == null) {
            continue;
            // Nothing to be done here.
          }
          final JoinColumn joinColumn = attr.getAnnotation(JoinColumn.class);
          if (joinColumn == null) {
            log.debug("Missing JoinColumn at OneToMany property: '"
                + table.getEntityClass()
                + "."
                + attr.getProperty()
                + "'. Is OK if ManyToOne is used at the referenced entity.");
            continue;
          }
          Class<?> targetEntity = oneToMany.targetEntity();
          if (targetEntity.equals(void.class) == true) {
            targetEntity = attr.getGenericType();
          }
          if (targetEntity == null) {
            log.warn("Can't determine generic type of list or set nor targetEntity is given: '"
                + table.getEntityClass()
                + "."
                + attr.getProperty()
                + "'!");
            continue;
          }
          final Class<?> referencedEntity = targetEntity;
          final Table referencedTable = getTable(referencedEntity);
          if (referencedTable == null) {
            log.warn("Table '"
                + referencedEntity
                + "' isn't given, so can't set OneToMany column. Please add this table to SchemaGenerator, too, or add create this table manually: '"
                + table.getEntityClass()
                + "."
                + attr.getProperty()
                + "'!");
            continue;
          }
          if (referencedTable.getAttributeByName(joinColumn.name()) == null) {
            final TableAttribute otherAttr = new TableAttribute(joinColumn.name(), table.getPrimaryKey().getType())
                .setForeignTable(table)
                .setForeignAttribute(table.getPrimaryKey().getName());
            referencedTable.addAttribute(otherAttr);
          } else {
            log.debug("Attribute '"
                + referencedTable.getName()
                + "."
                + joinColumn.name()
                + "' already exit. Nothing to be done here with OneToMany annotation in '"
                + table.getEntityClass()
                + "."
                + attr.getProperty()
                + "'.");
          }
        }
      }
      table.getAttributes().addAll(newAttrs);
    }
  }

  void prepareManyToMany()
  {
    final List<Table> joinTables = new LinkedList<Table>();
    for (final Table table : tables) {
      for (final TableAttribute attr : table.getAttributes()) {
        if (attr.getType().isIn(TableAttributeType.SET, TableAttributeType.LIST) == true) {
          final ManyToMany manyToMany = attr.getAnnotation(ManyToMany.class);
          if (manyToMany == null) {
            continue;
            // Nothing to be done here.
          }
          final JoinTable joinTableAnn = attr.getAnnotation(JoinTable.class);
          if (joinTableAnn == null) {
            log.warn("Missing JoinTable at ManyToMany property: '" + table.getEntityClass() + "." + attr.getProperty()
                + "'!");
            continue;
          }
          Class<?> targetEntity = manyToMany.targetEntity();
          if (targetEntity.equals(void.class) == true) {
            targetEntity = attr.getGenericType();
          }
          if (targetEntity == null) {
            log.warn("Can't determine generic type of list or set nor targetEntity is given: '"
                + table.getEntityClass()
                + "."
                + attr.getProperty()
                + "'!");
            continue;
          }
          final Table joinTable = new Table(joinTableAnn.name());
          joinTables.add(joinTable);
          log.debug("Adding joinTable '" + joinTableAnn.name() + "'.");
          final JoinColumn[] joinColumns = joinTableAnn.joinColumns();
          addJoinColumns(joinTable, joinColumns, table, table.getEntityClass(), attr.getProperty(), "joinColumns");

          final Class<?> referencedEntity = targetEntity;
          final Table referencedTable = getTable(referencedEntity);
          if (referencedTable == null) {
            log.warn("Table '"
                + referencedEntity
                + "' isn't given, so can't set OneToMany column. Please add this table to SchemaGenerator, too, or add create this table manually: '"
                + table.getEntityClass()
                + "."
                + attr.getProperty()
                + "'!");
            continue;
          }
          final JoinColumn[] inverseJoinColumns = joinTableAnn.inverseJoinColumns();
          addJoinColumns(joinTable, inverseJoinColumns, referencedTable, table.getEntityClass(), attr.getProperty(),
              "inverseJoinColumns");
        }
      }
    }
    for (final Table joinTable : joinTables) {
      add(joinTable);
    }
  }

  private void addJoinColumns(final Table joinTable, final JoinColumn[] joinColumns, final Table targetTable,
      final Class<?> declaringEntity, final String declaringProperty, final String annotation)
  {
    if (joinColumns == null || joinColumns.length == 0) {
      log.warn(annotation + " not given in joinTable annotation: '" + declaringEntity + "." + declaringProperty + "'!");
      return;
    }
    if (joinColumns.length > 1) {
      log.warn(annotation + ".length > 1 not yet supported in joinTable annotation: '" + declaringEntity + "."
          + declaringProperty + "'!");
      return;
    }
    final JoinColumn joinColumn = joinColumns[0];
    final TableAttribute primaryKey = targetTable.getPrimaryKey();
    final TableAttribute joinAttr = new TableAttribute(joinColumn.name(), primaryKey.getType());
    joinAttr.setForeignTable(targetTable.getName()).setForeignAttribute(primaryKey.getName()).setNullable(false);
    joinTable.addAttribute(joinAttr);

  }

  /**
   * @param tables
   * @return this for chaining.
   */
  public SchemaGenerator add(final Class<?>... entities)
  {
    if (entities == null) {
      return this;
    }
    for (final Class<?> cls : entities) {
      final Table table = new Table(cls);
      final Table superTable = table.getSuperTable();
      if (superTable != null) {
        final Table exisitingSuperTable = getTable(superTable.getEntityClass());
        if (exisitingSuperTable != null) {
          table.setSuperTable(exisitingSuperTable);
        } else {
          tables.add(superTable);
          superTable.autoAddAttributes();
        }
      }
      table.autoAddAttributes();
      tables.add(table);
    }
    return this;
  }

  public Table getTable(final Class<?> entityClass)
  {
    for (final Table table : tables) {
      if (table.getEntityClass().equals(entityClass) == true) {
        return table;
      }
    }
    return null;
  }

  /**
   * @param name sql name.
   * @return
   */
  public Table getTable(final String name)
  {
    if (name == null) {
      return null;
    }
    final String lowercase = name.toLowerCase();
    for (final Table table : tables) {
      if (lowercase.equals(table.getName().toLowerCase()) == true) {
        return table;
      }
    }
    return null;
  }
}

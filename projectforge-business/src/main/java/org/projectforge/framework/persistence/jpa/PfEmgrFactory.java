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

package org.projectforge.framework.persistence.jpa;

import de.micromata.genome.db.jpa.history.api.HistoryService;
import de.micromata.genome.db.jpa.history.api.HistoryServiceManager;
import de.micromata.genome.jpa.EmgrTx;
import de.micromata.mgc.jpa.hibernatesearch.api.SearchEmgrFactory;
import org.hibernate.cfg.AvailableSettings;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * A factory for creating PfEmgr objects.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
@Component
@DependsOn("attrSchemaService")
public class PfEmgrFactory extends SearchEmgrFactory<PfEmgr>
{
  /**
   * The instance.
   */
  private static PfEmgrFactory INSTANCE;

  @Autowired
  private DataSource ds;

  //  @Value("${hibernate.dialect}")
  //  private String hibernateDialect;

  @Value("${hibernate.show_sql}")
  private boolean hibernateShowSql;

  @Value("${hibernate.format_sql}")
  private boolean hibernateFormatSql;

  @Value("${hibernate.hbm2ddl.auto}")
  private String hibernateHbm2ddlAuto;

  @Value("${hibernate.search.default.indexBase}")
  private String hibernateSearchDefaultIndexBase;

  /**
   * Gets the.
   *
   * @return the pf emgr factory
   */
  public static synchronized PfEmgrFactory get()
  {
    if (INSTANCE != null) {
      return INSTANCE;
    }
    INSTANCE = new PfEmgrFactory();
    INSTANCE.init();
    return INSTANCE;
  }

  @Override
  protected void registerEvents()
  {
    super.registerEvents();
    HistoryService historyService = HistoryServiceManager.get().getHistoryService();
    historyService.registerEmgrListener(this);
    historyService.registerStandardHistoryPropertyConverter(this);
  }

  /**
   * Instantiates a new pf emgr factory.
   */
  @PostConstruct
  public void init()
  {
    this.unitName = "org.projectforge.webapp";
    super.initialize();
    INSTANCE = this;
  }

  /**
   * {@inheritDoc}
   *
   */

  @Override
  protected PfEmgr createEmgr(EntityManager entityManager, EmgrTx<PfEmgr> emgrTx)
  {
    return new PfEmgr(entityManager, this, emgrTx);
  }

  @Override
  public String getCurrentUserId()
  {
    Integer userId = ThreadLocalUserContext.getUserId();
    if (userId != null) {
      return userId.toString();
    }
    return "anon";
  }

  @Override
  protected Map<String, Object> getInitEntityManagerFactoryProperties()
  {
    Map<String, Object> properties = new HashMap<>();
    //properties.put(AvailableSettings.DIALECT, hibernateDialect);
    properties.put(AvailableSettings.SHOW_SQL, hibernateShowSql);
    properties.put(AvailableSettings.FORMAT_SQL, hibernateFormatSql);
    properties.put(AvailableSettings.HBM2DDL_AUTO, hibernateHbm2ddlAuto);
    properties.put(AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, true);
    properties.put(AvailableSettings.AUTOCOMMIT, false);
    properties.put("hibernate.search.default.indexBase", hibernateSearchDefaultIndexBase);
    properties.put(AvailableSettings.DATASOURCE, ds);
    return properties;
  }

}

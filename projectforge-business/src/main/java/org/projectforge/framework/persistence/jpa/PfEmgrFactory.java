package org.projectforge.framework.persistence.jpa;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.hibernate.cfg.AvailableSettings;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import de.micromata.genome.db.jpa.history.api.HistoryService;
import de.micromata.genome.db.jpa.history.api.HistoryServiceManager;
import de.micromata.genome.jpa.EmgrTx;
import de.micromata.mgc.jpa.hibernatesearch.api.SearchEmgrFactory;

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
    Map<String, Object> properties = new HashMap<String, Object>();
    //properties.put(AvailableSettings.DIALECT, hibernateDialect);
    properties.put(AvailableSettings.SHOW_SQL, hibernateShowSql);
    properties.put(AvailableSettings.FORMAT_SQL, hibernateFormatSql);
    properties.put(AvailableSettings.HBM2DDL_AUTO, hibernateHbm2ddlAuto);
    properties.put(AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, true);
    properties.put("hibernate.search.default.indexBase", hibernateSearchDefaultIndexBase);
    properties.put(AvailableSettings.DATASOURCE, ds);
    return properties;
  }

}

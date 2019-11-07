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

package org.projectforge.framework.persistence.database;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.core.ReferenceByIdMarshallingStrategy;
import com.thoughtworks.xstream.core.TreeUnmarshaller;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;
import de.micromata.genome.db.jpa.xmldump.api.JpaXmlBeforePersistListener;
import de.micromata.genome.db.jpa.xmldump.api.XmlDumpRestoreContext;
import de.micromata.genome.db.jpa.xmldump.impl.JpaXmlDumpServiceImpl;
import de.micromata.genome.db.jpa.xmldump.impl.SkippUnkownElementsCollectionConverter;
import de.micromata.genome.db.jpa.xmldump.impl.XStreamRecordConverter;
import de.micromata.genome.db.jpa.xmldump.impl.XStreamReferenceByIdUnmarshaller;
import de.micromata.genome.jpa.EmgrFactory;
import de.micromata.genome.jpa.IEmgr;
import de.micromata.genome.jpa.metainf.EntityMetadata;
import de.micromata.mgc.jpa.hibernatesearch.impl.SearchEmgr;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.address.AddressbookDao;
import org.projectforge.business.multitenancy.TenantDao;
import org.projectforge.business.multitenancy.TenantService;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.teamcal.admin.TeamCalDao;
import org.projectforge.business.user.GroupDao;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserXmlPreferencesDO;
import org.projectforge.business.user.UserXmlPreferencesDao;
import org.projectforge.framework.configuration.ConfigurationDao;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.history.entities.PfHistoryMasterDO;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * PF extends to JpaXmlDumpServiceImpl.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
@Service
public class PfJpaXmlDumpServiceImpl extends JpaXmlDumpServiceImpl implements InitializingBean, PfJpaXmlDumpService {
  private static final Logger LOG = LoggerFactory.getLogger(PfJpaXmlDumpServiceImpl.class);
  /**
   * Nasty hack to avoid problems with full text indice.
   */
  public static boolean isTransaction = false;
  @Autowired
  private PfEmgrFactory emfac;
  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private UserDao userDao;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private ConfigurationDao configDao;

  @Autowired
  private TaskDao taskDao;

  @Autowired
  private TeamCalDao teamCalDao;

  @Autowired
  private UserXmlPreferencesDao userXmlPrefDao;

  @Autowired
  private TenantService tenantService;

  @Autowired
  private TenantDao tenantDao;

  @Autowired
  private AddressDao addressDao;

  @Autowired
  private AddressbookDao addressbookDao;

  public PfJpaXmlDumpServiceImpl() {
    super();

  }

  public static InputStream openCpInputStream(String path) {
    final ClassPathResource cpres = new ClassPathResource(path);
    try {
      InputStream in;
      if (path.endsWith(".gz")) {
        in = new GZIPInputStream(cpres.getInputStream());
      } else {
        in = cpres.getInputStream();
      }
      return in;
    } catch (final IOException ex) {
      LOG.error(ex.getMessage(), ex);
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
    for (JpaXmlBeforePersistListener listener : getGlobalBeforeListener()) {
      factory.autowireBean(listener);
    }
  }

  @Override
  protected List<EntityMetadata> filterSortTableEntities(List<EntityMetadata> tables) {
    tables = super.filterSortTableEntities(tables);
    // move PfHistoryMasterDO to the end of all
    List<EntityMetadata> ret = tables.stream().filter((e) -> e.getJavaType() != PfHistoryMasterDO.class)
            .collect(Collectors.toList());
    ret.add(emfac.getMetadataRepository().getEntityMetadata(PfHistoryMasterDO.class));
    return ret;
  }

  @Override
  protected void insertEntities(EmgrFactory<?> fac, XmlDumpRestoreContext ctx, List<Object> objects,
                                List<EntityMetadata> tableEnts) {
    try {
      isTransaction = true;
      super.insertEntities(fac, ctx, objects, tableEnts);
    } finally {
      isTransaction = false;
    }
    fac.runInTrans((emgr) -> {
      SearchEmgr<?> semgr = (SearchEmgr<?>) emgr;
      for (TaskDO task : emgr.selectAllAttached(TaskDO.class)) {
        semgr.getFullTextEntityManager().index(task);
      }
      return null;
    });
  }

  @Override
  protected void insertEntitiesInTrans(XmlDumpRestoreContext ctx, List<Object> objects, List<EntityMetadata> tableEnts,
                                       IEmgr<?> emgr) {

    //    SearchEmgr<?> semgr = (SearchEmgr<?>) emgr;
    //    FullTextEntityManager ftem = semgr.getFullTextEntityManager();
    //    FullTextSession fts = ftem.unwrap(FullTextSession.class);
    //    fts.setFlushMode(FlushMode.MANUAL);
    //    FlushModeType fmode = ftem.getFlushMode();
    //    semgr.getFullTextEntityManager().setFlushMode(FlushModeType.COMMIT);
    super.insertEntitiesInTrans(ctx, objects, tableEnts, emgr);

  }

  @Override
  protected <T> T createInstance(Class<T> clazz) {
    T bean = super.createInstance(clazz);

    AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
    factory.autowireBean(bean);
    return bean;
  }

  @Override
  public int restoreDb(EmgrFactory<?> fac, InputStream inputStream, RestoreMode restoreMode) {
    XStream xstream = new XStream();
    xstream.setMarshallingStrategy(new ReferenceByIdMarshallingStrategy() {
      @Override
      protected TreeUnmarshaller createUnmarshallingContext(Object root, HierarchicalStreamReader reader,
                                                            ConverterLookup converterLookup, Mapper mapper) {
        return new XStreamReferenceByIdUnmarshaller(root, reader, converterLookup, mapper);
      }
    });

    XStreamRecordConverter recorder = new XStreamRecordConverter(xstream, fac);
    xstream.registerConverter(recorder, 10);
    xstream.registerConverter(new SkippUnkownElementsCollectionConverter(xstream.getMapper()),
            XStream.PRIORITY_VERY_HIGH);

    TenantDO defaultTenant = tenantService.getDefaultTenant();

    List<Object> objects = new ArrayList<>();
    Object result = xstream.fromXML(inputStream, objects);
    objects = (List<Object>) result;
    LOG.info("Read object from xml: " + objects.size());
    List<Object> recObjects = recorder.getAllEnties();
    List<PFUserDO> users = new ArrayList<>();

    // searching default tenant
    if (defaultTenant == null) {
      LOG.info("Default tenant is null, searching in XML dump");
      for (Object o : recObjects) {
        if (o instanceof TenantDO) {
          TenantDO t = (TenantDO) o;
          if (t.isDefault()) {
            LOG.info("Found default tenant");
            defaultTenant = t;
            break;
          }
        }
      }

      if (defaultTenant == null) {
        LOG.error("Default tenant is missing");
        return 0;
      }
    }

    // set default tenant
    for (Object o : recObjects) {
      if (o instanceof DefaultBaseDO) {
        DefaultBaseDO baseObject = (DefaultBaseDO) o;
        baseObject.setTenant(defaultTenant);
        if (baseObject instanceof PFUserDO) {
          PFUserDO user = (PFUserDO) baseObject;
          users.add(user);
        }
      }
      // TODO fix duplication bug!
      //if (o instanceof TaskDO) {
      //  TaskDO tdo = (TaskDO) o;
      //  System.out.println(tdo.getParentTaskId() + " " + tdo.getTitle() + " " + tdo.getResponsibleUserId() + " " + tdo.hashCode());
      //  System.out.println(tdo.toString());
      //  System.out.println(System.identityHashCode(tdo));
      //}
      if (o instanceof UserXmlPreferencesDO) {
        UserXmlPreferencesDO pref = (UserXmlPreferencesDO) o;
        pref.setTenant(defaultTenant);
      }
    }

    XmlDumpRestoreContext ctx = createRestoreContext(fac, recObjects);
    if (restoreMode == RestoreMode.InsertAll) {
      LOG.info("Writing XML objects to database");
      insertAll(fac, ctx);
      for (PFUserDO user : users) {
        Set<TenantDO> tenantsToAssign = new HashSet<>();
        tenantsToAssign.add(defaultTenant);
        tenantDao.internalAssignTenants(user, tenantsToAssign, null, false, false);
      }
    } else {
      throw new UnsupportedOperationException("restoreMode " + restoreMode + " currently not supported");
    }
    LOG.info("Imported entities: " + objects.size());
    return objects.size();
  }

  /**
   * Create test database from xml file.
   *
   * @return
   * @deprecated Changed to sql insert script. See SetupPage.java in projectforge-wicket module.
   */
  @Override
  @Deprecated
  public int createTestDatabase() {
    LOG.warn("PfJpaXmlDumpServiceImpl.createTestDatabase() not implemented because of deprcation.");
    return -1;
  }
}

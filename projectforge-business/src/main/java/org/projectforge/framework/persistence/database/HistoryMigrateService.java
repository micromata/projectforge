package org.projectforge.framework.persistence.database;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.projectforge.framework.persistence.history.entities.PfHistoryMasterDO;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import de.micromata.genome.db.jpa.history.api.DiffEntry;
import de.micromata.genome.db.jpa.history.api.HistProp;
import de.micromata.genome.db.jpa.history.entities.EntityOpType;
import de.micromata.genome.db.jpa.history.entities.PropertyOpType;
import de.micromata.genome.db.jpa.history.impl.HistoryServiceImpl;
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrBaseDO;
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO;
import de.micromata.genome.jpa.IEmgr;
import de.micromata.genome.jpa.StdRecord;
import de.micromata.genome.jpa.metainf.JpaMetadataEntityNotFoundException;
import de.micromata.genome.util.types.Holder;

@Service
public class HistoryMigrateService
{
  private static final Logger LOG = Logger.getLogger(HistoryMigrateService.class);

  @Autowired
  private TransactionTemplate txTemplate;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private PfEmgrFactory emfac;

  /**
   * if set true, existant entries will be overwritten.
   */
  private boolean overwrite = true;
  private int migratedCount = 0;
  private int notMigratedCount = 0;
  private int foundExistantCount = 0;

  public void migrate()
  {
    try {
      run();
    } catch (final RuntimeException ex) {
      throw ex;
    } catch (final Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private String expandPropType(IEmgr<?> emgr, String propType)
  {
    if (propType.indexOf('.') != -1) {
      return propType;
    }
    try {
      String entityName = emfac.getMetadataRepository().getEntityMetaDataBySimpleClassName(propType)
          .getJavaType().getName();
      return entityName;
    } catch (JpaMetadataEntityNotFoundException ex) {
      LOG.warn("Cannot find entity type :" + propType);
      return propType;
    }
  }

  private Void run()
  {
    final long printcountEach = 100;
    final Holder<Long> counter = new Holder<>(0L);
    txTemplate.execute((status) -> {

      final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
      jdbc.query("select * from t_history_entry", (rs) -> {
        emfac.runInTrans(emgr -> {
          try {
            PfHistoryMasterDO hm = new PfHistoryMasterDO();
            Integer pk = rs.getInt("id");
            String className = rs.getString("classname");
            Integer entPk = rs.getInt("modified_id");
            Timestamp date = rs.getTimestamp("timestamp");
            String comment = rs.getString("user_comment");
            //        Integer modifiedId = rs.getInt("modified_id");
            String userName = rs.getString("username");
            if (StringUtils.isBlank(userName) == true) {
              userName = "anon";
            }
            int optyp = rs.getInt("type");
            EntityOpType opType = opTypeFrom(optyp);
            hm.setModifiedBy(userName);
            hm.setCreatedBy(userName);
            hm.setCreatedAt(new Date(date.getTime()));
            hm.setModifiedAt(new Date(date.getTime()));
            hm.setEntityOpType(opType);
            String entityName = emfac.getMetadataRepository().getEntityMetaDataBySimpleClassName(className).getJavaType().getName();
            hm.setEntityName(entityName);
            hm.setEntityId((long) entPk);
            hm.setUserComment(comment);
            hm.setTransactionId("the_" + pk);
            //        hm.setTransactionId(ObjectUtils.toString(modifiedId));
            //        LOG.info("Selected history entry");

            jdbc.query("select * from t_history_property_delta where history_id_fk = ?", new Object[] { pk }, (rcs) -> {
              //          LOG.info("Selected history property delta");
              String newVal = rcs.getString("new_value");
              String oldVal = rcs.getString("old_value");
              String propertyName = rcs.getString("property_name");
              String propertyType = rcs.getString("property_type");
              propertyType = expandPropType(emgr, propertyType);
              DiffEntry de = new DiffEntry();
              HistProp newp = new HistProp(propertyName, propertyType, newVal);
              de.setNewProp(newp);
              HistProp oldp = new HistProp(propertyName, propertyType, oldVal);
              de.setOldProp(oldp);
              de.setPropertyName(propertyName);
              de.setPropertyOpType(getPropOpTypeFrom(hm.getEntityOpType()));
              HistoryServiceImpl.putHistProp(hm, de);
            });
            if (hm.getAttributes().isEmpty() == true) {
              if (opType == EntityOpType.Insert) {
                attachInsertProperties(emgr, hm);
              } else {
                LOG.warn("t_history_entry has no t_history_property_delta: " + pk);
                ++notMigratedCount;
                return null;
              }
            }
            insertHistory(emgr, hm);
            counter.set(counter.get() + 1);
            if (counter.get() % printcountEach == 0) {
              LOG.info("History converted: " + counter.get());
            }
          } catch (SQLException | RuntimeException ex) {
            LOG.error(ex.getMessage(), ex);
            ++notMigratedCount;
          }
          return null;
        });
      });
      return null;
    });
    LOG.info("Migrated histories: " + migratedCount + "; skipped migrated: " + notMigratedCount + "; found existant: " + foundExistantCount);
    return null;
  }

  private void attachInsertProperties(IEmgr<?> emgr, PfHistoryMasterDO hm)
  {
    //    EntityMetadata ent = emfac.getMetadataRepository().findEntityMetadata(hm.getEntityName());
    // a dummy entry.
    String newVal = "";
    String oldVal = "";
    String propertyName = "";
    String propertyType = "java.lang.String";
    DiffEntry de = new DiffEntry();
    HistProp newp = new HistProp(propertyName, propertyType, newVal);
    de.setNewProp(newp);
    HistProp oldp = new HistProp(propertyName, propertyType, oldVal);
    de.setOldProp(oldp);
    de.setPropertyName(propertyName);
    de.setPropertyOpType(getPropOpTypeFrom(hm.getEntityOpType()));
    HistoryServiceImpl.putHistProp(hm, de);

  }

  private PropertyOpType getPropOpTypeFrom(EntityOpType entop)
  {
    switch (entop) {
      case Update:
        return PropertyOpType.Update;
      case Insert:
        return PropertyOpType.Insert;
      case Deleted:
        return PropertyOpType.Delete;
      default:
        throw new IllegalArgumentException("Cannot convert EntityOpType to PropertyOpType: " + entop);

    }
  }

  private EntityOpType opTypeFrom(int typ)
  {
    switch (typ) {
      case 0:
        return EntityOpType.Insert;
      case 1:
        return EntityOpType.Update;
      case 2:
        return EntityOpType.Deleted;
      default:
        throw new IllegalArgumentException("Unknown int optype: " + typ);
    }
  }

  private String hmToString(PfHistoryMasterDO hm)
  {
    return "name: " + hm.getEntityName() + "; pk: " + hm.getEntityId() + "; transactionId: " + hm.getTransactionId()
        + "; modifiedAt: " + hm.getModifiedAtString();
  }

  private void insertHistory(IEmgr<?> emgr, PfHistoryMasterDO hm)
  {
    boolean exists = checkNonExistant(emgr, hm);
    if (exists == true) {
      LOG.info("Do not import because already exists: " + hmToString(hm));
      ++notMigratedCount;
      return;
    }
    setRecVersionData(hm);
    emgr.getEntityManager().persist(hm);
    LOG.info("Insert history: " + hm);
    ++migratedCount;

  }

  @SuppressWarnings("rawtypes")
  private void setRecVersionData(PfHistoryMasterDO hm)
  {
    for (JpaTabAttrBaseDO<PfHistoryMasterDO, Long> at : hm.getAttributes().values()) {
      copyVersionData(at, hm);
      for (JpaTabAttrDataBaseDO data : at.getData()) {
        copyVersionData(data, hm);

      }
    }
  }

  private void copyVersionData(StdRecord<?> target, StdRecord<?> source)
  {
    target.setCreatedAt(source.getCreatedAt());
    target.setModifiedAt(source.getModifiedAt());
    target.setCreatedBy(source.getCreatedBy());
    target.setModifiedBy(source.getModifiedBy());
  }

  private boolean checkNonExistant(IEmgr<?> emgr, PfHistoryMasterDO hm)
  {
    List<PfHistoryMasterDO> res;

    if (StringUtils.isNotBlank(hm.getTransactionId()) == true) {
      res = emgr.selectAttached(PfHistoryMasterDO.class,
          "select e from " + PfHistoryMasterDO.class.getName()
              + " e where e.entityId = :entityId and e.entityName = :entityName and e.transactionId = :transactionId",
          "entityId", hm.getEntityId(), "entityName", hm.getEntityName(), "transactionId", hm.getTransactionId());

    } else {
      res = emgr.selectDetached(PfHistoryMasterDO.class,
          "select e from " + PfHistoryMasterDO.class.getName()
              + " e where e.entityId = :entityId and e.entityName = :entityName and e.modifiedAt = :modifiedAt",
          "entityId", hm.getEntityId(), "entityName", hm.getEntityName(), "modifiedAt", hm.getModifiedAt());
    }
    if (overwrite == false || res.isEmpty() == true) {
      return res.isEmpty() == false;
    }
    ++foundExistantCount;
    res.forEach((rec) -> emgr.deleteAttached(rec));
    return false;
  }

  public boolean isOverwrite()
  {
    return overwrite;
  }

  public void setOverwrite(boolean overwrite)
  {
    this.overwrite = overwrite;
  }

}

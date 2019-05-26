package org.projectforge.framework.persistence.history.entities;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.micromata.genome.db.jpa.xmldump.api.JpaXmlBeforePersistListener;
import de.micromata.genome.db.jpa.xmldump.api.XmlDumpRestoreContext;
import de.micromata.genome.jpa.StdRecord;
import de.micromata.genome.jpa.metainf.EntityMetadata;

/**
 * Restore History entries with new Pks.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class PfHistoryMasterXmlBeforePersistListener implements JpaXmlBeforePersistListener
{
  private static final Logger LOG = LoggerFactory.getLogger(PfHistoryMasterXmlBeforePersistListener.class);

  @Override
  public Object preparePersist(EntityMetadata entityMetadata, Object entity, XmlDumpRestoreContext ctx)
  {
    PfHistoryMasterDO pfm = (PfHistoryMasterDO) entity;
    setNewUser(pfm, ctx);
    setRefEntityPk(pfm, ctx);
    setNewCollectionRefPks(pfm, ctx);
    return null;
  }

  private void setRefEntityPk(PfHistoryMasterDO pfm, XmlDumpRestoreContext ctx)
  {
    String entn = pfm.getEntityName();
    if (StringUtils.isBlank(entn) == true) {
      LOG.warn("History entry has no entityName");
      return;
    }
    Long id = pfm.getEntityId();
    EntityMetadata entityMeta = findEntityMetaData(entn, ctx);
    if (entityMeta == null) {
      LOG.warn("EntityName is not known entity: " + entn);
      return;
    }
    Integer intid = (int) (long) id;
    Object oldp = ctx.findEntityByOldPk(intid, entityMeta.getJavaType());
    if (oldp == null) {
      LOG.info("Cannot find oldpk from entity: " + entn + ": " + intid);
      return;
    }

    Object newPk = entityMeta.getIdColumn().getGetter().get(oldp);
    Number newPkN = (Number) newPk;
    pfm.setEntityId(newPkN.longValue());
  }

  private EntityMetadata findEntityMetaData(String className, XmlDumpRestoreContext ctx)
  {
    try {
      Class<?> cls = Class.forName(className);
      return ctx.findEntityMetaData(cls);
    } catch (ClassNotFoundException ex) {
      return null;
    }
  }

  private void setNewUser(PfHistoryMasterDO pfm, XmlDumpRestoreContext ctx)
  {

    String smodby = pfm.getModifiedBy();
    if (NumberUtils.isDigits(smodby) == false) {
      return;
    }
    Integer olduserpk = Integer.parseInt(smodby);
    PFUserDO user = ctx.findEntityByOldPk(olduserpk, PFUserDO.class);
    if (user == null) {
      LOG.warn("Cannot find user with old pk: " + smodby);
      return;
    }
    Integer pk = user.getId();
    if (pk == null) {
      LOG.warn("User id is null: " + user.getUserDisplayName());
      return;
    }
    String spk = Integer.toString(pk);
    pfm.visit((rec) -> {
      StdRecord<?> srec = (StdRecord<?>) rec;
      srec.setModifiedBy(spk);
      srec.setCreatedBy(spk);
    });
  }

  private void setNewCollectionRefPks(PfHistoryMasterDO pfm, XmlDumpRestoreContext ctx)
  {
    for (String key : pfm.getAttributes().keySet()) {
      if (key.endsWith(":ov") == false && key.endsWith(":nv") == false) {
        continue;
      }
      PfHistoryAttrDO row = (PfHistoryAttrDO) pfm.getAttributeRow(key);
      translatePkRefs(row, ctx);
    }

  }

  private void translatePkRefs(PfHistoryAttrDO row, XmlDumpRestoreContext ctx)
  {
    String typeClassName = row.getPropertyTypeClass();
    EntityMetadata entitymeta = findEntityMetaData(typeClassName, ctx);
    if (entitymeta == null) {
      return;
    }
    List<Integer> ilist = parseIntList(row.getStringData());
    if (ilist == null) {
      return;
    }
    List<String> nlist = new ArrayList<>();
    for (Integer opk : ilist) {
      Object oldEnt = ctx.findEntityByOldPk(opk, entitymeta.getJavaType());
      if (oldEnt == null) {
        return;
      }
      Object newPk = entitymeta.getIdColumn().getGetter().get(oldEnt);
      if (newPk == null) {
        return;
      }
      nlist.add(newPk.toString());
    }
    String nval = StringUtils.join(nlist, ',');
    row.setStringData(nval);
  }

  private List<Integer> parseIntList(String value)
  {
    if (StringUtils.isBlank(value) == true) {
      return null;
    }
    String[] values = StringUtils.split(value, ',');
    if (values == null || values.length == 0) {
      return null;
    }
    List<Integer> ret = new ArrayList<>();
    for (String sv : values) {
      try {
        int ival = Integer.parseInt(sv);
        ret.add(ival);
      } catch (NumberFormatException ex) {
        return null;
      }
    }
    return ret;
  }
}

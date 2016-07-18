package org.projectforge.business.fibu;

import java.io.Serializable;
import java.util.List;

import org.projectforge.business.fibu.kost.Kost1DO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.KostZuweisungDO;

import de.micromata.genome.db.jpa.xmldump.api.JpaXmlBeforePersistListener;
import de.micromata.genome.db.jpa.xmldump.api.XmlDumpRestoreContext;
import de.micromata.genome.db.jpa.xmldump.impl.XmlJpaPersistService;
import de.micromata.genome.jpa.metainf.EntityMetadata;

/**
 * The listener interface for receiving abstractRechnungXmlBeforePersist events. The class that is interested in
 * processing a abstractRechnungXmlBeforePersist event implements this interface, and the object created with that class
 * is registered with a component using the component's <code>addAbstractRechnungXmlBeforePersistListener<code> method.
 * When the abstractRechnungXmlBeforePersist event occurs, that object's appropriate method is invoked.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
public class AbstractRechnungXmlBeforePersistListener implements JpaXmlBeforePersistListener
{

  @Override
  public Object preparePersist(EntityMetadata entityMetadata, Object entity, XmlDumpRestoreContext ctx)
  {
    final AbstractRechnungDO<? extends AbstractRechnungsPositionDO> rechnung = (AbstractRechnungDO<?>) entity;
    final List<? extends AbstractRechnungsPositionDO> positions = rechnung.getPositionen();
    KontoDO konto = rechnung.getKonto();
    XmlJpaPersistService persserivce = ctx.getPersistService();
    if (konto != null) {

      konto = (KontoDO) persserivce.persist(ctx, ctx.findEntityMetaData(KontoDO.class), konto);
      rechnung.setKonto(null);
    }
    rechnung.setPositionen(null); // Need to nullable positions first (otherwise insert fails).
    persserivce.store(ctx, entityMetadata, rechnung);
    final Serializable id = rechnung.getId();

    if (konto != null) {
      rechnung.setKonto(konto);
    }
    if (positions != null) {
      for (final AbstractRechnungsPositionDO pos : positions) {
        if (pos.getKostZuweisungen() != null) {
          final List<KostZuweisungDO> zuweisungen = pos.getKostZuweisungen();
          pos.setKostZuweisungen(null); // Need to nullable first (otherwise insert fails).
          persserivce.persist(ctx, pos);
          if (pos instanceof RechnungsPositionDO) {
            ((RechnungDO) rechnung).addPosition((RechnungsPositionDO) pos);
          } else {
            ((EingangsrechnungDO) rechnung).addPosition((EingangsrechnungsPositionDO) pos);
          }
          if (zuweisungen != null) {
            for (int i = 0; i < zuweisungen.size(); ++i) {
              KostZuweisungDO zuweisung = zuweisungen.get(i);
              pos.addKostZuweisung(zuweisung);
              zuweisung.setKost1((Kost1DO) persserivce.persist(ctx, zuweisung.getKost1()));
              zuweisung.setKost2((Kost2DO) persserivce.persist(ctx, zuweisung.getKost2()));
              zuweisung = (KostZuweisungDO) persserivce.persist(ctx, zuweisung);
              zuweisungen.set(i, zuweisung);
            }
          }
        }
      }
    }
    return false;
  }

}

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

package org.projectforge.business.fibu;

import de.micromata.genome.db.jpa.xmldump.api.JpaXmlBeforePersistListener;
import de.micromata.genome.db.jpa.xmldump.api.XmlDumpRestoreContext;
import de.micromata.genome.db.jpa.xmldump.impl.XmlJpaPersistService;
import de.micromata.genome.jpa.metainf.EntityMetadata;
import org.projectforge.business.fibu.kost.Kost1DO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.KostZuweisungDO;

import java.io.Serializable;
import java.util.List;

/**
 * The listener interface for receiving abstractRechnungXmlBeforePersist events. The class that is interested in
 * processing a abstractRechnungXmlBeforePersist event implements this interface, and the object created with that class
 * is registered with a component using the component's <code>addAbstractRechnungXmlBeforePersistListener<code> method.
 * When the abstractRechnungXmlBeforePersist event occurs, that object's appropriate method is invoked.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
public class AbstractRechnungXmlBeforePersistListener implements JpaXmlBeforePersistListener {

  @Override
  public Object preparePersist(EntityMetadata entityMetadata, Object entity, XmlDumpRestoreContext ctx) {
    RechnungDO rechnung = null;
    EingangsrechnungDO eingangsrechnungDO = null;
    if (entity instanceof RechnungDO) rechnung = (RechnungDO) entity;
    else eingangsrechnungDO = (EingangsrechnungDO) entity;
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
    if (rechnung != null) {
      proceedKostZuweisungen(rechnung, persserivce, ctx);
    } else if (eingangsrechnungDO != null) {
      proceedKostZuweisungen(eingangsrechnungDO, persserivce, ctx);
    }
    return false;
  }

  private void proceedKostZuweisungen(AbstractRechnungDO rechnung, XmlJpaPersistService persserivce, XmlDumpRestoreContext ctx) {
    if (rechnung.getAbstractPositionen() == null) {
      return;
    }
    for (AbstractRechnungsPositionDO position : rechnung.getAbstractPositionen()) {
      if (position.getKostZuweisungen() == null)
        return;
      final List<KostZuweisungDO> zuweisungen = position.getKostZuweisungen();
      position.setKostZuweisungen(null); // Need to nullable first (otherwise insert fails).
      persserivce.persist(ctx, position);
      rechnung.addPosition(position);
      if (zuweisungen != null) {
        for (int i = 0; i < zuweisungen.size(); ++i) {
          KostZuweisungDO zuweisung = zuweisungen.get(i);
          position.addKostZuweisung(zuweisung);
          zuweisung.setKost1((Kost1DO) persserivce.persist(ctx, zuweisung.getKost1()));
          zuweisung.setKost2((Kost2DO) persserivce.persist(ctx, zuweisung.getKost2()));
          zuweisung = (KostZuweisungDO) persserivce.persist(ctx, zuweisung);
          zuweisungen.set(i, zuweisung);
        }
      }
    }
  }
}

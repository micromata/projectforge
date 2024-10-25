/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.fibu;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.business.fibu.*;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.projectforge.web.wicket.DownloadUtils;
import org.projectforge.web.wicket.EditPage;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@EditPage(defaultReturnPage = RechnungListPage.class)
public class RechnungEditPage extends AbstractEditPage<RechnungDO, RechnungEditForm, RechnungDao> implements ISelectCallerPage {
  private static final long serialVersionUID = 2561721641251015056L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RechnungEditPage.class);

  public RechnungEditPage(final PageParameters parameters) {
    super(parameters, "fibu.rechnung");
    init();
    if (isNew()) {
      final DayHolder day = new DayHolder();
      getData().setDatum(day.getLocalDate());
      getData().setStatus(RechnungStatus.GESTELLT);
      getData().setTyp(RechnungTyp.RECHNUNG);
    } else {
      final ContentMenuEntryPanel exportMenu = new ContentMenuEntryPanel(getNewContentMenuChildId(), getString("fibu.rechnung.exportInvoice"));
      addContentMenuEntry(exportMenu);
      for (String variant : WicketSupport.get(InvoiceService.class).getTemplateVariants()) {
        String variantTitle;
        if (StringUtils.isNotBlank(variant)) {
          variantTitle = variant;
        } else {
          variantTitle = getString("default");
        }
        String title = getString("fibu.rechnung.exportInvoice") + " (" + variantTitle.replace('_', ' ') + ")";
        final ContentMenuEntryPanel menu = new ContentMenuEntryPanel(getNewContentMenuChildId(), new SubmitLink(
                ContentMenuEntryPanel.LINK_ID, form) {
          @Override
          public void onSubmit() {
            log.debug("Export invoice.");
            ByteArrayOutputStream baos = WicketSupport.get(InvoiceService.class).getInvoiceWordDocument(getData(), variant);
            if (baos != null) {
              String filename = WicketSupport.get(InvoiceService.class).getInvoiceFilename(getData());
              DownloadUtils.setDownloadTarget(baos.toByteArray(), filename);
            }
          }

        }.setDefaultFormProcessing(false), title);
        exportMenu.addSubMenuEntry(menu);
        exportMenu.addSubMenuEntry(menu);
      }
    }
    getData().recalculate(); // Muss immer gemacht werden, damit das Zahlungsziel in Tagen berechnet wird.
  }

  @Override
  public AbstractSecuredBasePage onSaveOrUpdate() {
    if (isNew() && getData().getNummer() == null && getData().getTyp() != RechnungTyp.GUTSCHRIFTSANZEIGE_DURCH_KUNDEN
            && !RechnungStatus.GEPLANT.equals(getData().getStatus())) {
      getData().setNummer(getBaseDao().getNextNumber(getData()));
    }
    return null;
  }

  @Override
  protected RechnungDao getBaseDao() {
    return WicketSupport.get(RechnungDao.class);
  }

  @Override
  protected RechnungEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final RechnungDO data) {
    return new RechnungEditForm(this, data);
  }

  @Override
  protected Logger getLogger() {
    return log;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#cloneData()
   */
  @Override
  protected void cloneData() {
    super.cloneData();
    final RechnungDO rechnung = getData();
    rechnung.setNummer(null);

    final Integer zahlungsZielInTagen = rechnung.getZahlungsZielInTagen();
    final DayHolder dayZahlungsziel = new DayHolder();
    rechnung.setDatum(dayZahlungsziel.getLocalDate());
    if (zahlungsZielInTagen != null) {
      dayZahlungsziel.add(Calendar.DAY_OF_MONTH, zahlungsZielInTagen);
    }
    rechnung.setFaelligkeit(dayZahlungsziel.getLocalDate());

    final Integer skontoInTagen = rechnung.getDiscountZahlungsZielInTagen();
    final DayHolder daySkonto = new DayHolder();
    if (skontoInTagen != null) {
      daySkonto.add(Calendar.DAY_OF_MONTH, skontoInTagen);
    }
    rechnung.setDiscountMaturity(daySkonto.getLocalDate());

    rechnung.setZahlBetrag(null);
    rechnung.setBezahlDatum(null);
    rechnung.setStatus(RechnungStatus.GESTELLT);
    final List<RechnungsPositionDO> positionen = getData().getPositionen();
    if (positionen != null) {
      rechnung.setPositionen(new ArrayList<>());
      for (final RechnungsPositionDO origPosition : positionen) {
        final RechnungsPositionDO position = origPosition.newClone();
        rechnung.addPosition(position);
      }
    }
    form.refreshPositions();
  }

  @Override
  public void cancelSelection(final String property) {
    // Do nothing.
  }

  @Override
  public void select(final String property, final Object selectedValue) {
    if ("projektId".equals(property)) {
      getBaseDao().setProjekt(getData(), (Long) selectedValue);
      form.projektSelectPanel.getTextField().modelChanged();
      if (getData().getProjektId() != null
              && getData().getProjektId() >= 0
              && getData().getKundeId() == null
              && StringUtils.isBlank(getData().getKundeText())) {
        // User has selected a project and the kunde is not set:
        final ProjektDO projekt = WicketSupport.get(ProjektDao.class).find(getData().getProjektId());
        if (projekt != null) {
          getBaseDao().setKunde(getData(), projekt.getKundeId());
          form.customerSelectPanel.getTextField().modelChanged();
        }
      }
    } else if ("kundeId".equals(property)) {
      getBaseDao().setKunde(getData(), (Long) selectedValue);
      form.customerSelectPanel.getTextField().modelChanged();
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  @Override
  public void unselect(final String property) {
    if ("projektId".equals(property)) {
      getData().setProjekt(null);
      form.projektSelectPanel.getTextField().modelChanged();
    } else if ("kundeId".equals(property)) {
      getData().setKunde(null);
      form.customerSelectPanel.getTextField().modelChanged();
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }
}

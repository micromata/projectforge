/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.*;
import org.projectforge.common.i18n.UserException;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.PFDay;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.DownloadUtils;
import org.projectforge.web.wicket.EditPage;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@EditPage(defaultReturnPage = EingangsrechnungListPage.class)
public class EingangsrechnungEditPage
    extends AbstractEditPage<EingangsrechnungDO, EingangsrechnungEditForm, EingangsrechnungDao> implements
    ISelectCallerPage {
  private static final long serialVersionUID = 6847624027377867591L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EingangsrechnungEditPage.class);

  public EingangsrechnungEditPage(final EingangsrechnungDO data) {
    super(new PageParameters(), "fibu.eingangsrechnung");
    init(data);
    getData().recalculate(); // Muss immer gemacht werden, damit das Zahlungsziel in Tagen berechnet wird.
  }

  public EingangsrechnungEditPage(final PageParameters parameters) {
    super(parameters, "fibu.eingangsrechnung");
    init();
  }

  @Override
  protected void init() {
    super.init();

    final ContentMenuEntryPanel exportInvoiceButton = new ContentMenuEntryPanel(getNewContentMenuChildId(),
        new Link<Object>("link") {
          @Override
          public void onClick() {
            EingangsrechnungEditPage.this.exportInvoiceAsXML();
          }
        }, getString("fibu.rechnung.transferExport")).setTooltip(getString("fibu.rechnung.transferExport.tootlip"));
    if (isNew()) {
      exportInvoiceButton.setVisible(false);
    }
    addContentMenuEntry(exportInvoiceButton);
  }

  private void exportInvoiceAsXML() {
    this.form.getFeedbackMessages().clear();
    final EingangsrechnungDO invoice = this.getData();

    final String filename = String.format("transfer-%s-%s.xml", invoice.getId(), DateHelper.getTimestampAsFilenameSuffix(new Date()));
    try {
      SEPATransferResult result = WicketSupport.get(SEPATransferGenerator.class).format(this.getData());

      if (!result.isSuccessful()) {
        if (result.getErrors().isEmpty()) {
          // unknown error
          this.log.error("Oups, xml has zero size. Filename: " + filename);
          this.form.addError("fibu.rechnung.transferExport.error");
          return;
        }

        String missingFieldsStr = SEPATransferResult.getMissingFields(result, invoice);
        this.form.addError(SEPATransferResult.MISSING_FIELDS_ERROR_I18N_KEY, missingFieldsStr);

        return;
      }

      DownloadUtils.setDownloadTarget(result.getXml(), filename);
    } catch (UserException e) {
      this.form.addError("error", e.getParams()[0]);
    }
  }

  @Override
  protected EingangsrechnungDao getBaseDao() {
    return WicketSupport.get(EingangsrechnungDao.class);
  }

  @Override
  protected EingangsrechnungEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage,
                                                 final EingangsrechnungDO data) {
    return new EingangsrechnungEditForm(this, data);
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
    final EingangsrechnungDO rechnung = new EingangsrechnungDO();
    rechnung.copyValuesFrom(getData(), "id", "positionen");
    final int zahlungsZielInTagen = rechnung.getZahlungsZielInTagen();
    PFDay day = PFDay.now();
    rechnung.setDatum(day.getLocalDate());
    day = day.plusDays(zahlungsZielInTagen);
    rechnung.setFaelligkeit(day.getLocalDate());
    rechnung.setBezahlDatum(null);
    rechnung.setZahlBetrag(null);
    final List<EingangsrechnungsPositionDO> positionen = getData().getPositionen();
    if (positionen != null) {
      rechnung.setPositionen(new ArrayList<>());
      for (final EingangsrechnungsPositionDO origPosition : positionen) {
        final EingangsrechnungsPositionDO position = origPosition.newClone();
        rechnung.addPosition(position);
      }
    }
    form.refreshPositions();
    setResponsePage(new EingangsrechnungEditPage(rechnung));
  }

  @Override
  public void cancelSelection(final String property) {
    // Do nothing.
  }

  @Override
  public void select(final String property, final Object selectedValue) {
    log.error("Property '" + property + "' not supported for selection.");
  }

  @Override
  public void unselect(final String property) {
    log.error("Property '" + property + "' not supported for selection.");
  }
}

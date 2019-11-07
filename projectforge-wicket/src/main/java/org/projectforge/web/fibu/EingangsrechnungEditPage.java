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

package org.projectforge.web.fibu;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EingangsrechnungDO;
import org.projectforge.business.fibu.EingangsrechnungDao;
import org.projectforge.business.fibu.EingangsrechnungsPositionDO;
import org.projectforge.business.fibu.kost.reporting.SEPATransferGenerator;
import org.projectforge.business.fibu.kost.reporting.SEPATransferResult;
import org.projectforge.common.props.PropUtils;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.DownloadUtils;
import org.projectforge.web.wicket.EditPage;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;
import org.slf4j.Logger;

@EditPage(defaultReturnPage = EingangsrechnungListPage.class)
public class EingangsrechnungEditPage
    extends AbstractEditPage<EingangsrechnungDO, EingangsrechnungEditForm, EingangsrechnungDao> implements
    ISelectCallerPage
{
  private static final long serialVersionUID = 6847624027377867591L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EingangsrechnungEditPage.class);

  @SpringBean
  private EingangsrechnungDao eingangsrechnungDao;

  @SpringBean
  private SEPATransferGenerator SEPATransferGenerator;

  public EingangsrechnungEditPage(final PageParameters parameters)
  {
    super(parameters, "fibu.eingangsrechnung");
    init();
  }

  @Override
  protected void init()
  {
    super.init();

    final ContentMenuEntryPanel exportInvoiceButton = new ContentMenuEntryPanel(getNewContentMenuChildId(),
        new Link<Object>("link")
        {
          @Override
          public void onClick()
          {
            EingangsrechnungEditPage.this.exportInvoiceAsXML();
          }
        }, getString("fibu.rechnung.transferExport")).setTooltip(getString("fibu.rechnung.transferExport.tootlip"));
    if (isNew()) {
      exportInvoiceButton.setVisible(false);
    }
    addContentMenuEntry(exportInvoiceButton);
  }

  private void exportInvoiceAsXML()
  {
    this.form.getFeedbackMessages().clear();
    final EingangsrechnungDO invoice = this.getData();

    final String filename = String.format("transfer-%s-%s.xml", invoice.getPk(), DateHelper.getTimestampAsFilenameSuffix(new Date()));
    try {
      SEPATransferResult result = this.SEPATransferGenerator.format(this.getData());

      if (result.isSuccessful() == false) {
        if (result.getErrors().isEmpty()) {
          // unknown error
          this.log.error("Oups, xml has zero size. Filename: " + filename);
          this.form.addError("fibu.rechnung.transferExport.error");
          return;
        }

        List<String> missingFields = new ArrayList<>();

        // check invoice
        for (org.projectforge.business.fibu.kost.reporting.SEPATransferGenerator.SEPATransferError error : result.getErrors().get(invoice)) {
          switch (error) {
            case SUM:
              missingFields.add(this.getString("fibu.common.brutto"));
              break;
            case BANK_TRANSFER:
              missingFields.add(this.getString(PropUtils.getI18nKey(EingangsrechnungDO.class, "paymentType")));
              break;
            case IBAN:
              missingFields.add(this.getString(PropUtils.getI18nKey(EingangsrechnungDO.class, "iban")));
              break;
            case BIC:
              missingFields.add(this.getString(PropUtils.getI18nKey(EingangsrechnungDO.class, "bic")));
              break;
            case RECEIVER:
              missingFields.add(this.getString(PropUtils.getI18nKey(EingangsrechnungDO.class, "receiver")));
              break;
            case REFERENCE:
              missingFields.add(this.getString(PropUtils.getI18nKey(EingangsrechnungDO.class, "referenz")));
              break;
          }
        }

        String missingFieldsStr = String.join(", ", missingFields);
        this.form.addError("fibu.rechnung.transferExport.error.missing", missingFieldsStr);

        return;
      }

      DownloadUtils.setDownloadTarget(result.getXml(), filename);
    } catch (UserException e) {
      this.form.addError("error", e.getParams()[0]);
    }
  }

  @Override
  protected EingangsrechnungDao getBaseDao()
  {
    return eingangsrechnungDao;
  }

  @Override
  protected EingangsrechnungEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage,
      final EingangsrechnungDO data)
  {
    return new EingangsrechnungEditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#cloneData()
   */
  @Override
  protected void cloneData()
  {
    super.cloneData();
    final EingangsrechnungDO rechnung = getData();
    final int zahlungsZielInTagen = rechnung.getZahlungsZielInTagen();
    final DayHolder day = new DayHolder();
    rechnung.setDatum(day.getSQLDate());
    day.add(Calendar.DAY_OF_MONTH, zahlungsZielInTagen);
    rechnung.setFaelligkeit(day.getSQLDate());
    rechnung.setBezahlDatum(null);
    rechnung.setZahlBetrag(null);
    final List<EingangsrechnungsPositionDO> positionen = getData().getPositionen();
    if (positionen != null) {
      rechnung.setPositionen(new ArrayList<EingangsrechnungsPositionDO>());
      for (final EingangsrechnungsPositionDO origPosition : positionen) {
        final EingangsrechnungsPositionDO position = origPosition.newClone();
        rechnung.addPosition(position);
      }
    }
    form.refreshPositions();
  }

  @Override
  public void cancelSelection(final String property)
  {
    // Do nothing.
  }

  @Override
  public void select(final String property, final Object selectedValue)
  {
    log.error("Property '" + property + "' not supported for selection.");
  }

  @Override
  public void unselect(final String property)
  {
    log.error("Property '" + property + "' not supported for selection.");
  }
}

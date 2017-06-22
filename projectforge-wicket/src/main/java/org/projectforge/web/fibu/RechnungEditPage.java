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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.InvoiceService;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.ProjektDao;
import org.projectforge.business.fibu.RechnungDO;
import org.projectforge.business.fibu.RechnungDao;
import org.projectforge.business.fibu.RechnungStatus;
import org.projectforge.business.fibu.RechnungTyp;
import org.projectforge.business.fibu.RechnungsPositionDO;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.projectforge.web.wicket.DownloadUtils;
import org.projectforge.web.wicket.EditPage;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;

@EditPage(defaultReturnPage = RechnungListPage.class)
public class RechnungEditPage extends AbstractEditPage<RechnungDO, RechnungEditForm, RechnungDao> implements ISelectCallerPage
{
  private static final long serialVersionUID = 2561721641251015056L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(RechnungEditPage.class);

  @SpringBean
  private RechnungDao rechnungDao;

  @SpringBean
  private ProjektDao projektDao;

  @SpringBean
  private InvoiceService invoiceService;

  public RechnungEditPage(final PageParameters parameters)
  {
    super(parameters, "fibu.rechnung");
    init();
    if (isNew() == true) {
      final DayHolder day = new DayHolder();
      getData().setDatum(day.getSQLDate());
      getData().setStatus(RechnungStatus.GESTELLT);
      getData().setTyp(RechnungTyp.RECHNUNG);
    }
    getData().recalculate(); // Muss immer gemacht werden, damit das Zahlungsziel in Tagen berechnet wird.
    final ContentMenuEntryPanel menu = new ContentMenuEntryPanel(getNewContentMenuChildId(), new SubmitLink(
        ContentMenuEntryPanel.LINK_ID, form)
    {
      @Override
      public void onSubmit()
      {
        log.debug("Export invoice.");
        ByteArrayOutputStream baos = invoiceService.getInvoiceWordDocument(getData());
        if (baos != null) {
          DownloadUtils.setDownloadTarget(baos.toByteArray(),
              (getData().getNummer() != null ? getData().getNummer().toString() + "_" : "") + getData().getBetreff().replace(" ", "_") + "_" + "invoice.docx");
        }
      }

    }.setDefaultFormProcessing(false), getString("fibu.rechnung.exportInvoice"));
    addContentMenuEntry(menu);
  }

  @Override
  public AbstractSecuredBasePage onSaveOrUpdate()
  {
    if (isNew() == true && getData().getNummer() == null && getData().getTyp() != RechnungTyp.GUTSCHRIFTSANZEIGE_DURCH_KUNDEN) {
      getData().setNummer(rechnungDao.getNextNumber(getData()));
    }
    return null;
  }

  @Override
  protected RechnungDao getBaseDao()
  {
    return rechnungDao;
  }

  @Override
  protected RechnungEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final RechnungDO data)
  {
    return new RechnungEditForm(this, data);
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
    final RechnungDO rechnung = getData();
    rechnung.setNummer(null);
    final Integer zahlungsZielInTagen = rechnung.getZahlungsZielInTagen();
    final DayHolder day = new DayHolder();
    rechnung.setDatum(day.getSQLDate());
    if (zahlungsZielInTagen != null) {
      day.add(Calendar.DAY_OF_MONTH, zahlungsZielInTagen);
    }
    rechnung.setFaelligkeit(day.getSQLDate());
    rechnung.setZahlBetrag(null);
    rechnung.setBezahlDatum(null);
    rechnung.setStatus(RechnungStatus.GESTELLT);
    final List<RechnungsPositionDO> positionen = getData().getPositionen();
    if (positionen != null) {
      rechnung.setPositionen(new ArrayList<RechnungsPositionDO>());
      for (final RechnungsPositionDO origPosition : positionen) {
        final RechnungsPositionDO position = (RechnungsPositionDO) origPosition.newClone();
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
    if ("projektId".equals(property) == true) {
      rechnungDao.setProjekt(getData(), (Integer) selectedValue);
      form.projektSelectPanel.getTextField().modelChanged();
      if (getData().getProjektId() != null
          && getData().getProjektId() >= 0
          && getData().getKundeId() == null
          && StringUtils.isBlank(getData().getKundeText()) == true) {
        // User has selected a project and the kunde is not set:
        final ProjektDO projekt = projektDao.getById(getData().getProjektId());
        if (projekt != null) {
          rechnungDao.setKunde(getData(), projekt.getKundeId());
          form.customerSelectPanel.getTextField().modelChanged();
        }
      }
    } else if ("kundeId".equals(property) == true) {
      rechnungDao.setKunde(getData(), (Integer) selectedValue);
      form.customerSelectPanel.getTextField().modelChanged();
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  @Override
  public void unselect(final String property)
  {
    if ("projektId".equals(property) == true) {
      getData().setProjekt(null);
      form.projektSelectPanel.getTextField().modelChanged();
    } else if ("kundeId".equals(property) == true) {
      getData().setKunde(null);
      form.customerSelectPanel.getTextField().modelChanged();
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }
}

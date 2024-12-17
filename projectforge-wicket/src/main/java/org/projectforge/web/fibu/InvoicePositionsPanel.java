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

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.business.fibu.RechnungCache;
import org.projectforge.business.fibu.RechnungDao;
import org.projectforge.business.fibu.RechnungInfo;
import org.projectforge.business.fibu.RechnungPosInfo;
import org.projectforge.business.utils.CurrencyFormatter;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredPage;
import org.projectforge.web.wicket.WicketUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This panel shows invoice positions including links to the corresponding order pages.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class InvoicePositionsPanel extends Panel {
    private static final long serialVersionUID = 4744964208090705536L;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InvoicePositionsPanel.class);

    public InvoicePositionsPanel(final String id) {
        super(id);
    }

    @SuppressWarnings("serial")
    public void init(final Set<RechnungPosInfo> invoicePositionsByOrderPositionId) {
        final RepeatingView positionsRepeater = new RepeatingView("pos");
        add(positionsRepeater);
        if (invoicePositionsByOrderPositionId != null) {
            final SortedSet<Integer> invoiceNumbers = new TreeSet<Integer>();
            for (final RechnungPosInfo invoicePosition : invoicePositionsByOrderPositionId) {
                RechnungInfo rechnungInfo = invoicePosition.getRechnungInfo();
                if (rechnungInfo == null) {
                    log.warn("RechnungInfo not found for invoicePosition #" + invoicePosition.getId());
                    // Rechnung no available.
                    continue;
                }
                rechnungInfo = WicketSupport.get(RechnungCache.class).getRechnungInfo(rechnungInfo.getId());
                if (rechnungInfo == null) {
                    log.warn("RechnungInfo not found for invoice position id #" + invoicePosition.getId());
                    // Rechnung no available.
                    continue;
                }
                Integer nummer = rechnungInfo.getNummer();
                if (nummer == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("No invoice number given for invoice with id #" + rechnungInfo.getId());
                    }
                    continue; // Rechnung without number (planned one?).
                }
                invoiceNumbers.add(nummer);
            }
            boolean first = true;
            Long invoiceId = null;
            LocalDate invoiceDate = null;
            for (final Integer invoiceNumber : invoiceNumbers) {
                BigDecimal netSum = BigDecimal.ZERO;
                for (final RechnungPosInfo invoicePosition : invoicePositionsByOrderPositionId) {
                    if (invoicePosition.getRechnungInfo().getNummer() != invoiceNumber) {
                        // Invoice position doesn't match current invoice.
                        continue;
                    }
                    invoiceId = invoicePosition.getRechnungInfo().getId();
                    invoiceDate = invoicePosition.getRechnungInfo().getDate();
                    netSum = netSum.add(invoicePosition.getNetSum());
                }
                final WebMarkupContainer item = new WebMarkupContainer(positionsRepeater.newChildId());
                positionsRepeater.add(item);
                final Label separatorLabel = new Label("separator", ", ");
                if (first) {
                    separatorLabel.setVisible(false); // Invisible for first entry.
                    first = false;
                }
                item.add(separatorLabel);
                final String invoiceIdString = String.valueOf(invoiceId);
                final Link<String> link = new Link<String>("link") {
                    @Override
                    public void onClick() {
                        final PageParameters params = new PageParameters();
                        params.add(AbstractEditPage.PARAMETER_KEY_ID, invoiceIdString);
                        final RechnungEditPage page = new RechnungEditPage(params);
                        page.setReturnToPage((AbstractSecuredPage) getPage());
                        setResponsePage(page);
                    }

                    ;
                };
                item.add(link);
                final Component label = new Label("label", invoiceNumber);
                item.add(label);
                final String tooltip = DateTimeFormatter.instance().getFormattedDate(invoiceDate) + ": " + CurrencyFormatter.format(netSum);
                if (WicketSupport.get(RechnungDao.class).hasLoggedInUserSelectAccess(false)) {
                    link.add(new Label("label", invoiceNumber));
                    WicketUtils.addTooltip(link, tooltip);
                    label.setVisible(false);
                } else {
                    link.setVisible(false);
                    WicketUtils.addTooltip(label, tooltip);
                }
            }

            // final Iterator<RechnungsPositionVO> it = invoicePositionsByOrderPositionId.iterator();
            // int orderNumber = -1;
            // Link<String> link = null;
            // RechnungsPositionVO previousOrderPosition = null;
            // BigDecimal netSum = BigDecimal.ZERO;
            // while (it.hasNext() == true) {
            // final RechnungsPositionVO invoicePosition = it.next();
            // if (orderNumber == -1) {
            // orderNumber = invoicePosition.getRechnungNummer();
            // }
            // if (orderNumber == invoicePosition.getRechnungNummer().intValue()) {
            // netSum = netSum.add(invoicePosition.getNettoSumme());
            // } else {
            // orderNumber = invoicePosition.getRechnungNummer();
            // final WebMarkupContainer item = new WebMarkupContainer(positionsRepeater.newChildId());
            // positionsRepeater.add(item);
            // final Label separatorLabel = new Label("separator", ", ");
            // if (previousOrderPosition == null) {
            // separatorLabel.setVisible(false); // Invisible for first entry.
            // }
            // previousOrderPosition = invoicePosition;
            // item.add(separatorLabel);
            // link = new Link<String>("link") {
            // @Override
            // public void onClick()
            // {
            // final PageParameters params = new PageParameters();
            // params.add(AbstractEditPage.PARAMETER_KEY_ID, String.valueOf(invoicePosition.getRechnungId()));
            // final RechnungEditPage page = new RechnungEditPage(params);
            // page.setReturnToPage((AbstractSecuredPage) getPage());
            // setResponsePage(page);
            // };
            // };
            // item.add(link);
            // final String invoiceNumber = String.valueOf(invoicePosition.getRechnungNummer());
            // final Component label = new Label("label", invoiceNumber);
            // item.add(label);
            // final String tooltip = DateTimeFormatter.instance().getFormattedDate(invoicePosition.getUtilDate())
            // + ": "
            // + CurrencyFormatter.format(netSum);
            // if (rechnungDao.hasLoggedInUserSelectAccess(false) == true) {
            // link.add(new Label("label", invoiceNumber));
            // WicketUtils.addTooltip(link, tooltip);
            // label.setVisible(false);
            // } else {
            // link.setVisible(false);
            // WicketUtils.addTooltip(label, tooltip);
            // }
            // netSum = invoicePosition.getNettoSumme();
            // }
            // }
        }
    }
}

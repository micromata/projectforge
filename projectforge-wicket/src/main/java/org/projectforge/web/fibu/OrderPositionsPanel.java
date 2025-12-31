/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.business.fibu.OrderPositionInfo;
import org.projectforge.framework.utils.NumberFormatter;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredPage;
import org.projectforge.web.wicket.WicketUtils;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Set;

/**
 * This panel shows order positions including links to the corresponding order pages.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class OrderPositionsPanel extends Panel {
    private static final long serialVersionUID = 3427047480792831602L;

    public OrderPositionsPanel(final String id) {
        super(id);
    }

    @SuppressWarnings("serial")
    public void init(final Set<OrderPositionInfo> orderPositions) {
        final RepeatingView positionsRepeater = new RepeatingView("pos");
        add(positionsRepeater);
        if (orderPositions != null) {
            final Iterator<OrderPositionInfo> it = orderPositions.iterator();
            int orderNumber = -1;
            StringBuilder buf = new StringBuilder();
            Link<String> link = null;
            BigDecimal totalPersonDays = BigDecimal.ZERO;
            OrderPositionInfo previousOrderPosition = null;
            while (it.hasNext() == true) {
                final OrderPositionInfo orderPosition = it.next();
                if (orderPosition.getAuftragNummer() != null && orderNumber != orderPosition.getAuftragNummer().intValue()) {
                    orderNumber = orderPosition.getAuftragNummer();
                    final WebMarkupContainer item = new WebMarkupContainer(positionsRepeater.newChildId());
                    positionsRepeater.add(item);
                    final Label separatorLabel = new Label("separator", ", ");
                    if (previousOrderPosition != null) {
                        // Previous order position finished.
                        addTitleAttribute(link, previousOrderPosition, totalPersonDays, buf);
                        buf = new StringBuilder();
                        totalPersonDays = BigDecimal.ZERO;
                    } else {
                        separatorLabel.setVisible(false); // Invisible for first entry.
                    }
                    previousOrderPosition = orderPosition;
                    item.add(separatorLabel);
                    link = new Link<String>("link") {
                        @Override
                        public void onClick() {
                            final PageParameters params = new PageParameters();
                            params.add(AbstractEditPage.PARAMETER_KEY_ID, String.valueOf(orderPosition.getAuftragId()));
                            final AuftragEditPage page = new AuftragEditPage(params);
                            page.setReturnToPage((AbstractSecuredPage) getPage());
                            setResponsePage(page);
                        }
                    };
                    item.add(link);
                    link.add(new Label("label", String.valueOf(orderPosition.getAuftragNummer())));
                } else {
                    buf.append("\n");
                }
                buf.append("#").append(orderPosition.getNumber()).append(" (");
                if (orderPosition.getPersonDays() != null) {
                    buf.append(NumberFormatter.format(orderPosition.getPersonDays()));
                    totalPersonDays = totalPersonDays.add(orderPosition.getPersonDays());
                } else {
                    buf.append("??");
                }
                final String title = StringUtils.defaultString(orderPosition.getTitel());
                buf.append(" ").append(getString("projectmanagement.personDays.short")).append("): ").append(title);
                if (orderPosition.getStatus() != null) {
                    if (StringUtils.isNotBlank(title) == true) {
                        buf.append(", ");
                    }
                    buf.append(getString(orderPosition.getStatus().getI18nKey()));
                }
                if (it.hasNext() == false && link != null) {
                    addTitleAttribute(link, orderPosition, totalPersonDays, buf);
                }
            }
        }
    }

    private void addTitleAttribute(final Link<String> link, final OrderPositionInfo pos, final BigDecimal totalPersonDays,
                                   final StringBuilder buf) {
        final StringBuilder tooltip = new StringBuilder();
        tooltip.append(StringUtils.defaultString(pos.getAuftrag().getTitel())).append(" (").append(
                NumberFormatter.format(totalPersonDays)).append(" ").append(getString("projectmanagement.personDays.short")).append(")");
        if (pos.getAuftrag().getStatus() != null) {
            tooltip.append(", ").append(getString(pos.getAuftrag().getStatus().getI18nKey()));
        }
        WicketUtils.addTooltip(link, tooltip.toString(), buf.toString());
    }
}

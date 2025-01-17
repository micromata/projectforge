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

package org.projectforge.web.wicket;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.projectforge.business.fibu.ForecastOrderAnalysis;
import org.projectforge.web.WicketSupport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class HtmlPreviewPage extends WebPage {
    public HtmlPreviewPage(PageParameters parameters) {
        super(parameters);

        // Lade den HTML-Inhalt
        long dataId = parameters.get("dataId").toLong();
        byte[] htmlContent = WicketSupport.get(ForecastOrderAnalysis.class).htmlExportAsByteArray(dataId);

        // Erstelle einen IResourceStream für den HTML-Inhalt
        IResourceStream resourceStream = new AbstractResourceStream() {
            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(htmlContent);
            }

            @Override
            public String getContentType() {
                return "text/html";
            }

            @Override
            public void close() throws IOException {
            }
        };

        // Antwort an den Browser senden
        getRequestCycle().scheduleRequestHandlerAfterCurrent(
                new ResourceStreamRequestHandler(resourceStream) {
                    @Override
                    public void respond(IRequestCycle requestCycle) {
                        // Setze den Content-Disposition-Header explizit auf inline
                        WebResponse response = (WebResponse) requestCycle.getResponse();
                        response.setHeader("Content-Disposition", "inline"); // Rendern statt Download
                        super.respond(requestCycle);
                    }
                }
        );
    }
}

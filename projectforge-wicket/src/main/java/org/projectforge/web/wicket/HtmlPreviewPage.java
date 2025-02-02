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
import java.time.Duration;

public class HtmlPreviewPage extends AbstractSecuredPage {
    @Override
    protected String getTitle() {
        return "Forecast Order Analysis";
    }

    public HtmlPreviewPage(PageParameters parameters) {
        super(parameters);
        // Load HTML content
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

        // Send response to browser with No-Cache
        ResourceStreamRequestHandler handler = new ResourceStreamRequestHandler(resourceStream) {
            @Override
            public void respond(IRequestCycle requestCycle) {
                WebResponse response = (WebResponse) requestCycle.getResponse();
                response.setHeader("Content-Disposition", "inline"); // Rendern statt Download
                response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");

                super.respond(requestCycle);
            }
        };
        handler.setCacheDuration(Duration.ZERO); // Important: disable caching
        getRequestCycle().scheduleRequestHandlerAfterCurrent(handler);    }
}

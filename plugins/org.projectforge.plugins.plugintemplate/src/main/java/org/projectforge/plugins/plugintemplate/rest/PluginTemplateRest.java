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

package org.projectforge.plugins.plugintemplate.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.projectforge.model.rest.RestPaths;
import org.springframework.stereotype.Controller;

/**
 * REST-Schnittstelle für PlugInTemplate
 *
 * @author Florian Blumenstein
 */
@Controller
@Path("plugintemplate")
public class PluginTemplateRest
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PluginTemplateRest.class);

  @GET
  @Path(RestPaths.LIST)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getList()
  {
    final String json = "PlugInTemplate Test";
    log.info("Rest call finished PlugInTemplate list.");
    return Response.ok(json).build();
  }
}

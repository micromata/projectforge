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

package org.projectforge.model.rest;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Created by blumenstein on 05.11.16.
 */
public class CustomerDateAndTimeDeserialize extends JsonDeserializer<Date>
{
  private static Logger log = LoggerFactory.getLogger(CustomerDateAndTimeDeserialize.class);

  private SimpleDateFormat dateFormat = new SimpleDateFormat(
      "yyyy-MM-dd HH:mm:ss");

  @Override
  public Date deserialize(JsonParser paramJsonParser,
      DeserializationContext paramDeserializationContext)
      throws IOException, JsonProcessingException
  {
    String str = paramJsonParser.getText().trim();
    try {
      return dateFormat.parse(str);
    } catch (Exception e) {
      log.warn("Exception while parsing date: '" + str + "' Message: " + e.getMessage());
    }
    log.info("Try to use JSON default parser");
    return paramDeserializationContext.parseDate(str);
  }
}

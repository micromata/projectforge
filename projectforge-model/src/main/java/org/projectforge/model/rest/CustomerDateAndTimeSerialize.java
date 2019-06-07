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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Created by blumenstein on 05.11.16.
 */
public class CustomerDateAndTimeSerialize extends JsonSerializer<Date>
{
  private static Logger log = LoggerFactory.getLogger(CustomerDateAndTimeSerialize.class);

  private SimpleDateFormat dateFormat = new SimpleDateFormat(
      "yyyy-MM-dd HH:mm:ss");

  @Override
  public void serialize(final Date value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException, JsonProcessingException
  {
    if (value != null) {
      try {
        gen.writeString(dateFormat.format(value));
      } catch (Exception e) {
        log.warn("Exception while serialize date: " + e.getMessage());
        log.info("Try to use JSON default serializer");
        serializers.defaultSerializeDateKey(value, gen);
      }
    }
  }
}

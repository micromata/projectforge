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

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
      "yyyy-MM-dd HH:mm:ss.SSS");

  @Override
  public Date deserialize(JsonParser paramJsonParser,
      DeserializationContext paramDeserializationContext)
      throws IOException, JsonProcessingException
  {
    String str = paramJsonParser.getText().trim();
    try {
      return dateFormat.parse(str);
    } catch (Exception e) {
      log.debug("Exception while parsing date.");
    }
    return paramDeserializationContext.parseDate(str);
  }
}

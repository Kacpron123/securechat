package org.project.securechat.sharedClass;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Utility class for converting between JSON strings and Java objects using Jackson library.
 *
 */

public class JsonConverter {
  public static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  private static final Logger LOGGER = LogManager.getLogger();

  private static JsonNode parse(String stringToParse) throws IOException {
    try {
      return objectMapper.readTree(stringToParse);
    } catch (JsonProcessingException e) {
      LOGGER.error("JsonConverter : parse {} ", stringToParse, e);
      throw new IOException("JsonConverter : parse: " + stringToParse, e);
    }

  }


  /**
   * Parse a given JSON string into a given target type.
   *
   * @param jsonData JSON string to parse
   * @param targetType target type to parse to
   * @return parsed object
   * @throws IOException if there is a problem with the conversion
   */
  public static <T> T parseDataToObject(String jsonData, Class<T> targetType) throws IOException {
    try {
      JsonNode parsedNode = parse(jsonData);

      return objectMapper.readValue(parsedNode.toString(), targetType);
    } catch (IOException e) {
      LOGGER.error("JsonConverter : parseDataToObject {} ", jsonData, e);
      throw new IOException("JsonConverter : parseDataToObject  " + jsonData, e);
    }

  }

  /**
   * Parse a given object into a JSON string.
   *
   * @param obj object to parse
   * @return JSON string
   * @throws IOException if there is a problem with the conversion
   */
  public static <T> String parseObjectToJson(T obj) throws IOException {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (IOException e) {
      LOGGER.error("JsonConverter : parseObjectToJson ", e);
      throw new IOException("JsonConverter : parseObjectToJson ");
    }
  }

}

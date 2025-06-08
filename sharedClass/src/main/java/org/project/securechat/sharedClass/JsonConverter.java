package org.project.securechat.sharedClass;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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

  public static <T> T parseDataToObject(String jsonData, Class<T> targetType) throws IOException {
    try {
      JsonNode parsedNode = parse(jsonData);

      return objectMapper.readValue(parsedNode.toString(), targetType);
    } catch (IOException e) {
      LOGGER.error("JsonConverter : parseDataToObject {} ", jsonData, e);
      throw new IOException("JsonConverter : parseDataToObject  " + jsonData, e);
    }

  }

  public static <T> String parseObjectToJson(T obj) throws IOException {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (IOException e) {
      LOGGER.error("JsonConverter : parseObjectToJson ", e);
      throw new IOException("JsonConverter : parseObjectToJson ");
    }
  }

}

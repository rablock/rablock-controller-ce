/**
 * This file is part of Rablock Community Edition.
 *
 * Rablock Community Edition is free software: you can redistribute it 
 * and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 *
 * Rablock Community Edition is distributed in the hope that it will 
 * be useful, but WITHOUT ANY WARRANTY; without even the implied 
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Rablock Community Edition.
 * If not, see <https://www.gnu.org/licenses/>.
 */


package jp.techarts.bc.constitem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * タイプの定義クラス<br>
 * Copyright (c) 2018 Rablock Inc.
 *
 * @author TA
 * @version 1.0
 */
public final class ConstJsonType {

  /** STRING */
  public static final Class<? extends JsonNode> TEXTNODE = nodeType("string");
  /** INT */
  public static final Class<? extends JsonNode> INTNODE = nodeType("int");
  /** DOUBLE */
  public static final Class<? extends JsonNode> DOUBLENODE = nodeType("double");
  /** BOOLEAN */
  public static final Class<? extends JsonNode> BOOLEANNODE = nodeType("boolean");
  /** ARRAY */
  public static final Class<? extends JsonNode> ARRAYNODE = nodeType("array");
  /** NULL */
  public static final Class<? extends JsonNode> NULLNODE = nodeType("null");
  /** ObjectNode */
  public static final Class<? extends JsonNode> OBJECTNODE = nodeType("object");
  /** JsonObject */
  public static final Class<? extends Object> JSONOBJECT = object();

  public static Class<? extends JsonNode> nodeType(String item) {
    ObjectMapper mapper = new ObjectMapper();
    String jsonTypeString =
        "{\"string\":\"string\", \"int\":0, \"double\":1.1, \"boolean\":true, \"array\":[\"a\", \"b\"], \"null\":null, \"object\" :{\"o\":\"bj\"}}";
    try {
      JsonNode jsonType = mapper.readTree(jsonTypeString);
      return jsonType.get(item).getClass();
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Internal error: unrecorvable.", e);
    }
  }

  public static Class<? extends Object> object() {
    String jsonTypeString = "{\"object\" :{\"o\":\"bj\"}}";
    try {
      JSONObject jsonObj = new JSONObject(jsonTypeString);
      return jsonObj.get("object").getClass();
    } catch (JSONException e) {
      throw new RuntimeException("Internal error: unrecorvable.", e);
    }
  }
}

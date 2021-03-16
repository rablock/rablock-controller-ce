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


package jp.techarts.bc.jsonrpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import jp.techarts.bc.RablockSystemException;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 他ノードにJSON形式のデータを送信し、結果を受け取るクラス<br>
 * Copyright (c) 2018 Rablock Inc.
 *
 * @author TA
 * @version 1.0
 */
@Service
public class SendJson {
  private final Logger log = LoggerFactory.getLogger(SendJson.class);
  private final HttpSendJSON httpSendJSON;

  @Autowired
  public SendJson(final HttpSendJSON httpSendJSON) {
    this.httpSendJSON = httpSendJSON;
  }
  /**
   * 他ノードにトランザクションプールのデータを送信する。
   *
   * @param obj 送信するデータ
   * @param send_ip 送信先IPアドレス
   * @param port ポート番号
   * @throws RablockSystemException
   * @throws IOException ブロックをString型に変換時に発生
   */
  public String nodeSendPool(
      List<Document> deliveryList,
      String send_ip,
      String port,
      String digest_username,
      String digest_pass)
      throws RablockSystemException {

    // 送信先URL
    String strPostUrl = "http://" + send_ip + ":" + port + "/checknode";
    // 登録するJSON文字列
    String data = deliveryList.toString();

    String method = "receivePool";

    String JSON =
        "{\"id\":\"1\",\"jsonrpc\":\"2.0\",\"method\":\""
            + method
            + "\",\"params\": {\"list\": {\"data\": "
            + data
            + "}}}";
    // 認証
    String response = httpSendJSON.callPost(strPostUrl, JSON, digest_username, digest_pass);
    // 結果の表示
    ObjectMapper mapper = new ObjectMapper();
    try {
      JsonNode root = mapper.readTree(response);
      String result = root.get("result").asText();
      if (result.equals("NG")) {
        return "NG";
      }
      return "OK";
    } catch (JsonProcessingException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * 他ノードにブロックを送信する。
   *
   * @param obj 送信するデータ
   * @param send_ip 送信先IPアドレス
   * @param port ポート番号
   * @throws RablockSystemException
   */
  public String nodeSendBlock(
      Document obj, String send_ip, String port, String digest_username, String digest_pass)
      throws RablockSystemException {
    // 送信先URL
    String strPostUrl = "http://" + send_ip + ":" + port + "/checknode";
    // 登録するJSON文字列
    String block = obj.toJson();

    String method = "receiveBlock";

    String JSON =
        "{\"id\":\"1\",\"jsonrpc\":\"2.0\",\"method\":\""
            + method
            + "\",\"params\": {\"block\": "
            + block
            + "}}";
    // 認証
    String response = httpSendJSON.callPost(strPostUrl, JSON, digest_username, digest_pass);

    // 結果の表示
    ObjectMapper mapper = new ObjectMapper();
    try {
      JsonNode root = mapper.readTree(response);
      String result = root.get("result").asText();
      if (result.equals("NG")) {
        return "NG";
      }
      return "OK";
    } catch (JsonProcessingException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * トランザクションプールをコピーする
   *
   * @param send_ip コピー元のIPアドレス
   * @param port ポート番号
   * @return result トランザクションプールのデータ
   * @throws RablockSystemException
   */
  public String sendCopy(
      String send_ip, String method, String port, String digest_username, String digest_pass)
      throws RablockSystemException {
    // 送信先URL
    String strPostUrl = "http://" + send_ip + ":" + port + "/checknode";
    String JSON = "{\"id\":\"1\",\"jsonrpc\":\"2.0\",\"method\":\"" + method + "\"}";
    // 認証
    String response = httpSendJSON.callPost(strPostUrl, JSON, digest_username, digest_pass);
    // 結果の表示
    ObjectMapper mapper = new ObjectMapper();
    try {
      JsonNode root = mapper.readTree(response);
      return root.get("result").asText();
    } catch (JsonProcessingException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * 指定したハッシュ値を持つ他ノードのブロックを削除する。
   *
   * @param hash 削除するブロックのハッシュ値
   * @param method 送信先のチェックメソッド （修正要）
   * @param send_ip 送信先IPアドレス
   * @param port ポート番号
   * @return 他ノードでブロック削除: true 他ノードで削除ブロックなし: false
   * @throws RablockSystemException
   */
  public String nodeDeleteBlock(
      String hash,
      String method,
      String send_ip,
      String port,
      String digest_username,
      String digest_pass)
      throws RablockSystemException {
    // 送信先URL
    String strPostUrl = "http://" + send_ip + ":" + port + "/checknode";
    // 登録するJSON文字列
    String JSON =
        "{\"id\":\"1\",\"jsonrpc\":\"2.0\",\"method\":\""
            + method
            + "\",\"params\":{\"hash\":\""
            + hash
            + "\"}}";
    // 認証
    String response = httpSendJSON.callPost(strPostUrl, JSON, digest_username, digest_pass);
    // 結果の表示
    ObjectMapper mapper = new ObjectMapper();
    try {
      JsonNode root = mapper.readTree(response);
      return root.get("result").asText();
    } catch (JsonProcessingException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * 指定したハッシュ値を親に持つ他ノードのブロックを取得する。
   *
   * @param hash 削除するブロックのハッシュ値
   * @param method 送信先のチェックメソッド （修正要）
   * @param send_ip 送信先IPアドレス
   * @param port ポート番号
   * @return 他ノードでブロック削除: true 他ノードで削除ブロックなし: false
   * @throws RablockSystemException
   */
  public String nodeCheckBlock(
      String hash,
      String method,
      String send_ip,
      String port,
      String digest_username,
      String digest_pass)
      throws RablockSystemException {
    // 送信先URL
    String strPostUrl = "http://" + send_ip + ":" + port + "/checknode";

    String JSON =
        "{\"id\":\"1\",\"jsonrpc\":\"2.0\",\"method\":\""
            + method
            + "\",\"params\":{\"hash\":\""
            + hash
            + "\"}}";

    // 認証
    String response = httpSendJSON.callPost(strPostUrl, JSON, digest_username, digest_pass);

    // 結果の表示
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.readTree(response).get("result").asText();
    } catch (JsonProcessingException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * 指定したハッシュ値をもつブロックを取得する
   *
   * @param hash
   * @param method
   * @param send_ip
   * @param port
   * @return
   * @throws RablockSystemException
   */
  public String nodeGetBlock(
      String hash,
      String method,
      String send_ip,
      String port,
      String digest_username,
      String digest_pass)
      throws RablockSystemException {
    // 送信先URL
    String strPostUrl = "http://" + send_ip + ":" + port + "/checknode";
    String JSON =
        "{\"id\":\"1\",\"jsonrpc\":\"2.0\",\"method\":\""
            + method
            + "\",\"params\":{\"hash\":\""
            + hash
            + "\"}}";
    // 認証
    String response = httpSendJSON.callPost(strPostUrl, JSON, digest_username, digest_pass);
    // 結果の表示
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.readTree(response).get("result").asText();
    } catch (JsonProcessingException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }
}

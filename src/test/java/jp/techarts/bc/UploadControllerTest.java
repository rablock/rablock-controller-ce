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


package jp.techarts.bc;

import static org.junit.Assert.*;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.MongoTimeoutException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.annotation.PostConstruct;
import jp.techarts.bc.prop.GetAppProperties;
import jp.techarts.bc.test.SampleMethod;
import jp.techarts.bc.test.SetUpData;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UploadControllerTest {

  @Autowired private GetAppProperties app;
  @Autowired private UploadController upload;
  @Autowired private GetController get;
  @Autowired private SampleMethod test;
  @Autowired SetUpData set;

  private String db_host;
  /** データベース_ポート番号 */
  private int db_port = 0;
  /** データベース名 */
  private String db_name = "";
  /** データベース_パスワード */
  private String db_pass = "";
  /** データベース_ユーザ名 */
  private String db_user_name = "";
  /** プールコレクション名 */
  private String collection_pool_name = "";

  /** 起動時の処理<br> */
  @PostConstruct
  public void initAfterStartup() {
    // データベース名、コレクション名を設定
    db_host = app.getMongodb_host();
    db_port = app.getMongodb_port();
    db_name = app.getMongodb_db();
    db_pass = app.getMongodb_pass();
    db_user_name = app.getMongodb_username();
    collection_pool_name = app.getMongodb_coll_pool();
  }

  @Before
  public void initialize()
      throws InterruptedException, NoSuchAlgorithmException, UnsupportedEncodingException,
          RablockSystemException {

    // DB削除
    Thread.sleep(250);
    test.blockDataDelete();
    Thread.sleep(250);
    test.poolDataDelete();
    Thread.sleep(250);

    // データ登録
    set.createGenBlock();
    set.createTestPoolData();
  }

  private MongoClient getMongoClient() {
    // MongoDB認証
    MongoCredential mongoCredential =
        MongoCredential.createScramSha1Credential(db_user_name, db_name, db_pass.toCharArray());
    MongoClientSettings mongoClientOptions =
        MongoClientSettings.builder()
            .credential(mongoCredential)
            .applyToClusterSettings(
                builder -> builder.hosts(Arrays.asList(new ServerAddress(db_host, db_port))))
            .build();
    // MondoDBサーバへの接続
    return MongoClients.create(mongoClientOptions);
  }

  @Test
  public void MongoDB接続テスト() {
    boolean connected = true;
    try (MongoClient mongoClient = this.getMongoClient()) {
      mongoClient.getDatabase(db_name);
    } catch (MongoTimeoutException e) {
      connected = false;
    }
    assertTrue(connected);
  }

  @Test
  public void MongoDB登録テスト() {
    Document doc = new Document();
    doc.put("testItem1", "TEST");
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);
      MongoCollection<Document> coll = db.getCollection(collection_pool_name);
      coll.insertOne(doc);
    }
    List<Document> list = test.getAllPoolTestData();
    Document entryData = list.get(0);
    String item = entryData.get("testItem1", String.class);

    assertEquals("TEST", item);
  }

  @Test
  public void json受け取りpoolコレクションに登録テスト() throws IOException, RablockSystemException {
    String data = "{\"type\":\"new\",\"testItem1\":\"TEST\"}";
    upload.json(data);

    List<Document> list = test.getAllPoolTestData();
    Document entryData = list.get(0);
    String type = entryData.get("type", String.class);
    String item = entryData.get("testItem1", String.class);

    assertEquals("new", type);
    assertEquals("TEST", item);
  }

  @Test
  public void JsonParseExceptionが発生した場合テスト() throws IOException, RablockSystemException {
    String data = "NotJSON";
    try {
      upload.json(data);
      fail("Should be raised JsonParseException.");
    } catch (JsonParseException e) {
      /* Should come here. */
    }
  }

  @Test
  public void 修正_削除したデータに対し修正_削除するテスト() throws IOException, RablockSystemException {
    List<Document> list = test.getAllPoolTestData();
    String oid = test.getOid(list.get(0));
    String data =
        "{\"type\":\"delete\",\"user_id\":\"1001\",\"original_id\":\""
            + oid
            + "\",\"testItem1\":\"TEST\"}";
    String resurlt = upload.json(data);

    assertEquals("NG", resurlt);
  }

  @Test
  public void 連想配列データ登録テスト() throws IOException, RablockSystemException {
    String data =
        "{\"type\":\"new\","
            + "\"number\":\"00001\","
            + "\"testItem1\":\"TEST\","
            + "\"array\":"
            + "[{\"data_1\":\"aaa\",\"data_2\":\"bbb\",\"data_3\":\"ccc\"},"
            + "{\"data_1\":\"AAA\",\"data_2\":\"BBB\",\"data_3\":\"CCC\"}]}";

    upload.json(data);
    List<Document> list = test.getAllPoolTestData();
    Document entryData = list.get(list.size() - 1);
    String number = entryData.get("number", String.class);

    assertEquals("00001", number);
    //		String findJson = "{\"number\":\"00001\"}";
    //		String result = get.getJson(findJson);

  }

  @Test
  public void ネストデータ登録テスト() throws IOException, RablockSystemException {
    String data =
        "{\"type\":\"new\","
            + "\"Item\":\"abc\","
            + "\"testItem1\":\"TEST\","
            + "\"nestItem\":{\"no1\":\"123\",\"no2\":\"abc\"}}";

    upload.json(data);
    String id = "";
    JsonNode nestItem;
    String no1 = "";
    String result = get.getJson("{\"nestItem.no1\":\"123\"}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(result);
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      JsonNode iternode = iter.next();
      id = iternode.get("Item").asText();
      nestItem = iternode.get("nestItem");
      no1 = nestItem.get("no1").asText();
    }
    assertEquals("abc", id);
    assertEquals("123", no1);
  }

  @Test
  public void ネストデータ登録テスト_ネストの中にネスト() throws IOException, RablockSystemException {
    String data =
        "{\"type\":\"new\","
            + "\"Item\":\"abc\","
            + "\"nestItem\":"
            + "{\"no1\":\"123\","
            + "\"no2\":\"abc\","
            + "\"nest2\":{"
            + "\"aaa\":\"AAA\",\"bbb\":\"BBB\"}}}";

    upload.json(data);
    String id = "";
    JsonNode nestItem;
    JsonNode nest2;
    String no1 = "";
    String aaa = "";
    String result = get.getJson("{\"nestItem.nest2.aaa\":\"AAA\"}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(result);
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      JsonNode iternode = iter.next();
      id = iternode.get("Item").asText();
      nestItem = iternode.get("nestItem");
      no1 = nestItem.get("no1").asText();
      nest2 = nestItem.get("nest2");
      aaa = nest2.get("aaa").asText();
    }
    assertEquals("abc", id);
    assertEquals("123", no1);
    assertEquals("AAA", aaa);
  }
}

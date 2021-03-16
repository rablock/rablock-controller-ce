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


package jp.techarts.bc.test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jp.techarts.bc.Common;
import jp.techarts.bc.RablockSystemException;
import jp.techarts.bc.prop.GetAppProperties;
import org.bson.BsonArray;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SetUpData {

  private final SampleMethod test;
  private final Common common;

  private final String db_host;
  /** データベース_ポート番号 */
  private final int db_port;
  /** データベース名 */
  private final String db_name;
  /** データベース_パスワード */
  private final String db_pass;
  /** データベース_ユーザ名 */
  private final String db_user_name;
  /** プールコレクション名 */
  private final String collection_pool_name;
  /** ブロックコレクション名 */
  private final String collection_block_name;

  @Autowired
  public SetUpData(final GetAppProperties app, final SampleMethod test, final Common common) {
    this.test = test;
    this.common = common;
    // データベース名、コレクション名を設定
    db_host = app.getMongodb_host();
    db_port = app.getMongodb_port();
    db_name = app.getMongodb_db();
    db_pass = app.getMongodb_pass();
    db_user_name = app.getMongodb_username();
    collection_pool_name = app.getMongodb_coll_pool();
    collection_block_name = app.getMongodb_coll_block();
  }

  private MongoClient getMongoClient() {
    // MongoDB認証
    MongoCredential mongoCredential =
        MongoCredential.createScramSha1Credential(db_user_name, db_name, db_pass.toCharArray());
    MongoClientSettings mongoClientSettings =
        MongoClientSettings.builder()
            .credential(mongoCredential)
            .applyToClusterSettings(
                builder -> builder.hosts(Arrays.asList(new ServerAddress(db_host, db_port))))
            .build();
    // MondoDBサーバへの接続
    return MongoClients.create(mongoClientSettings);
  }

  /**
   * ジェネシスブロック登録
   *
   * @throws NoSuchAlgorithmException
   * @throws UnsupportedEncodingException
   * @throws RablockSystemException
   */
  public void createGenBlock()
      throws NoSuchAlgorithmException, UnsupportedEncodingException, RablockSystemException {
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);
      MongoCollection<Document> blockColl = db.getCollection(collection_block_name);
      Document doc =
          new Document("height", 0)
              .append("size", 0)
              .append("settime", common.getCurrentTime())
              .append("timestamp", common.getTimestamp())
              .append("prev_hash", "0")
              .append("testItem1", "GENTEST");

      String hashString = doc.toJson();
      String hashValue = common.hashCal(hashString);
      doc = doc.append("hash", hashValue);
      blockColl.insertOne(doc);
    }
  }

  /** トランザクションデータ登録 */
  public void createTestPoolData() {
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);
      MongoCollection<Document> coll = db.getCollection(collection_pool_name);

      // 1_ID:1001の新規データ
      Document data_0 =
          new Document("type", "new")
              .append("user_id", "1001")
              .append("deliveryF", true)
              .append("testItem1", "TEST");
      coll.insertOne(data_0);

      // 2_ID:1001の修正データ
      List<Document> testData = test.getAllPoolTestData();
      Document d = testData.get(0);
      String d_oid = d.get("_id").toString();
      Document data_1 =
          new Document("type", "modify")
              .append("user_id", "1001")
              .append("original_id", d_oid)
              .append("deliveryF", true)
              .append("testItem1", "TEST");
      coll.insertOne(data_1);

      // 3_ID:2001の新規データ
      Document data_2 =
          new Document("type", "new")
              .append("user_id", "2001")
              .append("deliveryF", true)
              .append("testItem1", "TEST");
      coll.insertOne(data_2);

      // 4_ID:2001の削除データ
      testData = test.getAllPoolTestData();
      d = testData.get(2);
      d_oid = d.get("_id").toString();
      Document data_3 =
          new Document("type", "delete")
              .append("user_id", "2001")
              .append("original_id", d_oid)
              .append("deliveryF", true)
              .append("testItem1", "TEST");
      coll.insertOne(data_3);

      // 5_ID:3001の新規データ
      Document data_4 =
          new Document("type", "new")
              .append("user_id", "3001")
              .append("deliveryF", true)
              .append("testItem1", "TEST");
      coll.insertOne(data_4);

      // 6_ID:3001の新規データ
      Document data_5 =
          new Document("type", "new")
              .append("user_id", "3001")
              .append("deliveryF", true)
              .append("testItem1", "TEST");
      coll.insertOne(data_5);

      // 7_ID:4001の新規データ
      Document data_6 =
          new Document("type", "new")
              .append("user_id", "4001")
              .append("deliveryF", true)
              .append("testItem1", "TEST");
      coll.insertOne(data_6);

      // 8_ID:4001の修正データ
      testData = test.getAllPoolTestData();
      d = testData.get(6);
      d_oid = d.get("_id").toString();
      Document data_7 =
          new Document("type", "modify")
              .append("user_id", "4001")
              .append("original_id", d_oid)
              .append("deliveryF", true)
              .append("testItem1", "TEST");
      coll.insertOne(data_7);

      // 9_ID:4001の修正データ
      testData = test.getAllPoolTestData();
      d = testData.get(7);
      d_oid = d.get("_id").toString();
      Document data_8 =
          new Document("type", "modify")
              .append("user_id", "4001")
              .append("original_id", d_oid)
              .append("deliveryF", false)
              .append("testItem1", "TEST");
      coll.insertOne(data_8);

      // 7_ID:4001の新規データ
      Document data_9 =
          new Document("type", "new")
              .append("user_id", "5001")
              .append("deliveryF", true)
              .append("testItem1", "TEST");
      coll.insertOne(data_9);
    }
  }

  /** トランザクションデータ登録(ネストデータ) */
  public void createNestTestPoolData() {
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);
      MongoCollection<Document> coll = db.getCollection(collection_pool_name);

      Document data = new Document(test.createNestData());
      coll.insertOne(data);
    }
  }
  /** 未伝搬のトランザクションデータ登録 */
  public void createTestPoolNotDeleveryData() {
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);
      MongoCollection<Document> coll = db.getCollection(collection_pool_name);

      // 1_ID:1001の新規データ
      Document data_0 =
          new Document("type", "new")
              .append("user_id", "1001")
              .append("deliveryF", false)
              .append("testItem1", "TEST");
      coll.insertOne(data_0);
    }
  }
  /** ブロックデータ登録 */
  public void createTestBlockData() {
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);

      // ID:5001の新規データ作成
      ObjectId id_5001 = new ObjectId();
      Document data_0 =
          new Document("_id", id_5001)
              .append("type", "new")
              .append("user_id", "5001")
              .append("deliveryF", true)
              .append("testItem1", "TEST");
      // ID:6001の新規データ作成
      ObjectId id_6001 = new ObjectId();
      Document data_1 =
          new Document("_id", id_6001)
              .append("type", "new")
              .append("user_id", "6001")
              .append("deliveryF", true)
              .append("testItem1", "TEST");
      // ID:6001の修正データ作成
      ObjectId id_6002 = new ObjectId();
      Document data_2 =
          new Document("_id", id_6002)
              .append("type", "modify")
              .append("user_id", "6001")
              .append("original_id", id_6001.toString())
              .append("deliveryF", true)
              .append("testItem1", "TEST");

      MongoCollection<Document> blockColl = db.getCollection(collection_block_name);

      List<Document> poolList = new ArrayList<>();
      poolList.add(data_0);
      poolList.add(data_1);
      poolList.add(data_2);

      // ブロック化
      Document genData = test.getGenBlock();
      String gen_hash = genData.get("hash").toString();

      Document block_1 =
          new Document("data", poolList)
              .append("prev_hash", gen_hash)
              .append("hash", "abcdef123")
              .append("testItem1", "TEST");
      blockColl.insertOne(block_1);
    }
  }

  /** ブロックデータ登録(ネストデータ1件) */
  public void createNestTestBlockData() {
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);

      // ネストデータ作成
      Document data_0 = test.createNestData();
      ObjectId id = new ObjectId();
      data_0.append("_id", id);

      MongoCollection<Document> blockColl = db.getCollection(collection_block_name);

      List<Document> poolList = new ArrayList<>();
      poolList.add(data_0);

      // ブロック化
      Document genData = test.getGenBlock();
      String gen_hash = genData.get("hash").toString();

      Document block_1 =
          new Document("data", poolList)
              .append("prev_hash", gen_hash)
              .append("hash", "abcdef123")
              .append("testItem1", "TEST")
              .append("height", 1);
      blockColl.insertOne(block_1);
    }
  }

  /** ブロックデータ登録(ネストデータ2件) */
  public void createNestTestBlockData_2() {
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);

      // ネストデータ作成
      Document data_0 = test.createNestData();
      ObjectId id_0 = new ObjectId();
      data_0.append("_id", id_0);

      Document data_1 = test.createNestData();
      ObjectId id_1 = new ObjectId();
      data_1.append("_id", id_1);

      List<Document> poolList = new ArrayList<>();
      poolList.add(data_0);
      poolList.add(data_1);

      MongoCollection<Document> blockColl = db.getCollection(collection_block_name);
      // ブロック化
      Document genData = test.getGenBlock();
      String gen_hash = genData.get("hash").toString();

      Document block_1 =
          new Document("data", poolList)
              .append("prev_hash", gen_hash)
              .append("hash", "abcdef123")
              .append("testItem1", "TEST");
      blockColl.insertOne(block_1);
    }
  }

  /** ブロックデータ登録(ネストデータ1件,通常データ1件) */
  public void createNestTestBlockData_1_1() {
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);

      // ネストデータ作成
      Document data_0 = test.createNestData();
      ObjectId id_0 = new ObjectId();
      data_0.append("_id", id_0);

      Document data_1 = test.createData();
      ObjectId id_1 = new ObjectId();
      data_1.append("_id", id_1);

      List<Document> poolList = new ArrayList<>();
      poolList.add(data_0);
      poolList.add(data_1);

      // ブロック化
      Document genData = test.getGenBlock();
      String gen_hash = genData.get("hash").toString();

      Document block_1 =
          new Document("data", poolList)
              .append("prev_hash", gen_hash)
              .append("hash", "abcdef123")
              .append("testItem1", "TEST");
      final MongoCollection<Document> blockColl = db.getCollection(collection_block_name);
      blockColl.insertOne(block_1);
    }
  }

  /** ブロックデータ登録(ネストデータ0件,通常データ1件) */
  public void createNestTestBlockData_0_1() {
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);

      Document data_1 = test.createData();
      ObjectId id_1 = new ObjectId();
      data_1.append("_id", id_1);

      List<Document> poolList = new ArrayList<>();
      poolList.add(data_1);

      MongoCollection<Document> blockColl = db.getCollection(collection_block_name);
      // ブロック化
      Document genData = test.getGenBlock();
      String gen_hash = genData.get("hash").toString();

      Document block_1 =
          new Document("data", poolList)
              .append("prev_hash", gen_hash)
              .append("hash", "abcdef123")
              .append("testItem1", "TEST");
      blockColl.insertOne(block_1);
    }
  }

  /** ブロックデータ登録(2ブロックにネストデータ1件ずつ) */
  public void createNestTest2BlockData() {
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);

      // ネストデータ作成
      Document data_0 = test.createNestData();
      ObjectId id_test = new ObjectId();
      data_0.append("_id", id_test);

      MongoCollection<Document> blockColl = db.getCollection(collection_block_name);

      List<Document> poolList = new ArrayList<>();
      poolList.add(data_0);

      // ブロック化
      Document lastData = test.getLastBlock();
      String last_hash = lastData.get("hash").toString();

      Document block_1 =
          new Document("data", poolList)
              .append("prev_hash", last_hash)
              .append("hash", "abcdef456")
              .append("testItem1", "TEST");
      blockColl.insertOne(block_1);

      // ネストデータ作成
      data_0.append("_id", new ObjectId());
      poolList = new ArrayList<>();
      poolList.add(data_0);

      // ブロック化
      Document lastBlockData = test.getLastBlock();
      last_hash = lastBlockData.get("hash").toString();

      Document block_2 =
          new Document("data", poolList)
              .append("prev_hash", last_hash)
              .append("hash", "abcdef789")
              .append("testItem1", "TEST")
              .append("height", 2);
      blockColl.insertOne(block_2);
    }
  }

  /** poolに新規データ、blockに修正データ登録 */
  public void createTrakingData_1() {
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);
      MongoCollection<Document> coll = db.getCollection(collection_pool_name);

      // 1_ID:1001の新規データ
      ObjectId id_1001 = new ObjectId();
      Document data_0 =
          new Document("_id", id_1001)
              .append("type", "new")
              .append("user_id", "1001")
              .append("deliveryF", true)
              .append("testItem1", "TEST");
      coll.insertOne(data_0);

      // ID:1001の修正データ作成
      ObjectId id_1001_modify = new ObjectId();
      Document data_1 =
          new Document("_id", id_1001_modify)
              .append("type", "modify")
              .append("user_id", "1001")
              .append("original_id", id_1001.toString())
              .append("deliveryF", true)
              .append("testItem1", "TEST");

      List<Document> poolList = new ArrayList<>();
      poolList.add(data_1);

      // ブロック化
      Document genData = test.getGenBlock();
      String gen_hash = genData.get("hash").toString();

      Document block_1 =
          new Document("data", poolList)
              .append("prev_hash", gen_hash)
              .append("hash", "abcdef123")
              .append("testItem1", "TEST");

      MongoCollection<Document> blockColl = db.getCollection(collection_block_name);
      blockColl.insertOne(block_1);
    }
  }
  /** blockに新規データ、poolに修正データ登録 */
  public void createTrakingData_2() {
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);

      // ID:1001の新規データ作成
      ObjectId id_1001_new = new ObjectId();
      Document data_1 =
          new Document("_id", id_1001_new)
              .append("type", "new")
              .append("user_id", "1001")
              .append("deliveryF", true)
              .append("testItem1", "TEST");

      List<Document> poolList = new ArrayList<>();
      poolList.add(data_1);

      // ブロック化
      Document genData = test.getGenBlock();
      String gen_hash = genData.get("hash").toString();

      Document block_1 =
          new Document("data", poolList)
              .append("prev_hash", gen_hash)
              .append("hash", "abcdef123")
              .append("testItem1", "TEST");

      MongoCollection<Document> coll = db.getCollection(collection_block_name);
      coll.insertOne(block_1);

      // 1_ID:1001の新規データ
      ObjectId id_1001_modify = new ObjectId();
      Document data_0 =
          new Document("_id", id_1001_modify)
              .append("type", "modify")
              .append("user_id", "1001")
              .append("original_id", id_1001_new.toString())
              .append("deliveryF", true)
              .append("testItem1", "TEST");

      MongoCollection<Document> poolColl = db.getCollection("pool");
      poolColl.insertOne(data_0);
    }
  }

  /** トランザクションデータ登録 getJsonメソッド用 */
  public void createTestPoolDataForGetJson() {
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);
      MongoCollection<Document> coll = db.getCollection(collection_pool_name);

      BsonArray testArray = new BsonArray();
      testArray.add(new BsonString("abc"));
      testArray.add(new BsonString("efg"));

      ObjectNode obj = new ObjectNode(null);

      Document data_0 =
          new Document("type", "new")
              .append("int_item", 123)
              .append("double_item", 456.78)
              .append("boolean_item", true)
              .append("boolean_item2", false)
              .append("null_item", null)
              .append("allay_item", testArray)
              .append("obj_item", obj)
              .append("deliveryF", true)
              .append("testItem1", "TEST");
      coll.insertOne(data_0);
    }
  }

  /** ブロックデータ登録 getJsonメソッド用 */
  public void createTestBlockDataForGetJson() {
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);

      BsonArray testArray = new BsonArray();
      testArray.add(new BsonString("abc"));
      testArray.add(new BsonString("efg"));

      ObjectNode obj = new ObjectNode(null);

      ObjectId jsonTest = new ObjectId();
      Document data_0 =
          new Document("_id", jsonTest)
              .append("type", "new")
              .append("int_item", 123)
              .append("double_item", 456.78)
              .append("boolean_item", true)
              .append("boolean_item2", false)
              .append("null_item", null)
              .append("allay_item", testArray)
              .append("obj_item", obj)
              .append("deliveryF", true)
              .append("testItem1", "TEST");

      Document data_1 =
          new Document("_id", new ObjectId())
              .append("type", "new")
              .append("deliveryF", true)
              .append("testItem1", "TEST");

      MongoCollection<Document> blockColl = db.getCollection(collection_block_name);

      List<Document> poolList = new ArrayList<>();
      poolList.add(data_0);
      poolList.add(data_1);

      // ブロック化
      Document genData = test.getGenBlock();
      String gen_hash = genData.get("hash").toString();

      Document block_1 =
          new Document("data", poolList)
              .append("prev_hash", gen_hash)
              .append("hash", "json123")
              .append("testItem1", "TEST");
      blockColl.insertOne(block_1);
    }
  }
}

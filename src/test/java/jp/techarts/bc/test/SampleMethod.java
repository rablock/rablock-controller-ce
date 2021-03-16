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

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import jp.techarts.bc.prop.GetAppProperties;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SampleMethod {

  private String db_host;
  /** データベース_ポート番号 */
  private int db_port = 0;
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
  public SampleMethod(final GetAppProperties app) {
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
   * Poolコレクションに登録したテストデータの件数確認
   *
   * @param coll
   * @param ip
   * @return count 件数
   */
  public long testDataPoolCount() {
    long count = -1;
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);
      // トランザクションプールDBの指定
      MongoCollection<Document> poolColl = db.getCollection(collection_pool_name);
      BasicDBObject searchQuery = new BasicDBObject("testItem1", new BasicDBObject("$eq", "TEST"));
      count = poolColl.countDocuments(searchQuery);
    }
    return count;
  }

  /**
   * Poolコレクションのテストデータを削除
   *
   * @param ip
   */
  public void poolDataDelete() {
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);
      // トランザクションプールDBの指定
      MongoCollection<Document> coll = db.getCollection(collection_pool_name);
      // BasicDBObject searchQuery = new BasicDBObject("testItem1", new
      // BasicDBObject("$eq", "TEST"));
      // coll.remove(searchQuery);
      coll.drop();
    }
  }

  /** Poolコレクションのテストデータをすべて取得 */
  public List<Document> getAllPoolTestData() {
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);
      // トランザクションプールDBの指定
      MongoCollection<Document> poolColl = db.getCollection(collection_pool_name);

      BasicDBObject searchQuery = new BasicDBObject("testItem1", new BasicDBObject("$eq", "TEST"));
      FindIterable<Document> curs = poolColl.find(searchQuery);
      return StreamSupport.stream(curs.spliterator(), false).collect(Collectors.toList());
    }
  }

  /**
   * Blockコレクションに登録したテストデータの件数確認
   *
   * @param coll
   * @param ip
   * @return count 件数
   */
  public long testDataBlockCount() {
    long count = -1;
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);
      // トランザクションプールDBの指定
      MongoCollection<Document> poolColl = db.getCollection(collection_block_name);
      count = poolColl.countDocuments();
    }
    return count;
  }

  /** Blockコレクションのジェネシスブロックを取得 */
  public Document getGenBlock() {
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);
      // トランザクションプールDBの指定
      MongoCollection<Document> poolColl = db.getCollection(collection_block_name);

      BasicDBObject searchQuery = new BasicDBObject("prev_hash", new BasicDBObject("$eq", "0"));
      return poolColl.find(searchQuery).first();
    }
  }

  /**
   * 最後のブロックを取得
   *
   * @return
   */
  public Document getLastBlock() {
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);
      // トランザクションプールDBの指定
      MongoCollection<Document> blockColl = db.getCollection(collection_block_name);

      Document doc = blockColl.find().sort(new BasicDBObject("$natural", -1)).limit(1).first();
      // curs = this.block.find().sort(new BasicDBObject("height",-1)).limit(1);
      if (doc != null) {
        doc.remove("data");
      }
      return doc;
    }
  }

  /**
   * Blockコレクションのテストデータを削除
   *
   * @param ip
   */
  public void blockDataDelete() {
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);
      // トランザクションプールDBの指定
      MongoCollection<Document> coll = db.getCollection(collection_block_name);

      // BasicDBObject searchQuery = new BasicDBObject("testItem1", new
      // BasicDBObject("$eq", "TEST"));
      // coll.remove(searchQuery);
      coll.drop();
    }
  }

  /** Blockコレクションのテストデータをすべて取得 */
  public List<Document> getAllBlockTestData() {
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);
      // トランザクションプールDBの指定
      MongoCollection<Document> blockColl = db.getCollection(collection_block_name);

      BasicDBObject searchQuery = new BasicDBObject("testItem1", new BasicDBObject("$eq", "TEST"));
      FindIterable<Document> curs = blockColl.find(searchQuery);
      return StreamSupport.stream(curs.spliterator(), false).collect(Collectors.toList());
    }
  }

  /** Document または DBObjectのオブジェクトIDを取得 */
  public String getOid(Document obj) {
    return obj.get("_id").toString();
  }

  /** 通常データ作成 */
  public Document createData() {
    return new Document("type", "new")
        .append("user_id", "1001")
        .append("deliveryF", true)
        .append("testItem1", "TEST");
  }

  /** ネストデータ作成 */
  public Document createNestData() {
    Document nest_data_2 =
        new Document("nestItem_4", "abc")
            .append("nestItem_5", "def")
            .append("nestIntItem", 1)
            .append("nestDoubleItem", 1.23)
            .append("nestBooleanItem", true);

    Document nest_data =
        new Document("nestItem_1", "ABC")
            .append("nestItem_2", "DEF")
            .append("nestItem_3", nest_data_2);

    return new Document("type", "new")
        .append("user_id", "1001")
        .append("deliveryF", true)
        .append("testItem1", "TEST")
        .append("nest", nest_data);
  }
}

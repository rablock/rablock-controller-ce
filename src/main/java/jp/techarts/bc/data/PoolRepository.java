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


package jp.techarts.bc.data;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.MongoTimeoutException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import jp.techarts.bc.RablockSystemException;
import jp.techarts.bc.constitem.ConstItem;
import jp.techarts.bc.constitem.ConstJsonType;
import jp.techarts.bc.constitem.ConstType;
import jp.techarts.bc.prop.GetAppProperties;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * MongoDBアクセスサービスクラス<br>
 * Copyright (c) 2018 Rablock Inc.
 *
 * @author TA
 * @version 1.0
 */
@Repository
public class PoolRepository {
  private final Logger log = LoggerFactory.getLogger(PoolRepository.class);

  private final GetAppProperties app;

  /** MongoDB アクセス クライアント */
  private MongoClient mongoClient = null;

  /** MongoDB DBオブジェクト */
  private MongoDatabase dbObject = null;

  /** MongoDB DBCollection トランザクションデータ */
  private MongoCollection<Document> pool = null;

  private final QueryHelper queryHelper;

  @Autowired
  PoolRepository(final GetAppProperties app, final QueryHelper queryHelper) {
    this.app = app;
    this.queryHelper = queryHelper;
  }

  /**
   * DBログイン処理
   *
   * @param portNo
   * @param dbName
   * @param dbUser
   * @param dbPass
   * @return
   */
  @PostConstruct
  private void DbLogin() {
    final String host = app.getMongodb_host();
    final int portNo = app.getMongodb_port();
    final String dbName = app.getMongodb_db();
    final String dbPass = app.getMongodb_pass();
    final String dbUser = app.getMongodb_username();
    final String collectionName = app.getMongodb_coll_pool();
    // MongoDB認証
    MongoCredential mongoCredential =
        MongoCredential.createScramSha1Credential(dbUser, dbName, dbPass.toCharArray());

    log.debug("DbLogin");

    MongoClientSettings settings =
        MongoClientSettings.builder()
            .retryWrites(true)
            .credential(mongoCredential)
            .applyToClusterSettings(
                builder -> builder.hosts(Arrays.asList(new ServerAddress(host, portNo))))
            .build();
    // MondoDBサーバへの接続
    this.mongoClient = MongoClients.create(settings);
    // MongoDB DBオブジェクト取得
    this.dbObject = this.mongoClient.getDatabase(dbName);
    // 使用するコレクションの指定
    this.pool = this.dbObject.getCollection(collectionName);
  }

  /** DBログアウト処理 */
  @PreDestroy
  public void DbLogout() {

    log.debug("DbLogout");

    this.mongoClient.close();
    this.mongoClient = null;
    /** MongoDB DBオブジェクト */
    this.dbObject = null;
    /** MongoDB DBCollection トランザクションデータ */
    this.pool = null;
  }

  /**
   * トランザクションデータの登録
   *
   * @param objName
   * @param data
   * @return
   */
  public InsertOneResult insertTranData(Document data) {
    try {
      return this.pool.insertOne(data);
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * トランザクションデータの削除
   *
   * @param objName
   * @param data
   * @return
   */
  public DeleteResult removeTranData(Document data) {
    try {
      return this.pool.deleteOne(data);
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * 指定のOidでトランザクションデータを削除
   *
   * @param objName
   * @param oid
   * @return
   */
  public DeleteResult removeTranDataByOid(String oid) {
    Bson data = eq("_id", new ObjectId(oid));
    try {
      return this.pool.deleteOne(data);
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * DeliveryFがtrueのトランザクションデータを削除
   *
   * @param objName
   * @return
   */
  public DeleteResult removeDeliveredTranData() {
    Bson data = eq("deliveryF", true);
    try {
      return this.pool.deleteMany(data);
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * トランザクションデータの全情報取得
   *
   * @param objName
   * @return
   */
  public List<Document> getTranDataList() {
    try {
      return StreamSupport.stream(pool.find().spliterator(), false).collect(Collectors.toList());
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * トランザクションデータの全オブジェクトID情報取得
   *
   * @param objName
   * @return
   */
  public List<ObjectId> getTranDataObjIDList() {
    try {
      return StreamSupport.stream(this.pool.find().spliterator(), false)
          .map(curs -> curs.get("_id", ObjectId.class))
          .collect(Collectors.toList());
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * トランザクションデータのDeliveryFがTRUEのデータ取得
   *
   * @param objName
   * @return
   */
  public List<Document> getTranDeliveredDataList() {
    Bson searchQuery = eq("deliveryF", true);
    try {
      return StreamSupport.stream(this.pool.find(searchQuery).spliterator(), false)
          .collect(Collectors.toList());
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * トランザクションデータのDeliveryFがFALSEのデータ取得
   *
   * @param objName
   * @return
   */
  public List<Document> getNotTranDeliveredDataList() {
    Bson searchQuery = eq("deliveryF", false);
    try {
      return StreamSupport.stream(this.pool.find(searchQuery).spliterator(), false)
          .collect(Collectors.toList());
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * トランザクションデータのDeliveryFを更新
   *
   * @param objName
   * @param dbUpdateObj
   * @param status
   * @return
   */
  public UpdateResult updateTranDeliveryStatus(Document dbUpdateObj, boolean status) {
    Bson filter = eq("_id", dbUpdateObj.get("_id"));
    Document newDoc = new Document(dbUpdateObj);
    newDoc.put("deliveryF", status);

    ReplaceOptions options = new ReplaceOptions().upsert(true);
    try {
      return this.pool.replaceOne(filter, newDoc, options);
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * トランザクションデータのオブジェクトID検索
   *
   * @param objName
   * @return
   */
  public Optional<Document> getTranDataByOid(String key, String oid) {
    Bson searchQuery = eq(key, new ObjectId(oid));
    try {
      return Optional.ofNullable(this.pool.find(searchQuery).first());
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * トランザクションの修正・削除データを検索
   *
   * @param objName
   * @return
   */
  public List<String> getTranModifyDeleteList() {
    // 削除されたデータと修正されたデータをブロックから取得
    Bson queryDeleteOrModify = queryHelper.getDBObjectDeleteOrModify(ConstItem.DATA_TYPE);
    try {
      return StreamSupport.stream(this.pool.find(queryDeleteOrModify).spliterator(), false)
          .map(x -> x.get(ConstItem.DATA_ORIGINAL_ID, String.class))
          .filter(x -> x != null)
          .collect(Collectors.toList());
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * プールからトランザクションの指定項目と値でデータを取得
   *
   * @param objName
   * @param key
   * @param jsonnode
   * @param modifyDeleteList
   * @return
   */
  public List<Document> getTranByKeyValue(
      String key, JsonNode jsonnode, List<String> modifyDeleteList) {
    // JSONの値のデータ型取得
    Class<? extends JsonNode> dataType = jsonnode.get(key).getClass();
    Bson valueQuery;
    if (dataType.equals(ConstJsonType.TEXTNODE)) {
      String value = jsonnode.get(key).asText();
      valueQuery = eq(key, value);
    } else if (dataType.equals(ConstJsonType.INTNODE)) {
      int value = jsonnode.get(key).asInt();
      valueQuery = eq(key, value);
    } else if (dataType.equals(ConstJsonType.DOUBLENODE)) {
      Double value = jsonnode.get(key).asDouble();
      valueQuery = eq(key, value);
    } else if (dataType.equals(ConstJsonType.BOOLEANNODE)) {
      boolean value = jsonnode.get(key).asBoolean();
      valueQuery = eq(key, value);
    } else {
      return Collections.emptyList();
    }

    final Bson queryNewOrModify = queryHelper.getDBObjectNewOrModify(ConstItem.DATA_TYPE);
    final Bson searchQuery = and(valueQuery, queryNewOrModify);
    List<ObjectId> excludeList =
        modifyDeleteList.stream().map(oid -> new ObjectId(oid)).collect(Collectors.toList());
    try {
      return StreamSupport.stream(this.pool.find(searchQuery).spliterator(), false)
          .filter(tranObject -> !(excludeList.contains(tranObject.getObjectId("_id"))))
          .collect(Collectors.toList());
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * プールからトランザクションの指定項目と値で暗号化されたデータを取得
   *
   * @param objName
   * @param key
   * @param value
   * @param modifyDeleteList
   * @return
   */
  public List<Document> getCryptoTranByKeyValue(
      String key, String value, List<String> modifyDeleteList) {
    // $regex正規表現で取得
    Bson valueQuery = regex(key, value);
    final Bson queryNewOrModify = queryHelper.getDBObjectNewOrModify(ConstItem.DATA_TYPE);
    final Bson searchQuery = and(valueQuery, queryNewOrModify);
    List<Document> list = new ArrayList<>();
    try {
      StreamSupport.stream(this.pool.find(searchQuery).spliterator(), false)
          .forEach(
              tranObject -> {
                list.add(tranObject);
                // 削除リストにIDがあるオブジェクトはリストから除く
                modifyDeleteList.stream()
                    .filter(deleteId -> tranObject.get("_id").equals(new ObjectId(deleteId)))
                    .forEach(deleteId -> list.remove(tranObject));
              });
      return list;
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * トランザクションの指定項目とString値でデータを取得
   *
   * @param objName
   * @param key
   * @param jsonnode
   * @param modifyDeleteList
   * @return
   */
  public List<Document> getTranByKeyStringValue(
      String key, String value, List<String> modifyDeleteList) {
    List<Document> list = new ArrayList<>();
    Bson valueQuery = eq(key, value);
    final Bson queryNewOrModify = queryHelper.getDBObjectNewOrModify(ConstItem.DATA_TYPE);
    final Bson searchQuery = and(valueQuery, queryNewOrModify);
    try {
      StreamSupport.stream(this.pool.find(searchQuery).spliterator(), true)
          .filter(
              tranObject ->
                  tranObject.get("type").equals(ConstType.NEW)
                      || tranObject.get("type").equals(ConstType.MODIFY))
          .forEach(
              tranObject -> {
                list.add(tranObject);
                // 削除リストにIDがあるオブジェクトはリストから除く
                modifyDeleteList.stream()
                    .filter(deleteId -> tranObject.get("_id").equals(new ObjectId(deleteId)))
                    .forEach(deleteId -> list.remove(tranObject));
              });
      return list;
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }
}

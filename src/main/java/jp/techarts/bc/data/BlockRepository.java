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

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.MongoTimeoutException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import jp.techarts.bc.RablockSystemException;
import jp.techarts.bc.constitem.ConstItem;
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
public class BlockRepository {
  private final Logger log = LoggerFactory.getLogger(BlockRepository.class);

  private final GetAppProperties app;

  /** MongoDB アクセス クライアント */
  private MongoClient mongoClient = null;

  /** MongoDB DBオブジェクト */
  private MongoDatabase dbObject = null;

  /** MongoDB DBCollection ブロックデータ */
  private MongoCollection<Document> block = null;

  private final QueryHelper queryHelper;

  @Autowired
  public BlockRepository(final GetAppProperties app, final QueryHelper queryHelper) {
    this.app = app;
    this.queryHelper = queryHelper;
  }

  /** 起動時の処理<br> */
  @PostConstruct
  public void DBLogin() {
    final String host = app.getMongodb_host();
    final int portNo = app.getMongodb_port();
    final String dbName = app.getMongodb_db();
    final String dbPass = app.getMongodb_pass();
    final String dbUser = app.getMongodb_username();
    final String collectionName = app.getMongodb_coll_block();
    MongoCredential mongoCredential =
        MongoCredential.createScramSha1Credential(dbUser, dbName, dbPass.toCharArray());
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
    this.block = this.dbObject.getCollection(collectionName);
  }

  /** DBログアウト処理 */
  @PreDestroy
  public void DbLogout() {

    log.debug("DbLogout");

    if (this.mongoClient != null) {
      this.mongoClient.close();
      this.mongoClient = null;
    }
    /** MongoDB DBオブジェクト */
    this.dbObject = null;
    /** MongoDB DBCollection ブロックデータ */
    this.block = null;
  }

  /**
   * ブロックデータの登録
   *
   * @param objName
   * @param data
   * @return
   */
  public InsertOneResult insertBlockData(Document data) {
    try {
      return this.block.insertOne(data);
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * ブロックデータの削除
   *
   * @param objName
   * @param data
   * @return
   */
  public DeleteResult removeBlockData(Document data) {
    try {
      return this.block.deleteOne(data);
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * ブロックデータの更新
   *
   * @param objName
   * @param queryObj
   * @param updateObj
   * @return
   */
  public UpdateResult updateBlockData(Document queryObj, Document updateObj) {
    try {
      return this.block.updateOne(queryObj, updateObj);
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * ブロックデータのキー情報による更新
   *
   * @param objName
   * @param key
   * @param value
   * @param updateObj
   * @return
   */
  public UpdateResult updateBlockByKeyValue(String key, String value, Document updateObj) {
    Bson searchQuery = eq(key, value);
    try {
      return this.block.updateOne(searchQuery, updateObj);
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * ブロックデータの件数取得
   *
   * @param objName
   * @return
   */
  public int getBlockCount() {
    try {
      return (int) this.block.countDocuments();
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * ブロックヘッダー項目の値でブロックを取得
   *
   * @param objName
   * @param key
   * @param value
   * @return
   */
  public Optional<Document> getBlockByKeyValue(String key, String value) {
    Bson query = eq(key, value);
    try {
      return Optional.ofNullable(this.block.find(query).first());
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * ブロックヘッダー項目のoid値でブロックを取得
   *
   * @param objName
   * @param key
   * @param value
   * @return
   */
  public Optional<Document> getBlockDataByOid(String key, String oid) {
    Bson query = eq(key, new ObjectId(oid));
    try {
      return Optional.ofNullable(this.block.find(query).first());
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * ブロックデータの全情報取得
   *
   * @param objName
   * @return
   */
  public List<Document> getBlockDataList() {
    try {
      return StreamSupport.stream(
              this.block.find().sort(new BasicDBObject("$natural", 1)).spliterator(), false)
          .collect(Collectors.toList());
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * Blockコレクションのブロックのデータ全件取得
   *
   * @param objName
   * @return
   */
  public List<Document> getAllBlockDataList() {
    try {
      return StreamSupport.stream(this.block.find().spliterator(), true)
          .filter(blockObject -> !blockObject.get(ConstItem.BLOCK_PREV_HASH).equals("0"))
          .map(blockObject -> blockObject.getList(ConstItem.BLOCK_DATA, Document.class))
          .flatMap(Collection::stream)
          .collect(Collectors.toList());
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * ブロックデータのオブジェクトID検索
   *
   * @param objName
   * @param oid
   * @return Document or null.
   */
  public Optional<Document> getBlockDataByOid(String oid) {
    // リストを取得
    Bson searchQuery = eq("data._id", new ObjectId(oid));
    try {
      Document blockObject = this.block.find(searchQuery).first();
      if (blockObject != null) {
        final List<Document> dataObjList =
            blockObject.getList(ConstItem.BLOCK_DATA, Document.class);
        final ObjectId targetOid = new ObjectId(oid);
        return dataObjList.stream()
            .dropWhile(dataObj -> !dataObj.get("_id", ObjectId.class).equals(targetOid))
            .limit(1)
            .findFirst();
      }
      return Optional.empty();
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * 最終ブロックデータを取得
   *
   * @param objName
   * @return
   */
  public Optional<Document> getLastBlockData() {
    try {
      Document doc = this.block.find().sort(new BasicDBObject("$natural", -1)).limit(1).first();
      if (doc != null) {
        doc.remove(ConstItem.BLOCK_DATA);
      }
      return Optional.ofNullable(doc);
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * ブロックの修正・削除データを検索
   *
   * @param objName
   * @return
   */
  public List<String> getBlockModifyDeleteList() {
    // 削除されたデータと修正されたデータをブロックから取得
    final Bson deleteOrModify = queryHelper.getDBObjectDeleteOrModify("data.type");
    try {
      return StreamSupport.stream(this.block.find(deleteOrModify).spliterator(), true)
          .map(
              deleteObj ->
                  deleteObj.getList(ConstItem.BLOCK_DATA, Document.class).stream()
                      .map(x -> x.get(ConstItem.DATA_ORIGINAL_ID, String.class))
                      .filter(x -> x != null)
                      .collect(Collectors.toList()))
          .flatMap(Collection::stream)
          .collect(Collectors.toList());
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * ブロックから指定項目とObject値でデータを取得
   *
   * @param objName
   * @param key
   * @param value
   * @param modifyDeleteList
   * @return
   */
  public List<Document> getBlockByKeyValue(
      String key, Object value, List<String> modifyDeleteList) {
    final List<Document> list = new ArrayList<>();

    final Bson newOrModify = queryHelper.getDBObjectNewOrModify("data.type");

    String keyString = "data." + key;
    final Bson keyIsValue = eq(keyString, value);
    final Bson searchQuery = and(keyIsValue, newOrModify);

    try {
      StreamSupport.stream(this.block.find(searchQuery).spliterator(), true)
          .map(blockCursor -> blockCursor.getList(ConstItem.BLOCK_DATA, Document.class))
          .forEach(
              dataObjList ->
                  dataObjList.stream()
                      .filter(
                          dataObj ->
                              dataObj.get(ConstItem.DATA_TYPE).equals(ConstType.NEW)
                                  || dataObj.get(ConstItem.DATA_TYPE).equals(ConstType.MODIFY))
                      .forEach(
                          dataObj -> {
                            // NewとModifyのデータに絞り込み
                            String key_tmp = key;
                            Document dataObj_tmp = dataObj;
                            String[] nestKeyList = key_tmp.split(Pattern.quote("."));
                            boolean findValueFlag = true;

                            // 検索ネストの最下層の一個うえまで
                            for (final int i :
                                IntStream.range(0, nestKeyList.length - 1).toArray()) {
                              if (dataObj.get(nestKeyList[i]) == null) {
                                findValueFlag = false;
                                break;
                              } else {
                                dataObj = dataObj.get(nestKeyList[i], Document.class);
                                key_tmp = nestKeyList[i + 1];
                              }
                            }

                            Object objValue = dataObj.get(key_tmp);
                            // 検索した値が指定した値と同じ
                            if (findValueFlag && objValue != null && objValue.equals(value)) {
                              list.add(dataObj_tmp);
                              final Document dataObjFinal = dataObj;
                              modifyDeleteList.stream()
                                  .forEach(
                                      deleteId -> {
                                        if (dataObj_tmp.get("_id").equals(new ObjectId(deleteId))) {
                                          list.remove(dataObjFinal);
                                        }
                                      });
                            }
                          }));
      return list;
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * ブロックデータの全ハッシュ情報取得
   *
   * @param objName
   * @return
   */
  public List<String> getBlockDataHashList() {
    return StreamSupport.stream(this.block.find().spliterator(), false)
        .map(obj -> obj.get("hash", String.class))
        .collect(Collectors.toList());
  }

  public List<Document> getBlockByKeyCryptoValue(
      final String key, final String value, final List<String> modifyDeleteList) {
    final List<Document> list = new ArrayList<>();
    // リストを取得
    final Bson queryNewOrModify = queryHelper.getDBObjectNewOrModify("data.type");

    final String keyString = "data." + key;
    final Bson queryRegex = regex(keyString, value);
    final Bson searchQuery = and(queryRegex, queryNewOrModify);

    try {
      StreamSupport.stream(this.block.find(searchQuery).spliterator(), true)
          .map(blockCursor -> blockCursor.getList(ConstItem.BLOCK_DATA, Document.class))
          .forEach(
              dataObjList ->
                  dataObjList.stream()
                      .filter(
                          dataObj ->
                              dataObj.get(ConstItem.DATA_TYPE).equals(ConstType.NEW)
                                  || dataObj.get(ConstItem.DATA_TYPE).equals(ConstType.MODIFY))
                      .forEach(
                          dataObj -> {
                            String key_tmp = key;
                            final Document dataObj_tmp = dataObj;
                            String[] nestKeyList = key_tmp.split(Pattern.quote("."));
                            boolean findValueFlag = true;

                            // 検索ネストの最下層の一個うえまで
                            for (final int i :
                                IntStream.range(0, nestKeyList.length - 1).toArray()) {
                              if (dataObj.get(nestKeyList[i]) == null) {
                                findValueFlag = false;
                                break;
                              } else {
                                dataObj = dataObj.get(nestKeyList[i], Document.class);
                                key_tmp = nestKeyList[i + 1];
                              }
                            }
                            if (findValueFlag && dataObj.get(key_tmp) instanceof String) {
                              String objValue = dataObj.get(key_tmp, String.class);
                              // 指定のハッシュ値で始まるデータを取得
                              if (objValue.startsWith(value.replace("^", ""))) { // 正規表現のため付与した^を削除
                                list.add(dataObj_tmp);
                                final Document dataObjFinal = dataObj;
                                modifyDeleteList.stream()
                                    .forEach(
                                        deleteId -> {
                                          if (dataObj_tmp
                                              .get("_id")
                                              .equals(new ObjectId(deleteId))) {
                                            list.remove(dataObjFinal);
                                          }
                                        });
                              }
                            }
                          }));
      return list;
    } catch (MongoTimeoutException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }
}

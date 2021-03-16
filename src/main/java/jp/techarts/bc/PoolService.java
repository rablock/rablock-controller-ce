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

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import java.util.List;
import java.util.Optional;
import javax.annotation.PostConstruct;
import jp.techarts.bc.constitem.ConstJsonType;
import jp.techarts.bc.constitem.ConstType;
import jp.techarts.bc.data.PoolRepository;
import jp.techarts.bc.prop.GetAppProperties;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * MongoDBのプールコレクションのサービスクラス<br>
 * Copyright (c) 2018 Rablock Inc.
 *
 * @author TA
 * @version 1.0
 */
@Service
public class PoolService {
  private final Logger log = LoggerFactory.getLogger(PoolService.class);

  private final ApplicationContext appCtx;
  private final PoolRepository dao;
  private BlockService blockService;
  private final Common common;

  private final String cryptoStatus;

  @Autowired
  public PoolService(
      final GetAppProperties app,
      final ApplicationContext appCtx,
      final PoolRepository dao,
      final Common common) {
    this.appCtx = appCtx;
    this.dao = dao;
    this.common = common;

    log.info("mode:for community");

    cryptoStatus = app.getCryptoStatus();
  }

  /** 起動時の処理<br> */
  @PostConstruct
  public void initAfterStartup() {
    this.blockService = appCtx.getBean(BlockService.class);
  }

  /**
   * トランザクションデータの登録
   *
   * @param data
   * @return
   * @throws RablockSystemException
   */
  public InsertOneResult insertTranData(Document data) throws RablockSystemException {
    // トランザクションデータの登録
    return dao.insertTranData(data);
  }

  /**
   * トランザクションデータの削除
   *
   * @param objName
   * @param data
   * @return
   * @throws RablockSystemException
   */
  public DeleteResult removeTranData(Document data) throws RablockSystemException {
    // トランザクションデータの削除
    return dao.removeTranData(data);
  }

  /**
   * 指定のOidでトランザクションデータを削除
   *
   * @param oid
   * @return
   * @throws RablockSystemException
   */
  public DeleteResult removeTranDataByOid(String oid) throws RablockSystemException {
    // 指定のOidでトランザクションデータを削除
    return dao.removeTranDataByOid(oid);
  }

  /**
   * DeliveryFがtrueのトランザクションデータを削除
   *
   * @return
   * @throws RablockSystemException
   */
  public DeleteResult removeDeliveredTranData() throws RablockSystemException {
    // 指定のOidでトランザクションデータを削除
    return dao.removeDeliveredTranData();
  }

  /**
   * PoolコレクションのOidデータ全件取得
   *
   * @param db_name データベース名
   * @param collection_pool_name プールコレクション名
   * @return PoolコレクションのOidデータ全件
   * @throws RablockSystemException
   */
  public List<ObjectId> getAllPoolOid() throws RablockSystemException {
    // 全トランザクションデータの取得
    return dao.getTranDataObjIDList();
  }

  /**
   * PoolコレクションのDeliveryFがFALSEのデータ取得（伝搬時）
   *
   * @return PoolコレクションのDeliveryFがFALSEのデータ全件
   * @throws RablockSystemException
   */
  public List<Document> getNotDeliveredData() throws RablockSystemException {
    // トランザクションデータのDeliveryFがFALSEのデータ取得
    return dao.getNotTranDeliveredDataList();
  }

  /**
   * PoolコレクションのDeliveryFを更新
   *
   * @param dbUpdateObj
   * @param status
   * @return
   * @throws RablockSystemException
   */
  public UpdateResult updateDeliveryStatus(Document dbUpdateObj, boolean status)
      throws RablockSystemException {
    // トランザクションデータのDeliveryFを更新
    return dao.updateTranDeliveryStatus(dbUpdateObj, status);
  }

  /**
   * トランザクションプールにデータを登録する
   *
   * @param JSONObject 登録するデータ
   * @param tx_time トランザクションタイム
   * @param itemList タイプに応じたアイテムリスト
   * @return doc DBObject型のデータ
   * @throws RablockSystemException
   */
  public Document setPool(JSONObject jSONObject, String currentTime, List<String> itemList)
      throws RablockSystemException {
    final Document doc = Document.parse(jSONObject.toString());
    itemList
        .parallelStream()
        .forEach(
            itemName -> {
              if (jSONObject.isNull(itemName)) {
                doc.replace(itemName, null, null);
              } else if (jSONObject.get(itemName).getClass().equals(ConstJsonType.JSONOBJECT)) {
                String getData = jSONObject.get(itemName).toString();
                if (common.nestCheck(getData)) {
                  Document nestData = common.nestProcess(getData, new Document());
                  doc.replace(itemName, jSONObject.get(itemName), nestData);
                }
              } else {
                String getData = jSONObject.get(itemName).toString();
                if (common.nestCheck(getData) && this.cryptoStatus.equals("ON")) {
                  Document nestData = common.nestProcess(getData, new Document());
                  doc.replace(itemName, jSONObject.get(itemName), nestData);
                }
              }
            });

    // 登録日時と伝播フラグを設定
    doc.append("settime", currentTime).append("deliveryF", false);
    dao.insertTranData(doc);
    log.info("トランザクションデータが追加されました。", doc);
    return doc;
  }

  /**
   * トランザクションプールにデータを登録する
   *
   * @param jSONObject 登録するデータ
   * @param currentTime トランザクションタイム
   * @return doc DBObject型のデータ
   * @throws RablockSystemException
   */
  public Document setPool(JSONObject jSONObject, String currentTime) throws RablockSystemException {
    // 登録対象のデータをmongoDB登録用のデータ型に変更
    Document doc = Document.parse(jSONObject.toString());
    // 登録日時と伝播フラグを設定
    doc.append("settime", currentTime).append("deliveryF", false);
    dao.insertTranData(doc);
    return doc;
  }

  /**
   * Poolコレクションのデータ全件取得
   *
   * @param db_name データベース名
   * @param collection_pool_name プールコレクション名
   * @return Poolコレクションのデータ全件
   * @throws RablockSystemException
   */
  public List<Document> getAllPool() throws RablockSystemException {
    // 全トランザクションデータの取得
    return dao.getTranDataList();
  }

  /**
   * PoolコレクションのDeliveryFがTRUEのデータ取得（マイニング時）
   *
   * @return PoolコレクションのDeliveryFがTRUEのデータ全件
   * @throws RablockSystemException
   */
  public List<Document> getDeliveredData() throws RablockSystemException {
    // トランザクションデータのDeliveryFがTRUEのデータ取得
    return dao.getTranDeliveredDataList();
  }

  /**
   * PoolテーブルからオブジェクトIDでデータを取得
   *
   * @param oid オブジェクトID
   * @return dataList 指定したオブジェクトIDのデータ
   * @throws RablockSystemException
   */
  public Optional<Document> getDataByOidinPool(String oid) throws RablockSystemException {
    // トランザクションデータのオブジェクトID検索
    return dao.getTranDataByOid("_id", oid);
  }

  /**
   * 修正・削除データをPoolから検索
   *
   * @return deleteList 指定したデータの修正・削除データリスト
   * @throws RablockSystemException
   */
  public List<String> getModifyDeleteListinPool() throws RablockSystemException {
    // トランザクションの修正・削除データを検索
    return dao.getTranModifyDeleteList();
  }

  /**
   * プールから指定の項目と値でデータを取得する
   *
   * @param key String項目
   * @param jsonnode Json形式のObject
   * @return dataList データリスト
   * @throws RablockSystemException
   */
  public List<Document> getByKeyValue(String key, JsonNode jsonnode) throws RablockSystemException {
    // 削除リスト取得
    List<String> modifyDeleteList = blockService.getModifyDeleteListinBlock();
    List<String> poolModifyDeleteList = getModifyDeleteListinPool();
    modifyDeleteList.addAll(poolModifyDeleteList);

    // トランザクションの指定項目と値でデータを取得
    return dao.getTranByKeyValue(key, jsonnode, modifyDeleteList);
  }

  /**
   * プールから指定の項目と値で暗号化されたデータを取得する
   *
   * @param key String項目
   * @param jsonnode Json形式のObject
   * @return dataList データリスト
   * @throws RablockSystemException
   */
  public List<Document> getCryptoByKeyValue(String key, String value)
      throws RablockSystemException {
    // 削除リスト取得
    List<String> modifyDeleteList = blockService.getModifyDeleteListinBlock();
    List<String> poolModifyDeleteList = getModifyDeleteListinPool();
    modifyDeleteList.addAll(poolModifyDeleteList);
    // トランザクションの指定項目と値でデータを取得
    return dao.getCryptoTranByKeyValue(key, value, modifyDeleteList);
  }

  /**
   * 指定の項目とString値でデータを取得する
   *
   * @param key String項目
   * @param value String
   * @return dataList データリスト
   * @throws RablockSystemException
   */
  public List<Document> getByKeyStringValue(String key, String value)
      throws RablockSystemException {
    // 削除リスト取得
    List<String> modifyDeleteList = blockService.getModifyDeleteListinBlock();
    List<String> poolModifyDeleteList = getModifyDeleteListinPool();
    modifyDeleteList.addAll(poolModifyDeleteList);
    // トランザクションの指定項目とString値でデータを取得
    return dao.getTranByKeyStringValue(key, value, modifyDeleteList);
  }
}

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

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import java.util.List;
import java.util.Optional;
import javax.annotation.PostConstruct;
import jp.techarts.bc.data.BlockRepository;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * MongoDBのブロックコレクションのサービスクラス<br>
 * Copyright (c) 2018 Rablock Inc.
 *
 * @author TA
 * @version 1.0
 */
@Service
public class BlockService {
  private final Logger log = LoggerFactory.getLogger(BlockService.class);

  private final ApplicationContext appCtx;
  private final BlockRepository dao;
  private PoolService poolService;
  private final Common common;

  @Autowired
  public BlockService(
      final ApplicationContext app, final BlockRepository dao, final Common common) {
    this.appCtx = app;
    this.dao = dao;
    this.common = common;
  }

  @PostConstruct
  public void init() {
    this.poolService = appCtx.getBean(PoolService.class);
  }

  /**
   * ブロックデータの全ハッシュ情報取得
   *
   * @param key
   * @param value
   * @return
   * @throws RablockSystemException
   */
  public List<String> getBlockDataHashList() throws RablockSystemException {
    // ブロックデータの全ハッシュ情報取得
    return dao.getBlockDataHashList();
  }

  /**
   * ブロックデータの登録
   *
   * @param data
   * @return
   * @throws RablockSystemException
   */
  public InsertOneResult insertBlockData(Document data) throws RablockSystemException {
    return dao.insertBlockData(data);
  }

  /**
   * ブロックヘッダー項目のoid値でブロックを取得
   *
   * @param key 検索する項目名
   * @param value 項目の値
   * @return
   * @throws RablockSystemException
   */
  public Optional<Document> getBlockDataByOid(String key, String oid)
      throws RablockSystemException {
    // ブロックヘッダー項目の値でブロックを取得
    return dao.getBlockDataByOid(key, oid);
  }

  /**
   * 指定されたキー情報でブロックを削除する
   *
   * @param key
   * @param value
   * @return
   * @throws RablockSystemException
   */
  public DeleteResult removeBlockByKeyValue(String key, String value)
      throws RablockSystemException {
    // ブロックデータの削除
    Document query = new Document(key, value);
    return dao.removeBlockData(query);
  }

  /**
   * 指定されたブロックを更新する
   *
   * @param queryBlock
   * @param updateBlock
   * @return
   * @throws RablockSystemException
   */
  public UpdateResult updateBlock(Document queryBlock, Document updateBlock)
      throws RablockSystemException {
    return dao.updateBlockData(queryBlock, updateBlock);
  }

  /**
   * 指定されたブロックをキー情報で更新する
   *
   * @param key
   * @param value
   * @param updateBlock
   * @return
   * @throws RablockSystemException
   */
  public UpdateResult updateBlockByKeyValue(String key, String value, Document updateBlock)
      throws RablockSystemException {
    return dao.updateBlockByKeyValue(key, value, updateBlock);
  }

  /**
   * ジェネシスブロックを登録する。
   *
   * @throws RablockSystemException
   */
  public Document setGenBlock() throws RablockSystemException {
    Document doc =
        new Document("height", 0)
            .append("size", 0)
            .append("settime", common.getCurrentTime())
            .append("timestamp", common.getTimestamp())
            .append("prev_hash", "0");
    String hashString = doc.toJson();
    // ハッシュ値を作成
    String hashValue = common.hashCal(hashString);
    doc = doc.append("hash", hashValue);
    // ブロックチェーンに登録
    dao.insertBlockData(doc);
    return doc;
  }

  /**
   * 最後のブロックを取得する
   *
   * @return blockCursor 最後のブロック
   * @throws RablockSystemException
   */
  public Optional<Document> getLastBlock() throws RablockSystemException {
    // 最終ブロックデータを取得
    return dao.getLastBlockData();
  }

  /**
   * Blockコレクションのブロック全件取得
   *
   * @param db_name データベース名
   * @param collection_block_name ブロックコレクション名
   * @return Blockコレクションのブロック全件
   * @throws RablockSystemException
   */
  public List<Document> getAllBlock() throws RablockSystemException {
    // ブロックデータの全情報取得
    return dao.getBlockDataList();
  }

  /**
   * ブロックヘッダー項目の値でブロックを取得
   *
   * @param key 検索する項目名
   * @param value 項目の値
   * @return
   * @throws RablockSystemException
   */
  public Optional<Document> getBlockByKeyValue(String key, String value)
      throws RablockSystemException {
    // ブロックヘッダー項目の値でブロックを取得
    return dao.getBlockByKeyValue(key, value);
  }
  /**
   * ブロック件数を取得する
   *
   * @return blockCount ブロックの数
   * @throws RablockSystemException
   */
  public int getBlockCount() throws RablockSystemException {
    // ブロックデータ件数を取得
    return dao.getBlockCount();
  }

  /**
   * 指定されたブロックを削除する
   *
   * @param block
   * @throws RablockSystemException
   */
  public void removeBlock(Document block) throws RablockSystemException {
    // ブロックデータの削除
    dao.removeBlockData(block);
  }

  /**
   * Blockコレクションのブロックのデータ全件取得
   *
   * @param db_name データベース名
   * @param collection_block_name ブロックコレクション名
   * @return Blockコレクションのデータ全件
   * @throws RablockSystemException
   */
  public List<Document> getAllBlockData() throws RablockSystemException {
    // Blockコレクションのブロックのデータ全件取得
    return dao.getAllBlockDataList();
  }

  /**
   * blockコレクションからoidでデータを取得
   *
   * @param oid オブジェクトID
   * @return DBObject
   * @throws RablockSystemException
   */
  public Optional<Document> findByOidinBlock(String oid) throws RablockSystemException {
    // ブロックデータのオブジェクトID検索
    return dao.getBlockDataByOid(oid);
  }

  /**
   * 項目と値で修正・削除データをblockから検索
   *
   * @param key 項目
   * @param value 値
   * @return deleteList 修正・削除データリスト
   * @throws RablockSystemException
   */
  public List<String> getModifyDeleteListinBlock() throws RablockSystemException {
    // ブロックの修正・削除データを検索
    return dao.getBlockModifyDeleteList();
  }

  /**
   * 指定のString項目と値でデータを取得する
   *
   * @param key String項目
   * @param value 値
   * @return dataList データリスト
   * @throws RablockSystemException
   */
  public List<Document> getByKeyValue(String key, Object value) throws RablockSystemException {
    // 削除リスト取得
    List<String> modifyDeleteList = getModifyDeleteListinBlock();
    List<String> poolModifyDeleteList = poolService.getModifyDeleteListinPool();
    modifyDeleteList.addAll(poolModifyDeleteList);
    // ブロックの修正・削除データを検索
    return dao.getBlockByKeyValue(key, value, modifyDeleteList);
  }

  /**
   * 暗号化されたデータをハッシュ計算した文字列で前方一致検索する
   *
   * @param key
   * @param value
   * @return
   * @throws RablockSystemException
   */
  public List<Document> getCryptoByKeyValue(String key, String value)
      throws RablockSystemException {
    // 削除リスト取得
    List<String> modifyDeleteList = getModifyDeleteListinBlock();
    List<String> poolModifyDeleteList = poolService.getModifyDeleteListinPool();
    modifyDeleteList.addAll(poolModifyDeleteList);
    // ブロックの修正・削除データを検索
    return dao.getBlockByKeyCryptoValue(key, value, modifyDeleteList);
  }
}

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
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import javax.annotation.PostConstruct;
import jp.techarts.bc.BlockService;
import jp.techarts.bc.Destination;
import jp.techarts.bc.PoolService;
import jp.techarts.bc.RablockSystemException;
import jp.techarts.bc.ResolveService;
import jp.techarts.bc.constitem.ConstItem;
import jp.techarts.bc.constitem.ConstType;
import jp.techarts.bc.grobal.Global;
import jp.techarts.bc.prop.GetAppProperties;
import jp.techarts.bc.prop.IpProperties;
import jp.techarts.bc.prop.PortProperties;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * ReceiveServerAPIの実装クラス<br>
 * Copyright (c) 2018 Rablock Inc.
 *
 * @author TA
 * @version 1.0
 */
@Service
@AutoJsonRpcServiceImpl
public class ReceiveServerAPIImpl implements ReceiveServerAPI {
  private final Logger log = LoggerFactory.getLogger(ReceiveServerAPIImpl.class);

  private final IpProperties ip;
  private final PortProperties port;

  private final BlockService blockService;
  private final PoolService poolService;
  private final ResolveService resolveService;

  private final List<Destination> destinationList = new ArrayList<>();

  private final String digestUserName;
  private final String digestPass;

  private final SendJson sendJson;

  @Autowired
  public ReceiveServerAPIImpl(
      final GetAppProperties app,
      final IpProperties ip,
      final PortProperties port,
      final BlockService blockService,
      final PoolService poolService,
      final ResolveService resolveService,
      final SendJson sendJson) {
    this.ip = ip;
    this.port = port;
    this.blockService = blockService;
    this.poolService = poolService;
    this.resolveService = resolveService;
    this.sendJson = sendJson;
    digestUserName = app.getDigestUserName();
    digestPass = app.getDigestPass();
  }

  /** 起動時の処理<br> */
  @PostConstruct
  public void initAfterStartup() {
    // 送信先IPアドレス
    final List<String> sendIpArray = ip.getIp();
    // 送信先ポート番号
    final List<String> sendPortArray = port.getPort();
    destinationList.addAll(
        IntStream.range(0, sendIpArray.size())
            .mapToObj(i -> new Destination(sendIpArray.get(i), sendPortArray.get(i)))
            .collect(Collectors.toList()));
  }

  /**
   * トランザクションデータの受信
   *
   * @throws RablockSystemException
   */
  @Override
  public String receivePool(JsonNode list) throws RablockSystemException {
    synchronized (Global.lock) {
      // PoolコレクションのOidデータ全件取得
      List<ObjectId> idList = poolService.getAllPoolOid();

      StreamSupport.stream(list.get("data").spliterator(), true)
          .filter(
              n -> {
                String id = n.get("_id").get("$oid").asText();
                ObjectId objectId = new ObjectId(id);
                return !idList.contains(objectId);
              })
          .map(n -> (Document) Document.parse(n.toString()).put("deliveryF", true))
          .forEach(
              json ->
                  // トランザクションデータの登録
                  poolService.insertTranData(json));
    }
    return "OK";
  }

  /**
   * ブロックデータの受信
   *
   * @throws RablockSystemException
   */
  @Override
  public String receiveBlock(JsonNode block) throws RablockSystemException {
    Document blockObj = Document.parse(block.toString());
    synchronized (Global.lock) {
      // ジェネシスブロック受け取り時の処理
      if (blockObj.get("prev_hash").equals("0")) {
        // mongoへ格納
        if (blockService
            .getBlockByKeyValue("hash", blockObj.get("hash", String.class))
            .isPresent()) {
          log.info("すでに同じブロックがあるため追加できません。 コード{}", 8106);
          return "NG";
        }
        // ブロックデータの登録
        blockService.insertBlockData(blockObj);
        return "OK";
      }

      if (blockService.getBlockByKeyValue("hash", blockObj.get("hash", String.class)).isPresent()) {
        log.info("すでに同じブロックがあるため追加できません。 コード{}", 8107);
        return "NG";
      }

      if (blockService
          .getBlockByKeyValue("hash", blockObj.get("prev_hash", String.class))
          .isEmpty()) {
        log.info("親ブロックが見つからないので追加できません。 コード{}", 8108);
        return "NG";
      }

      // ブロックデータの登録
      blockService.insertBlockData(blockObj);
      // 受け取ったトランザクションを自ノードのプールから削除する
      StreamSupport.stream(block.get(ConstItem.BLOCK_DATA).spliterator(), true)
          .forEach(
              n -> {
                JsonNode id = n.get("_id");
                String oid = id.get("$oid").asText();

                // 指定のOidでトランザクションデータを削除
                poolService.removeTranDataByOid(oid);
              });
    }
    return "OK";
  }

  /**
   * ブロックデータを受信し、同時に配信を実行
   *
   * @throws RablockSystemException
   */
  @Override
  public String receiveSendBlock(JsonNode block) throws RablockSystemException {
    Document blockObj = Document.parse(block.toString());
    synchronized (Global.lock) {
      if (blockService.getBlockByKeyValue("hash", blockObj.get("hash", String.class)).isPresent()) {
        log.info("すでに同じブロックがあるため追加できません。 コード{}", 8109);
        return "NG";
      }

      if (blockService
          .getBlockByKeyValue("hash", blockObj.get("prev_hash", String.class))
          .isEmpty()) {
        log.info("親ブロックが見つからないので追加できません。 コード{}", 8110);
        return "NG";
      }

      // ブロックデータの登録
      blockService.insertBlockData(blockObj);

      // 受け取ったトランザクションを自ノードのプールから削除する
      StreamSupport.stream(block.get(ConstItem.BLOCK_DATA).spliterator(), true)
          .forEach(
              n -> {
                JsonNode id = n.get("_id");
                String oid = id.get("$oid").asText();

                // 指定のOidでトランザクションデータを削除
                poolService.removeTranDataByOid(oid);
              });

      return destinationList
          .parallelStream()
          .map(
              dest -> {
                final String sendIp_res =
                    sendJson.nodeSendBlock(
                        blockObj, dest.ip, dest.port, digestUserName, digestPass);
                if (sendIp_res.equals("NG")) {
                  log.warn(dest.ip + "ブロックを送信できませんでした。 コード{}", 4107);
                }
                return sendIp_res;
              })
          .filter(res -> res.equals("NG"))
          .findFirst()
          .orElse("OK");
    }
  }

  /**
   * トランザクションデータのコピー
   *
   * @throws RablockSystemException
   */
  @Override
  public String copyPool() throws RablockSystemException {
    // PoolコレクションのDeliveryFがTRUEのデータ取得
    List<Document> list = poolService.getDeliveredData();
    return new Document("pool", list).toJson();
  }

  /**
   * ブロックデータのコピー
   *
   * @throws RablockSystemException
   */
  @Override
  public String copyBlock() throws RablockSystemException {
    // Blockコレクションのブロック全件取得
    List<Document> list = blockService.getAllBlock();
    return new Document("block", list).toJson();
  }

  /**
   * 指定のハッシュデータでブロックデータを削除
   *
   * @throws RablockSystemException
   * @throws JsonProcessingException
   */
  @Override
  public String receiveDeleteBlock(String hash)
      throws RablockSystemException, JsonProcessingException {
    synchronized (Global.lock) {
      Optional<Document> blockOrEmpty = blockService.getBlockByKeyValue("hash", hash);
      if (blockOrEmpty.isPresent()) {
        Document block = blockOrEmpty.get();
        log.info(hash + "枝分かれデータを削除。 コード{}", 8300);
        blockService.removeBlock(block);

        List<Document> dataObjList = block.getList(ConstItem.BLOCK_DATA, Document.class);

        // プールに戻す処理
        resolveService.backToPool(dataObjList);

        return "OK";
      }
    }
    return "NG";
  }

  /**
   * 指定のハッシュ値のブロックデータを確認
   *
   * @throws RablockSystemException
   */
  @Override
  public String receiveCheckBlockbyHash(String hash)
      throws JsonProcessingException, RablockSystemException {
    // ブロックヘッダー項目の値でブロックを取得
    Optional<Document> blockObj = blockService.getBlockByKeyValue("hash", hash);
    if (blockObj.isPresent()) {
      return blockObj.get().toJson();
    }
    return "BLOCK_NOT_FOUND";
  }

  /**
   * 指定の親ハッシュ値のブロックデータを確認
   *
   * @throws RablockSystemException
   */
  @Override
  public String receiveCheckBlockbyPrevHash(String prevHash)
      throws JsonProcessingException, RablockSystemException {
    // ブロックヘッダー項目の値でブロックを取得
    Optional<Document> blockObj = blockService.getBlockByKeyValue("prev_hash", prevHash);

    if (blockObj.isPresent()) {
      return blockObj.get().toJson();
    }
    return "NG";
  }

  /**
   * ブロックデータ件数の取得
   *
   * @throws RablockSystemException
   */
  @Override
  public int getBlockCount() throws RablockSystemException {
    return blockService.getBlockCount();
  }

  /**
   * ハッシュリストの取得
   *
   * @throws RablockSystemException
   */
  @Override
  public String getHashList() throws RablockSystemException {
    // ブロックデータの全ハッシュ情報取得
    List<String> hashList = blockService.getBlockDataHashList();
    return new Document("hash", hashList).toJson();
  }
}

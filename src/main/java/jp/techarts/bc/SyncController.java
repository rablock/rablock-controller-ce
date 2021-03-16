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

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.PostConstruct;
import jp.techarts.bc.constitem.ConstType;
import jp.techarts.bc.grobal.Global;
import jp.techarts.bc.jsonrpc.SendJson;
import jp.techarts.bc.prop.GetAppProperties;
import jp.techarts.bc.prop.IpProperties;
import jp.techarts.bc.prop.PortProperties;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * コントローラークラス<br>
 * Copyright (c) 2018 Rablock Inc.
 *
 * @author TA
 * @version 1.0
 */
@RestController
@RequestMapping("/sync")
@Api(tags = "Synchronize", value = "Sync")
public class SyncController {
  private final Logger log = LoggerFactory.getLogger(SyncController.class);

  private final IpProperties ip;
  private final PortProperties port;

  private final BlockService blockService;
  private final PoolService poolService;
  private final ResolveService resolveService;
  private final MajorNode majorNode;

  private final List<Destination> destinationList = new ArrayList<>();

  /** プールコレクション名 */
  private final String collection_pool_name;

  private final String digestUserName;
  private final String digestPass;

  private final SendJson sendJson;

  @Autowired
  public SyncController(
      final GetAppProperties app,
      final IpProperties ip,
      final PortProperties port,
      final BlockService blockService,
      final PoolService poolService,
      final ResolveService resolveService,
      final SendJson sendJson,
      final MajorNode majorNode) {
    this.ip = ip;
    this.port = port;
    this.blockService = blockService;
    this.poolService = poolService;
    this.resolveService = resolveService;
    this.sendJson = sendJson;
    this.majorNode = majorNode;

    digestUserName = app.getDigestUserName();
    digestPass = app.getDigestPass();
    // データベース名、コレクション名を設定
    collection_pool_name = app.getMongodb_coll_pool();
  }

  /**
   * 起動時の処理<br>
   * 他ノード連携<br>
   */
  @PostConstruct
  public void initAfterStartup() {
    // 送信先IPアドレス
    List<String> sendIpArray = ip.getIp();
    // 送信先ポート番号
    List<String> sendPortArray = port.getPort();
    destinationList.addAll(
        IntStream.range(0, sendIpArray.size())
            .mapToObj(i -> new Destination(sendIpArray.get(i), sendPortArray.get(i)))
            .collect(Collectors.toList()));
  }

  /**
   * ジェネシスブロックを生成し伝搬する
   *
   * @return OK 正常終了
   * @throws RablockSystemException
   */
  @RequestMapping(
      value = "/gen",
      method = {RequestMethod.POST})
  @ApiOperation(value = "ジェネシスブロックを生成し伝搬する")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "OK 正常終了")})
  private String setGenBlock() throws RablockSystemException {
    synchronized (Global.lock) {
      // ブロック件数を取得する
      int blockCount = 0;
      blockCount = blockService.getBlockCount();
      if (blockCount != 0) {
        log.info("ブロックが存在するためジェネシスブロックを追加することができません。 コード{}", 8100);
        return "ブロックが存在するためジェネシスブロックを追加することができません。";
      }

      // 他ノードが0件か確認する
      List<String> resultArray = new ArrayList<>();

      // 他ノード接続確認
      Optional<String> connectionFailedMessage =
          destinationList
              .parallelStream()
              .map(
                  dest -> {
                    String result =
                        sendJson.sendCopy(
                            dest.ip, "copyBlock", dest.port, digestUserName, digestPass);
                    if (result.equals("NG")) {
                      log.warn(dest.ip + "に接続できないのでジェネシスブロックを生成できません。 コード{}", 1001);
                      return dest.ip + "に接続できないのでジェネシスブロックを生成できません。";
                    }
                    log.info(dest.ip + "の通信結果：：" + result + " コード{}", 5001);
                    resultArray.add(result);
                    return null;
                  })
              .filter(x -> x != null)
              .findFirst();
      if (connectionFailedMessage.isPresent()) {
        return connectionFailedMessage.get();
      }

      // 他ノードジェネシスブロック存在確認
      Optional<String> genesisBlockAvailableMessage =
          resultArray
              .parallelStream()
              .map(
                  res -> {
                    Document sendIp_blockData = Document.parse(res);
                    if (sendIp_blockData.getList("block", Document.class).get(0) != null) {
                      log.info(res + "にブロックがあるためジェネシスブロックを追加できません。 コード{}", 8101);
                      return res + "にブロックがあるためジェネシスブロックを追加できません。";
                    }
                    return null;
                  })
              .filter(x -> x != null)
              .findFirst();
      if (genesisBlockAvailableMessage.isPresent()) {
        return genesisBlockAvailableMessage.get();
      }

      // ブロック生成・自ノードに追加
      Document doc = blockService.setGenBlock();

      // 他ノードに送信
      destinationList
          .parallelStream()
          .forEach(
              dest -> {
                String send_res =
                    sendJson.nodeSendBlock(doc, dest.ip, dest.port, digestUserName, digestPass);
                if (send_res.equals("NG")) {
                  log.warn(dest.ip + "ブロックを送信できませんでした。 コード{}", 1002);
                }
              });
    }
    return "OK";
  }

  /**
   * 他ノードと差異がある時、差分をコピーする
   *
   * @return 正常終了：OK + 詳細
   * @throws RablockSystemException
   * @throws JsonProcessingException
   */
  @RequestMapping(
      value = "/poolsync",
      method = {RequestMethod.POST})
  @ApiOperation(value = "他ノードと差異がある時、差分をコピーする")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "OK + 詳細")})
  private String poolDiffCopy() throws RablockSystemException, JsonProcessingException {

    String result;

    synchronized (Global.lock) {
      // 他ノード通信確認
      // 他ノードのプールコレクション取得
      long ngConNodeCount =
          destinationList
              .parallelStream()
              .map(
                  dest -> {
                    final String sendIp_res =
                        sendJson.sendCopy(
                            dest.ip, "copyPool", dest.port, digestUserName, digestPass);
                    if (sendIp_res.equals("NG")) {
                      log.warn(sendIp_res + "との通信に失敗したので差分コピーできません。 コード{}", 1003);
                      return false;
                    }
                    log.info(dest.ip + "の通信結果：：" + sendIp_res + " コード{}", 5002);
                    return true;
                  })
              .filter(x -> x == false)
              .count();

      if (ngConNodeCount == destinationList.size()) {
        return "全ての他ノード通信に失敗したので差分コピーできません。";
      }

      // プール（伝搬済データ）をコピー
      result = diffPoolCopy();
    }
    return result;
  }

  /**
   * トランザクションプールの差分コピーをする
   *
   * @return
   * @throws RablockSystemException
   * @throws JsonProcessingException
   */
  private String diffPoolCopy() throws RablockSystemException, JsonProcessingException {
    int count = 0;
    ObjectMapper mapper = new ObjectMapper();
    // PoolコレクションのOidデータ全件取得
    List<ObjectId> list = poolService.getAllPoolOid();

    boolean copyFlag = false;

    // copyPool:DeliveryFがTrueのデータのみコピー
    for (final Destination dest : destinationList) {
      String sendIp_res =
          sendJson.sendCopy(dest.ip, "copyPool", dest.port, digestUserName, digestPass);

      if (!sendIp_res.equals("NG")) {
        // 自ノードのDeliveryFがtrueのトランザクションデータを削除する
        poolService.removeDeliveredTranData();
        JsonNode sendIp_data = mapper.readTree(sendIp_res);
        // 受け取ったトランザクションを登録する
        count +=
            StreamSupport.stream(sendIp_data.get(collection_pool_name).spliterator(), false)
                .map(
                    n -> {
                      String id = n.get("_id").get("$oid").asText();
                      ObjectId objectId = new ObjectId(id);
                      if (!list.contains(objectId)) {
                        Document json = Document.parse(n.toString());
                        // トランザクションデータの登録
                        poolService.insertTranData(json);
                        return true;
                      }
                      return false;
                    })
                .filter(result -> result == true)
                .count();
        copyFlag = true;
      } else {
        log.warn(dest.ip + "::プールの差分コピーに失敗しました。。 コード{}", 1004);
        copyFlag = false;
        return "プールの差分コピーに失敗しました。";
      }
      if (copyFlag) {
        break;
      }
    }

    if (count == 0) {
      log.info("差分はありません。 コード{}", 7100);
      return "OK:差分はありません。";
    }
    log.info(count + "件差分をコピーしました。 コード{}", 7101);
    return "OK:" + count + "件差分をコピーしました";
  }

  /**
   * 他ノードと差異がある時、差分をコピーする（他ノードの半数以上が起動していないと実行不可）
   *
   * @return 正常終了：OK + 詳細
   * @throws RablockSystemException
   */
  @RequestMapping(
      value = "/blockdiff",
      method = {RequestMethod.POST})
  @ApiOperation(
      value = "他ノードと差異がある時、差分をコピーする",
      notes = "他ノードと差異がある時、差分をコピーする（他ノードの半数以上が起動していないと実行不可）")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "OK + 詳細")})
  private String blockDiffCopy() throws RablockSystemException {
    String result;
    synchronized (Global.lock) {

      // 多数派ノードの取得
      MajorNode node = majorNode.search("copyBlock");

      if (!node.warningMessage.equals("")) {
        return node.warningMessage + "ブロック差分コピーできません。";
      }
      // ブロックテーブルの差分コピーをする
      result = diffBlockCopy(node.majorIpAdress, node.majorPort);
    }
    return result;
  }

  /**
   * ブロックテーブルの差分コピーをする
   *
   * @return 差分コピーした件数を返却
   * @throws RablockSystemException
   */
  private String diffBlockCopy(String ipAdress, String port) throws RablockSystemException {

    long count = 0;
    long deleteCount = 0;
    // 他ノードのハッシュ値リストを取得
    String hashRes = sendJson.sendCopy(ipAdress, "getHashList", port, digestUserName, digestPass);
    if (!hashRes.equals("NG")) {
      Document sendIp_data = Document.parse(hashRes);
      List<String> res_HashList = sendIp_data.getList("hash", String.class);

      // ブロックデータの全ハッシュ情報取得
      List<String> myHashList = blockService.getBlockDataHashList();

      List<String> deleteList =
          myHashList.stream()
              .filter(hash -> !res_HashList.contains(hash))
              .collect(Collectors.toList());

      List<String> addList = res_HashList;
      myHashList.stream()
          .filter(hash -> res_HashList.contains(hash))
          .forEach(hash -> addList.remove(hash));

      // oidListに自ノードにのみ保存されているブロックがあるので削除する
      deleteCount =
          deleteList.stream()
              .map(hash -> blockService.removeBlockByKeyValue("hash", hash))
              .count();

      // 自ノードにないブロックを追加する
      Stream<Document> stream =
          addList
              .parallelStream()
              .map(
                  hash ->
                      sendJson.nodeGetBlock(
                          hash,
                          "receiveCheckBlockbyHash",
                          ipAdress,
                          port,
                          digestUserName,
                          digestPass))
              .map(block -> Document.parse(block))
              .map(
                  obj -> {
                    blockService.insertBlockData(obj);
                    return obj;
                  });
      count = stream.count();
      stream
          .filter(obj -> !obj.get("prev_hash").equals("0"))
          .forEach(
              obj ->
                  obj.getList("data", Document.class)
                      .parallelStream()
                      .forEach(data -> poolService.removeTranData(data)));

      if (count == 0 && deleteCount == 0) {
        log.info("差分はありません。 コード{}", 8102);
      } else {
        log.info("ブロックの差分コピーを実施しました。 コード{}", 8103);
      }

    } else {
      log.warn("ブロックの差分コピーに失敗しました。 コード{}", 4100);
    }
    return "OK:追加ブロック" + count + "件、削除ブロック" + deleteCount + "件";
  }

  /**
   * 同期処理 多数派ノードと同じ状態にし、枝分かれを解消する。
   *
   * @return 正常終了：OK + 詳細
   * @throws RablockSystemException
   * @throws JsonProcessingException
   */
  @RequestMapping(
      value = "/blocksync",
      method = {RequestMethod.POST})
  @ApiOperation(value = "同期処理", notes = "同期処理 多数派ノードと同じ状態にし、枝分かれを解消する。")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "OK + 詳細")})
  private String blocksync() throws RablockSystemException, JsonProcessingException {
    String result = "";
    synchronized (Global.lock) {
      // データ改ざんチェック
      String result1 = resolveService.eachBlockCheck();
      // 差分コピー
      String result2 = blockDiffCopy();
      // 枝分かれ解消
      String result3 = resolveService.resolveFork();

      result = result1 + "\n" + result2 + "\n" + result3;
      result = "OK:" + result;
    }
    return result;
  }

  @ExceptionHandler(RablockSystemException.class)
  @ResponseStatus(code = INTERNAL_SERVER_ERROR)
  public Map<String, String> handleSystemException(final RablockSystemException e) {
    log.error(e.getMessage(), e);
    return Collections.singletonMap("message", "invalid");
  }
}

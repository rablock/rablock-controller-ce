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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import javax.annotation.PostConstruct;
import jp.techarts.bc.constitem.ConstItem;
import jp.techarts.bc.constitem.ConstType;
import jp.techarts.bc.jsonrpc.SendJson;
import jp.techarts.bc.prop.GetAppProperties;
import jp.techarts.bc.prop.IpProperties;
import jp.techarts.bc.prop.PortProperties;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * MongoDBのブロックコレクションのサービスクラス<br>
 * Copyright (c) 2018 Rablock Inc.
 *
 * @author TA
 * @version 1.0
 */
@Service
public class ResolveService {
  private final Logger log = LoggerFactory.getLogger(ResolveService.class);

  private final IpProperties ip;
  private final PortProperties port;
  private final BlockService blockService;
  private final PoolService poolService;
  private final MajorNode majorNode;
  private final Common common;

  /** 送信先リスト */
  private final List<Destination> destinationList = new ArrayList<>();

  private final String digestUserName;
  private final String digestPass;

  private final SendJson sendJson;

  @Autowired
  public ResolveService(
      final GetAppProperties app,
      final IpProperties ip,
      final PortProperties port,
      final BlockService blockService,
      final PoolService poolService,
      final MajorNode majorNode,
      final SendJson sendJson,
      final Common common) {
    this.ip = ip;
    this.port = port;
    this.blockService = blockService;
    this.poolService = poolService;
    this.majorNode = majorNode;
    this.sendJson = sendJson;
    this.common = common;

    digestUserName = app.getDigestUserName();
    digestPass = app.getDigestPass();
  }

  /**
   * 起動時の処理<br>
   * 他ノード連携<br>
   */
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
   * ブロックのデータが改ざんされていないかブロック毎にチェック
   *
   * @param sendIp 改ざんデータ発見時に確認するための他ノードのIPアドレス
   * @param sendIpSecond 改ざんデータ発見時に確認するための他ノードのIPアドレス
   * @param sendPort 改ざんデータ発見時に確認するための他ノードのポート番号
   * @param sendPortSecond 改ざんデータ発見時に確認するための他ノードのポート番号
   * @throws RablockSystemException
   */
  public String eachBlockCheck() throws RablockSystemException {
    String returnMessage = "データ改ざんチェック:";
    ObjectMapper mapper = new ObjectMapper();
    boolean eachBlockCheckFlag = false;
    int updateBlockCount = 0;
    Map<String, String> falsifiedBlockOidMap = new HashMap<>();

    // Blockコレクションのブロック全件取得
    List<Document> dataList = blockService.getAllBlock();

    for (final Document compareObj : dataList) {
      // ブロックのハッシュ値を取得
      String hash = compareObj.get(ConstItem.BLOCK_HASH, String.class);

      // ブロックの前ハッシュ値取得
      String prevHash = compareObj.get(ConstItem.BLOCK_PREV_HASH, String.class);
      if (!prevHash.equals("0")) {
        // ジェンシスブロック以外はdataをセットして確認する
        compareObj.remove("_id");
        compareObj.remove("hash");
      } else {
        compareObj.remove("_id");
        compareObj.remove("data");
        compareObj.remove("hash");
      }

      // ハッシュ値計算
      String hashString = compareObj.toJson();
      final String resultHash = common.hashCal(hashString);

      // 改ざんされたブロックを再取得
      Optional<Document> falseObj = blockService.getBlockByKeyValue("hash", hash);
      // TODO really no need to check null ?
      String falseOid = falseObj.get().get("_id").toString();

      // 改ざんされている
      if (!resultHash.equals(hash)) {
        MajorNode major = majorNode.search(hash, "receiveCheckBlockbyHash");

        if (!major.warningMessage.equals("")) {
          eachBlockCheckFlag = true;
          falsifiedBlockOidMap.put(falseOid, major.warningMessage + "その為、改ざん修正できません。");
          continue;
        }

        String majorResult =
            sendJson.nodeCheckBlock(
                hash,
                "receiveCheckBlockbyHash",
                major.majorIpAdress,
                major.majorPort,
                digestUserName,
                digestPass);
        if (majorResult.equals("BLOCK_NOT_FOUND")) {
          log.warn("多数派ノードに同じブロックが存在しないため改ざん修正できません。 コード{}", 4102);
          eachBlockCheckFlag = true;
          falsifiedBlockOidMap.put(falseOid, "多数派ノードに同じhash値のブロックが存在しない為、改ざん修正できません。");
          continue;
        }

        if (!majorResult.equals("NG")) {
          majorResult = "{\"block\" : " + majorResult + "}";
          try {
            JsonNode data = mapper.readTree(majorResult);
            JsonNode node = data.get("block");
            Document resultObj = Document.parse(node.toString());
            if (falseObj.equals(resultObj)) {
              log.warn("改ざんされたブロックが多数派ノードのブロックと同じため改ざん修正できません。 コード{}", 4102);
              eachBlockCheckFlag = true;
              falsifiedBlockOidMap.put(falseOid, "改ざんされたブロックが多数派ノードと同じため改ざん修正できません。");
              continue;
            }
            // 指定されたブロックを更新する
            blockService.updateBlock(compareObj, resultObj);
            updateBlockCount++;
            log.info("改ざんブロック コード{}: " + falseObj, 8104);
            log.info("正しいブロック コード{}: " + resultObj, 8104);
            eachBlockCheckFlag = true;
            falsifiedBlockOidMap.put(falseOid, "修正しました。");
          } catch (JsonProcessingException e) {
            throw new RablockSystemException("システムエラー", e);
          }
        }
      }
    }
    if (eachBlockCheckFlag) {
      returnMessage += "ブロックが" + falsifiedBlockOidMap.size() + "件改ざんされています。\n";
      returnMessage +=
          falsifiedBlockOidMap.entrySet().stream()
              .map(falseEntry -> falseEntry.getKey() + ":" + falseEntry.getValue() + "\n")
              .collect(Collectors.joining());
      returnMessage += "改ざんされたブロックを" + updateBlockCount + "件修正しました。";
    } else {
      returnMessage += "異常なし。";
    }
    return returnMessage;
  }

  /**
   * 枝分かれ解消処理
   *
   * @return String result: 枝分かれ解消処理で削除したブロック
   * @throws RablockSystemException
   * @throws JsonProcessingException
   */
  public String resolveFork() throws RablockSystemException, JsonProcessingException {
    String result;

    // 枝分かれがない場合
    if (!checkFork()) {
      return "OK:枝分かれデータなし";
    }
    // 枝分かれ解消
    result = resolve();
    return result;
  }

  /**
   * 枝分かれが存在する場合は、枝分かれをなくす。 他ノードにも連携する。
   *
   * @throws RablockSystemException
   * @throws JsonProcessingException
   */
  public String resolve() throws RablockSystemException, JsonProcessingException {
    StringBuilder result = new StringBuilder();
    // 枝分かれが存在する間繰り返す
    while (checkFork()) {

      log.info("枝分かれが存在します");

      // 枝分かれブロック取得
      List<Document> forkBlocks = forkData();
      log.info("枝分かれが存在します。フォークブロックの数は" + forkBlocks.size() + "件です。 コード{}", 8105);

      Document firstBlock = forkBlocks.get(0);
      Document secondBlock = forkBlocks.get(1);

      String firstHash = firstBlock.get(ConstItem.BLOCK_HASH, String.class);
      String secondHash = secondBlock.get(ConstItem.BLOCK_HASH, String.class);

      // 枝分かれの長さが短いブロック
      Document shortBlock = null; // TODO remove null initialize later. This code may hide bugs.
      int blockCount = blockService.getBlockCount();
      for (int i = 0; i < blockCount; i++) {
        Optional<Document> firstB =
            blockService.getBlockByKeyValue(ConstItem.BLOCK_PREV_HASH, firstHash);
        Optional<Document> secondB =
            blockService.getBlockByKeyValue(ConstItem.BLOCK_PREV_HASH, secondHash);
        // レコードが存在しなかった場合、短いブロックにする
        if (secondB.isEmpty()) {
          if (firstB.isPresent()) {
            shortBlock = secondBlock;
            break;
          } else {
            shortBlock = secondBlock;
            break;
          }
        }
        if (firstB.isEmpty()) {
          shortBlock = firstBlock;
          break;
        }
        // 両方存在した場合、ハッシュ値を再設定する
        firstHash = firstB.get().get(ConstItem.BLOCK_HASH, String.class);
        secondHash = secondB.get().get(ConstItem.BLOCK_HASH, String.class);
      }
      // 短いブロックのレコードを格納するリスト
      List<Document> shortList = new ArrayList<>();
      shortList.add(shortBlock);
      Boolean shortFlag = true;
      String shortFirstHash = shortBlock.get(ConstItem.BLOCK_HASH, String.class);

      while (shortFlag) {
        Optional<Document> bc =
            blockService.getBlockByKeyValue(ConstItem.BLOCK_PREV_HASH, shortFirstHash);

        // ブロックが存在した場合、リストに追加
        if (bc.isPresent()) {
          Document block = bc.get();
          shortFirstHash = block.get(ConstItem.BLOCK_HASH, String.class);
          shortList.add(block);
        } else {
          shortFlag = false;
        }
      }

      for (final Document block : shortList) {
        // 他ノードのブロックを削除
        List<String> resultArray =
            this.destinationList
                .parallelStream()
                .map(
                    dest -> {
                      final String nodeResult =
                          sendJson.nodeDeleteBlock(
                              block.get(ConstItem.BLOCK_HASH, String.class),
                              "receiveDeleteBlock",
                              dest.ip,
                              dest.port,
                              digestUserName,
                              digestPass);
                      if (nodeResult.equals("NG")) {
                        log.warn(dest.ip + ": データ削除に失敗しました  コード{}", 4300);
                      }
                      return nodeResult;
                    })
                .collect(Collectors.toList());

        // 削除できたノード数
        final long okNode = resultArray.stream().filter(res -> res.equals("OK")).count();

        // プールに戻すデータを取得
        List<Document> dataObjList = block.getList(ConstItem.BLOCK_DATA, Document.class);

        // １ノード以上で削除できたら自ノードも削除
        if (okNode >= 1) {
          blockService.removeBlock(block);

          // 返却値にブロックの内容を入れる
          result.append("枝分かれ削除：" + block.toString() + ", ");

          // 他ノードすべて削除失敗
        } else {
          log.warn("枝分かれデータ削除に失敗しました。 コード{}", 4301);
          return "枝分かれデータ削除に失敗しました";
        }
        // プールに戻す処理
        backToPool(dataObjList);
      }
    }
    if (result.toString().isEmpty()) {
      return "OK:枝分かれなし";
    }
    return "OK:" + result.toString();
  }

  /**
   * 一番最後に枝分かれしているブロックをリストに入れて返却する
   *
   * @return branchBlocks 一番最後に枝分かれしているブロックのリスト
   * @throws RablockSystemException
   */
  public List<Document> forkData() throws RablockSystemException {
    List<Document> forkBlocks = new ArrayList<>();
    int forkCount = 0;
    List<Document> allBlocks = blockService.getAllBlock();

    // 最後に枝分かれしているブロックを取得するために逆順にする
    Collections.reverse(allBlocks);

    for (final Document block : allBlocks) {
      forkCount = 0;
      forkBlocks.clear();
      String hash = block.get(ConstItem.BLOCK_HASH, String.class);
      for (final Document b : allBlocks) {
        String prev_hash = b.get(ConstItem.BLOCK_PREV_HASH, String.class);
        if (hash.equals(prev_hash)) {
          // DBにInsertした順に追加されていく
          forkBlocks.add(b);
          forkCount++;
        }
      }
      // 枝分かれしたブロックが２つ以上存在する場合、返却する。
      if (forkCount > 1) {
        return forkBlocks;
      }
    }
    // デットロジック（枝分かれは存在しているため）
    return forkBlocks;
  }

  /**
   * 枝分かれの存在チェック
   *
   * @return true:存在する false:存在しない
   * @throws RablockSystemException
   */
  public boolean checkFork() throws RablockSystemException {
    List<Document> allBlocks = blockService.getAllBlock();
    for (final Document block : allBlocks) {
      String hash = block.get(ConstItem.BLOCK_HASH, String.class);
      int forkCount = 0;

      for (final Document b : allBlocks) {
        String prev_hash = b.get(ConstItem.BLOCK_PREV_HASH, String.class);
        // ハッシュ値が一致するレコードが存在する場合
        if (hash.equals(prev_hash)) {
          forkCount++;
        }
        // カウントが2以上の場合、枝分かれが存在する
        if (forkCount > 1) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * 枝分かれで削除されたブロックのデータをプールに戻す処理
   *
   * @param dataObjList
   * @throws RablockSystemException
   * @throws JsonProcessingException
   */
  public void backToPool(List<Document> dataObjList)
      throws RablockSystemException, JsonProcessingException {
    StringBuilder s = new StringBuilder();
    s.append(
        "{\"data\":"
            + dataObjList.stream().map(x -> x.toJson()).collect(Collectors.toList()).toString()
            + "}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(s.toString());
    StreamSupport.stream(node.get("data").spliterator(), true)
        .forEach(
            n -> {
              JsonNode id = n.get("_id");
              String oid = id.get("$oid").asText();
              // oidでブロック内のデータを検索
              Optional<Document> blockObj = blockService.getBlockDataByOid("data._id", oid);
              // oidでトランザクションプール内のデータを検索
              Optional<Document> blockObj2 = poolService.getDataByOidinPool(oid);
              if (blockObj.isEmpty() && blockObj2.isEmpty()) {
                log.info(oid + " はブロックにないのでプールに戻します。 コード{}", 8200);
                Document json = Document.parse(n.toString());
                json.put("deliveryF", true);
                // トランザクションデータの登録
                poolService.insertTranData(json);
              }
            });
  }
}

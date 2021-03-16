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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import javax.annotation.PostConstruct;
import jp.techarts.bc.jsonrpc.SendJson;
import jp.techarts.bc.prop.GetAppProperties;
import jp.techarts.bc.prop.IpProperties;
import jp.techarts.bc.prop.PortProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 多数派ノード検索クラス<br>
 * Copyright (c) 2018 Rablock Inc.
 *
 * @author TA
 * @version 1.0
 */
@Service
class MajorNode {
  private final Logger log = LoggerFactory.getLogger(ResolveService.class);

  private final IpProperties ip;
  private final PortProperties port;

  /** 自ノードのポート番号 */
  private final String myPort;

  // 多数派ノードのIPアドレス
  String majorIpAdress = "";
  // 多数派ノードのポート番号
  String majorPort = "";
  // 警告メッセージ
  String warningMessage = "";

  private final String digestUserName;
  private final String digestPass;

  final List<Destination> destinationList = new ArrayList<>();

  private final SendJson sendJson;

  @Autowired
  public MajorNode(
      final GetAppProperties app,
      final SendJson sendJson,
      final IpProperties ip,
      final PortProperties port) {
    this.sendJson = sendJson;
    this.ip = ip;
    this.port = port;
    myPort = app.getPort();
    digestUserName = app.getDigestUserName();
    digestPass = app.getDigestPass();
  }

  @PostConstruct
  public void initAfterStartup() {
    final List<String> sendIpArray = ip.getIp();
    final List<String> sendPortArray = port.getPort();
    destinationList.addAll(
        IntStream.range(0, sendIpArray.size())
            .mapToObj(i -> new Destination(sendIpArray.get(i), sendPortArray.get(i)))
            .collect(Collectors.toList()));
    destinationList.add(new Destination("127.0.0.1", myPort));
  }

  /**
   * 全ノードから多数派ノードを検索
   *
   * @param method
   * @return
   * @throws RablockSystemException
   */
  public MajorNode search(String method) throws RablockSystemException {

    // TODO should be refactored.
    MajorNode major = this;

    List<String> resultArray = new ArrayList<>();

    // ノードデータ取得確認
    try {
      resultArray.addAll(
          destinationList
              .parallelStream()
              .map(
                  dest -> {
                    try {
                      final String sendIp_res =
                          sendJson.sendCopy(dest.ip, method, dest.port, digestUserName, digestPass);
                      if (sendIp_res.equals("NG")) {
                        log.warn(dest.ip + "は通信に失敗したのでデータが取得できません。 コード{}", 1006);
                      }
                      return sendIp_res;
                    } catch (RablockSystemException e) {
                      throw new RuntimeException(e);
                    }
                  })
              .collect(Collectors.toList()));
    } catch (RuntimeException e) {
      if (e.getCause() instanceof RablockSystemException) {
        throw (RablockSystemException) e.getCause();
      } else {
        throw e;
      }
    }

    // 通信できたノード数
    final long conNode = resultArray.parallelStream().filter(res -> !res.equals("NG")).count();

    if (conNode <= destinationList.size() / 2) {
      log.warn("半数以上のノードと通信に失敗しました。 コード{}", 1007);
      major.warningMessage = "半数以上のノードと通信に失敗しました。";
      return major;
    }

    LongStream nodeSameCountStream =
        IntStream.range(0, destinationList.size())
            .mapToLong(
                i -> {
                  // ノード同士の一致数
                  return IntStream.range(i + 1, destinationList.size())
                      .map(
                          j -> {
                            if (resultArray.get(i).equals(resultArray.get(j))) {
                              log.info(
                                  destinationList.get(i).ip
                                      + "と"
                                      + destinationList.get(j).ip
                                      + "は同じデータを保持しています。 コード{}",
                                  8000);
                              return 1;
                            }
                            return 0;
                          })
                      .filter(result -> result == 1)
                      .count();
                });
    // 多数派ノードの数
    long majorCount = nodeSameCountStream.max().orElseThrow(AssertionError::new);
    int i = nodeSameCountStream.boxed().collect(Collectors.toList()).indexOf(majorCount);

    if (nodeSameCountStream.filter(nodeSameCount -> nodeSameCount == majorCount).count() > 1) {
      log.warn("多数派のノードが存在しません。 コード{}", 1008);
      major.warningMessage = "多数派のノードが存在しません。";
      return major;
    }

    final Destination dest = destinationList.get(i);
    major.majorIpAdress = dest.ip;
    major.majorPort = dest.port;

    log.debug("通信できたノード数：：" + conNode);
    log.debug("多数派アドレス：：" + dest.ip);
    log.debug("多数派ポート番号：：" + dest.port);

    return major;
  }

  /**
   * 全ノードから多数派ノードを検索
   *
   * @param hash
   * @param method
   * @return
   * @throws RablockSystemException
   */
  public MajorNode search(String hash, String method) throws RablockSystemException {
    // TODO should be refactored.
    MajorNode major = this;

    // ノードデータ取得確認
    List<String> resultArray =
        destinationList
            .parallelStream()
            .map(
                dest -> {
                  final String sendIp_res =
                      sendJson.nodeCheckBlock(
                          hash, method, dest.ip, dest.port, digestUserName, digestPass);
                  if (sendIp_res.equals("NG")) {
                    log.warn(dest.ip + "は通信に失敗したのでデータが取得できません。 コード{}", 1009);
                  }
                  return sendIp_res;
                })
            .collect(Collectors.toList());

    // 通信できたノード数
    final long conNode = resultArray.stream().filter(res -> !res.equals("NG")).count();

    if (conNode <= destinationList.size() / 2) {
      log.warn("半数以上のノードと通信に失敗しました。 コード{}", 1010);
      major.warningMessage = "半数以上のノードと通信に失敗しました。";
      return major;
    }

    // 多数派ノードのIPアドレス
    String majorIpAdress = "";
    // 多数派ノードのポート番号
    String majorPort = "";
    // 多数派ノードの数
    long majorCount = 0;
    // 同数フラグ
    boolean sameNumberFlag = false;

    for (final int i : IntStream.range(0, destinationList.size()).toArray()) {
      IntStream nodeSameStream =
          IntStream.range(i + 1, destinationList.size())
              .filter(j -> resultArray.get(i).equals(resultArray.get(j)));
      final long nodeSameCount = nodeSameStream.count();
      nodeSameStream.forEach(
          j ->
              log.info(
                  destinationList.get(i).ip
                      + "と"
                      + destinationList.get(j).ip
                      + "は同じデータを保持しています。 コード{}",
                  8001));

      if (nodeSameCount > majorCount) {
        majorCount = nodeSameCount;
        final Destination dest = destinationList.get(i);
        majorIpAdress = dest.ip;
        majorPort = dest.port;
        sameNumberFlag = false;
      } else if (nodeSameCount == majorCount) {
        sameNumberFlag = true;
      }
    }

    if (sameNumberFlag) {
      log.warn("多数派のノードが存在しません。 コード{}", 1011);
      major.warningMessage = "多数派のノードが存在しません。";
      return major;
    }

    major.majorIpAdress = majorIpAdress;
    major.majorPort = majorPort;
    return major;
  }
}

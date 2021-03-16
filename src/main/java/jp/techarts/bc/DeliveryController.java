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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.PostConstruct;
import jp.techarts.bc.constitem.ConstType;
import jp.techarts.bc.grobal.Global;
import jp.techarts.bc.jsonrpc.SendJson;
import jp.techarts.bc.prop.GetAppProperties;
import jp.techarts.bc.prop.IpProperties;
import jp.techarts.bc.prop.PortProperties;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * デリバリーコントローラークラス<br>
 * Copyright (c) 2018 Rablock Inc.
 *
 * @author TA
 * @version 1.0
 */
@RestController
@RequestMapping("/delivery")
@Api(tags = "Delivery", description = "Delivery controller")
public class DeliveryController {
  private final Logger log = LoggerFactory.getLogger(DeliveryController.class);

  private final IpProperties ip;
  private final PortProperties port;
  private final PoolService poolService;

  private final String digestUserName;

  private final String digestPass;

  private final List<Destination> destinationList = new ArrayList<>();

  private final SendJson sendJson;

  @Autowired
  public DeliveryController(
      final GetAppProperties app,
      final IpProperties ip,
      final PortProperties port,
      final PoolService poolService,
      final SendJson sendJson) {
    this.ip = ip;
    this.port = port;
    this.poolService = poolService;
    this.sendJson = sendJson;

    this.digestUserName = app.getDigestUserName();
    this.digestPass = app.getDigestPass();
  }

  @PostConstruct
  public void init() {
    /** 送信先IPアドレス */
    final List<String> sendIpArray = ip.getIp();
    /** 送信先ポート番号 */
    final List<String> sendPortArray = port.getPort();
    destinationList.addAll(
        IntStream.range(0, sendIpArray.size())
            .mapToObj(i -> new Destination(sendIpArray.get(i), sendPortArray.get(i)))
            .collect(Collectors.toList()));
  }

  /**
   * 伝搬されていないトランザクションプールのデータを他ノードに伝搬する
   *
   * @return 正常終了：OK
   * @throws RablockSystemException
   */
  @RequestMapping(
      value = "/deliverypool",
      method = {RequestMethod.POST})
  @ApiOperation(value = "Transfer transaction", notes = "伝搬されていないトランザクションプールのデータを他ノードに伝搬する")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "OK or NG")})
  public String deliveryPool() throws RablockSystemException {
    synchronized (Global.lock) {
      // PoolコレクションのDeliveryFがFALSEのデータ取得（伝搬時）
      final List<Document> deliveryList = poolService.getNotDeliveredData();

      // 0件の時はOKを返却
      if (deliveryList.size() == 0) {
        return "OK";
      }

      long conNode =
          destinationList
              .parallelStream()
              .map(
                  dest -> {
                    String result =
                        sendJson.nodeSendPool(
                            deliveryList, dest.ip, dest.port, digestUserName, digestPass);
                    log.info(dest.ip + "のトランザクションデータ送信結果：：" + result + " コード{}", 5000);
                    return result;
                  })
              .map(res -> res.equals("OK"))
              .count();

      // １ノード以上に送信できたらOK
      if (conNode >= 1) {
        deliveryList
            .parallelStream()
            .forEach(
                deliveryObj -> {
                  // deliveryFをtrueに更新する
                  deliveryObj.put("deliveryF", true);
                  // PoolコレクションのDeliveryFを更新
                  poolService.updateDeliveryStatus(deliveryObj, true);
                });
      } else {
        log.warn((destinationList.size() - conNode) + "つのノードに送信できませんでした。 コード{}", 1000);
        return "NG";
      }
    }
    return "OK";
  }

  @ExceptionHandler(RablockSystemException.class)
  @ResponseStatus(code = INTERNAL_SERVER_ERROR)
  public Map<String, String> handleSystemException(final RablockSystemException e) {
    log.error(e.getMessage(), e);
    return Collections.singletonMap("message", "invalid");
  }
}

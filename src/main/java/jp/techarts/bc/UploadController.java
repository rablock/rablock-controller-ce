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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jp.techarts.bc.constitem.ConstItem;
import jp.techarts.bc.constitem.ConstType;
import jp.techarts.bc.prop.GetAppProperties;
import jp.techarts.bc.rsa.RSAPublicEncode;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
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
@RequestMapping("/post")
@Api(value = "Upload")
public class UploadController {
  private final Logger log = LoggerFactory.getLogger(UploadController.class);

  private final PoolService poolService;
  private final BlockService blockService;
  private final RSAPublicEncode publicEn;
  private final Common common;

  /** 暗号化ON/OFF */
  private final String cryptoStatus;

  @Autowired
  public UploadController(
      final GetAppProperties config,
      final PoolService poolService,
      final BlockService blockService,
      final RSAPublicEncode publicEn,
      final Common common) {
    this.poolService = poolService;
    this.blockService = blockService;
    this.publicEn = publicEn;
    this.common = common;
    this.cryptoStatus = config.getCryptoStatus();
  }

  // テスト用 curl -X POST http://localhost:9000/post/test
  @RequestMapping(
      value = "/test",
      method = {RequestMethod.POST})
  public String test() {
    log.debug("テスト実行");
    return "OK";
  }

  /**
   * 受け取ったJSONの全項目を登録する
   *
   * @param Json形式の文字列
   * @return 正常終了：OK
   * @throws RablockSystemException
   * @throws JsonProcessingException
   */
  @RequestMapping(value = "/json", method = RequestMethod.POST)
  @ApiOperation(value = "受け取ったJSONの全項目を登録する")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "OK or NG")})
  public String json(@ApiParam(value = "JSON形式の文字列") @RequestBody() String json)
      throws RablockSystemException, JsonProcessingException {
    // 項目名をリストに格納
    List<String> itemList = common.getItemList(json);
    JSONObject jsonObj;

    // json文字列をjsonObjectに変換
    jsonObj = new JSONObject(json);

    // 重複で修正・削除していないかチェック
    if (jsonObj.get(ConstItem.DATA_TYPE).equals(ConstType.MODIFY)
        || jsonObj.get(ConstItem.DATA_TYPE).equals(ConstType.DELETE)) {
      // delete&modifyList
      List<String> modifyDeleteList = blockService.getModifyDeleteListinBlock();
      List<String> poolModifyDeleteList = poolService.getModifyDeleteListinPool();
      modifyDeleteList.addAll(poolModifyDeleteList);

      String oid = jsonObj.getString(ConstItem.DATA_ORIGINAL_ID);
      if (modifyDeleteList.contains(oid)) {
        return "NG";
      }
    }

    // ON type, original_id, settime, deliveryF以外は暗号化される
    if (cryptoStatus.equals("ON")) {
      itemList
          .parallelStream()
          .filter(
              key -> (key.equals(ConstItem.DATA_TYPE) || key.equals(ConstItem.DATA_ORIGINAL_ID)))
          .forEach(
              key -> {
                boolean cryptoFlag = false;
                boolean putFlag = false;
                // タイプ、オリジナルIDの値はそのまま
                String data = jsonObj.getString(key);
                String abnormalData = jsonObj.get(key).toString();
                if (common.nestCheck(abnormalData)) {
                  data = publicEn.nestCrypt(abnormalData);
                  cryptoFlag = true;
                } else if (common.arrayCheck(abnormalData)) {
                  if (common.arrayKeyCheck(abnormalData)) {
                    data = publicEn.arrayCrypt(abnormalData);
                  } else {
                    data = publicEn.arrayNoKeyCrypt(abnormalData);
                  }
                  JSONArray arrayData = new JSONArray(data);
                  jsonObj.put(key, arrayData);
                  cryptoFlag = true;
                  putFlag = true;
                } else {
                  data = String.valueOf(jsonObj.get(key));
                }
                if (!cryptoFlag) {
                  data = publicEn.crypto(data);
                }
                if (!putFlag) {
                  jsonObj.put(key, data);
                }
              });
    }

    poolService.setPool(jsonObj, common.getCurrentTime(), itemList);
    return "OK";
  }

  @ExceptionHandler(RablockSystemException.class)
  @ResponseStatus(code = INTERNAL_SERVER_ERROR)
  public Map<String, String> handleSystemException(final RablockSystemException e) {
    log.error(e.getMessage(), e);
    return Collections.singletonMap("message", "invalid");
  }
}

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

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import jp.techarts.bc.constitem.ConstJsonType;
import jp.techarts.bc.prop.GetAppProperties;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
@RequestMapping("/get")
public class GetController {
  private final Logger log = LoggerFactory.getLogger(GetController.class);

  private final BlockService blockService;
  private final PoolService poolService;
  private final Common common;

  /** 暗号化ON/OFF */
  private final String cryptoStatus;

  @Autowired
  public GetController(
      final GetAppProperties config,
      final BlockService blockService,
      final PoolService poolService,
      final Common common) {
    this.blockService = blockService;
    this.poolService = poolService;
    this.common = common;
    cryptoStatus = config.getCryptoStatus();
  }

  /**
   * 項目とその値のJSONを送って検索
   *
   * @param jsonvalue
   * @return
   * @throws RablockSystemException
   * @throws JsonParseException
   */
  @RequestMapping("/json")
  @ApiOperation(value = "Search by JSON", notes = "項目とその値のJSONを送って検索")
  public String getJson(@RequestBody() final String jsonvalue)
      throws RablockSystemException, JsonProcessingException {
    final ObjectMapper mapper = new ObjectMapper();

    final List<String> itemList = common.getItemList(jsonvalue);
    if (itemList.size() != 1) {
      return "データ形式が不正です";
    }
    final String key = itemList.get(0);

    // JsonNodeに変換
    JsonNode jsonnode = mapper.readTree(jsonvalue);

    // 送信されたデータ型が合致する検索方法でデータ取得
    List<Document> blockDataList;
    List<Document> poolList;

    if (cryptoStatus.equals("ON")) {

      String value = jsonnode.get(key).asText();
      // 値を正規表現にする
      value = "^" + common.hashCal(value);
      blockDataList = blockService.getCryptoByKeyValue(key, value);
      poolList = poolService.getCryptoByKeyValue(key, value);
    } else {
      final Class<? extends JsonNode> dataType = jsonnode.get(key).getClass();
      if (dataType.equals(ConstJsonType.TEXTNODE)) {
        final String value = jsonnode.get(key).asText();
        blockDataList = blockService.getByKeyValue(key, value);
      } else if (dataType.equals(ConstJsonType.INTNODE)) {
        final int value = jsonnode.get(key).asInt();
        blockDataList = blockService.getByKeyValue(key, value);
      } else if (dataType.equals(ConstJsonType.DOUBLENODE)) {
        final Double value = jsonnode.get(key).asDouble();
        blockDataList = blockService.getByKeyValue(key, value);
      } else if (dataType.equals(ConstJsonType.BOOLEANNODE)) {
        final boolean value = jsonnode.get(key).asBoolean();
        blockDataList = blockService.getByKeyValue(key, value);
      } else if (dataType.equals(ConstJsonType.NULLNODE)) {
        log.info("nullでは検索できません。値は、" + jsonvalue + " コード{}", 6000);
        return "nullでは検索できません";
      } else if (dataType.equals(ConstJsonType.ARRAYNODE)) {
        log.info("配列では検索できません。値は、" + jsonvalue + " コード{}", 6001);
        return "配列では検索できません";
      } else if (dataType.equals(ConstJsonType.OBJECTNODE)) {
        log.info("オブジェクトでは検索できません。値は、" + jsonvalue + " コード{}", 6002);
        return "オブジェクトでは検索できません";
      } else {
        log.info(dataType + "は検索できない形式です。値は、" + jsonvalue + " コード{}", 6003);
        return dataType + "は検索できない形式です。";
      }

      poolList = poolService.getByKeyValue(key, jsonnode);
    }

    final List<String> list = new ArrayList<>();
    list.addAll(poolList.stream().map(x -> x.toJson()).collect(Collectors.toList()));
    list.addAll(blockDataList.stream().map(x -> x.toJson()).collect(Collectors.toList()));
    return list.toString();
  }

  /**
   * 指定されたオブジェクトIDから履歴を返却
   *
   * @param oid
   * @return 全履歴のリスト
   * @throws RablockSystemException
   */
  @RequestMapping("/searchoid/{oid}")
  @ApiOperation(value = "指定されたオブジェクトIDから履歴を返却")
  @ApiParam(required = true, value = "Object ID")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "全履歴のリスト")})
  public String getOid(@PathVariable final String oid) throws RablockSystemException {
    // blockコレクションから検索
    Optional<Document> obj =
        blockService
            .findByOidinBlock(oid)
            .or(
                () ->
                    // blockコレクションにない場合poolコレクション検索
                    poolService.getDataByOidinPool(oid));
    if (obj.isEmpty()) {
      return " null ";
    } else {
      return obj.get().toJson();
    }
  }

  /**
   * 全データを返却
   *
   * @return 全履歴のリスト
   * @throws RablockSystemException
   */
  @RequestMapping("/alltxdata")
  @ApiOperation(value = "全データを返却")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "全履歴のリスト")})
  public String getAll() throws RablockSystemException {
    final List<String> list = new ArrayList<>();
    list.addAll(
        poolService
            .getAllPool()
            .parallelStream()
            .map(x -> x.toJson())
            .collect(Collectors.toList()));
    list.addAll(
        blockService
            .getAllBlockData()
            .parallelStream()
            .map(x -> x.toJson())
            .collect(Collectors.toList()));
    return list.toString();
  }

  /**
   * 全ブロックを返却
   *
   * @return Blockコレクションのデータ全件
   * @throws RablockSystemException
   */
  @RequestMapping("/block")
  @ApiOperation(value = "全ブロックを返却")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Blockコレクションのデータ全件")})
  public String getBlock() throws RablockSystemException {
    return blockService
        .getAllBlock()
        .parallelStream()
        .map(x -> x.toJson())
        .collect(Collectors.toList())
        .toString();
  }

  /**
   * ブロック化されていないトランザクションデータを取得
   *
   * @return Poolコレクションのデータ全件
   * @throws RablockSystemException
   */
  @RequestMapping("/pool")
  @ApiOperation(value = "ブロック化されていないトランザクションデータを取得")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Poolコレクションのデータ全件")})
  public String getPool() throws RablockSystemException {
    // Poolコレクションから取得
    return poolService
        .getAllPool()
        .parallelStream()
        .map(x -> x.toJson())
        .collect(Collectors.toList())
        .toString();
  }

  /**
   * 伝搬済みのトランザクションデータを取得
   *
   * @return PoolコレクションのデータでDeliveryFがTRUEのデータ全件
   * @throws RablockSystemException
   */
  @RequestMapping("/deliveredpool")
  @ApiOperation(value = "伝搬済みのトランザクションデータを取得")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "PoolコレクションのデータでDeliveryFがTRUEのデータ全件")})
  public String getDeliveredPool() throws RablockSystemException {
    // Poolコレクションから取得
    return poolService
        .getDeliveredData()
        .parallelStream()
        .map(x -> x.toJson())
        .collect(Collectors.toList())
        .toString();
  }

  /**
   * 最後のブロックのハッシュ値を取得
   *
   * @return Poolコレクションのデータ全件
   * @throws RablockSystemException
   */
  @RequestMapping("/lastblock")
  @ApiOperation(value = "最後のブロックのハッシュ値を取得")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Poolコレクションのデータ全件")})
  public String getLastBlock() throws RablockSystemException {
    Optional<Document> lastBlock = blockService.getLastBlock();
    return lastBlock.isPresent() ? lastBlock.get().toJson() : "null";
  }

  /**
   * 指定されたデータの履歴を返却
   *
   * @param _id
   * @return 履歴のリスト
   * @throws RablockSystemException
   */
  @RequestMapping("/history/{_id}")
  @ApiOperation(value = "指定されたデータの履歴を返却")
  @ApiParam(required = true, value = "ObjectID")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "履歴のリスト")})
  public String getHistory(@PathVariable String _id) throws RablockSystemException {

    final List<Document> historyList = new ArrayList<>();
    boolean newFlag = false;
    while (!newFlag) {
      // blockコレクションから検索
      Document obj =
          blockService
              .findByOidinBlock(_id)
              .orElse(
                  // blockコレクションにない場合poolコレクション検索
                  poolService.getDataByOidinPool(_id).orElse(null));
      // TODO really no need to check if obj == null ?.

      historyList.add(obj);

      if (obj.get("type").equals("new")) {
        newFlag = true;
      } else {
        _id = obj.get("original_id", String.class);
      }
    }

    return historyList.stream().map(x -> x.toJson()).collect(Collectors.toList()).toString();
  }

  /**
   * 全ブロックのデータ件数合計
   *
   * @return 履歴のリスト
   * @throws RablockSystemException
   */
  @RequestMapping("/totalnumber")
  @ApiOperation(value = "全ブロックのデータ件数合計")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "履歴のリスト")})
  public String getTotalNumber() throws RablockSystemException {
    final List<Document> blockList = blockService.getAllBlock();
    return Integer.toString(
        blockList
            .parallelStream()
            .map(block -> block.get("size", Integer.class))
            .mapToInt(Integer::intValue)
            .sum());
  }

  /**
   * 値を絞り込んで、指定した項目の値の合計を取得する
   *
   * @param key
   * @param value
   * @param item
   * @return
   */
  //	@RequestMapping("/calculate/{key}/{value}/{item}")
  //    public Integer calculate(@PathVariable String key, @PathVariable String value, @PathVariable
  // String item) {
  //		if (cryptoStatus.equals("ON")) {
  //			return null;
  //		}
  //
  //		List<DBObject> blockDataList = blockService.getDataByKeyValue(key, value);
  //		List<DBObject> poolList = poolService.getDataKeyValue(key, value);
  //		blockDataList.addAll(poolList);
  //
  //		int calculatedValue = 0;
  //		for (DBObject obj : blockDataList) {
  //			calculatedValue += (int) obj.get(item);
  //		}
  //
  //		return calculatedValue;
  //    }

  @ExceptionHandler(JsonParseException.class)
  @ResponseStatus(code = BAD_REQUEST)
  public Map<String, String> handleJsonParseException(final JsonParseException e) {
    return Collections.singletonMap("message", "NG");
  }

  @ExceptionHandler(RablockSystemException.class)
  @ResponseStatus(code = INTERNAL_SERVER_ERROR)
  public Map<String, String> handleException(final RablockSystemException e) {
    log.error(e.getMessage(), e);
    return Collections.singletonMap("message", "invalid");
  }
}

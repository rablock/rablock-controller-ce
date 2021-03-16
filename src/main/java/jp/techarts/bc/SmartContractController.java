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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import jp.techarts.bc.constitem.ConstItem;
import jp.techarts.bc.constitem.ConstType;
import jp.techarts.bc.prop.GetAppProperties;
import org.bson.Document;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * スマートコントラクト関連のコントローラクラス<br>
 * Copyright (c) 2018 Rablock Inc.
 *
 * @author TA
 * @version 1.0
 */
@RestController
@RequestMapping("/contract")
@Api(value = "スマートコントラクト関連のコントローラクラス")
public class SmartContractController {

  /** アプリケーションプロパティ */
  private final GetAppProperties config;

  /** プールコレクションのサービスクラス */
  private final PoolService poolService;

  /** スマートコントラクト関連のサービスクラス */
  private final SmartContractService contractService;

  /** 共通処理のサービスクラス */
  private final Common common;

  /** ロガー */
  private final Logger log = LoggerFactory.getLogger(SmartContractController.class);

  @Autowired
  public SmartContractController(
      final GetAppProperties config,
      final PoolService poolService,
      final SmartContractService contractService,
      final Common common) {
    this.config = config;
    this.poolService = poolService;
    this.contractService = contractService;
    this.common = common;
  }

  /**
   * 契約定義をブロックチェーンに登録する。
   *
   * @param json JSON形式の契約定義情報
   * @return 正常終了：OK / 異常終了：NG
   */
  @RequestMapping(value = "/define", method = RequestMethod.POST)
  @ApiOperation(value = "契約定義をブロックチェーンに登録する。")
  @ApiParam(value = "JSON形式の契約定義情報")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "正常終了：OK / 異常終了：NG")})
  public String define(@RequestBody String json) {

    try {
      // 登録対象データの項目チェック
      Optional<JsonNode> nodeOrEmpty = contractService.checkContractDefine(json);
      if (nodeOrEmpty.isEmpty()) {
        String errMsg = "契約定義のデータ形式が不正です。";
        log.error(errMsg);
        return contractService.createResponse("NG", errMsg, null);
      }

      JsonNode node = nodeOrEmpty.get();
      if (ConstType.NEW.equals(node.get(ConstItem.DATA_TYPE).asText())) {
        // 新規登録時のデータチェック
        if (!contractService.checkNewDefine(node)) {
          String errMsg = "不正なデータのため契約定義情報は登録できません。";
          log.error(errMsg);
          return contractService.createResponse("NG", errMsg, null);
        }
      } else if (ConstType.MODIFY.equals(node.get(ConstItem.DATA_TYPE).asText())
          || ConstType.DELETE.equals(node.get(ConstItem.DATA_TYPE).asText())) {
        // 変更・削除時のデータチェック
        if (!contractService.checkModifyDeleteDefine(node)) {
          String errMsg = "不正なデータのため契約定義情報の変更・削除はできません。";
          log.error(errMsg);
          return contractService.createResponse("NG", errMsg, null);
        }
      } else {
        // データ種別が不正
        String errMsg = "不正なデータ種別です。";
        log.error("データ種別：" + node.get(ConstItem.DATA_TYPE).asText() + " は不正です。");
        return contractService.createResponse("NG", errMsg, null);
      }

      if ("ON".equals(config.getCryptoStatus())) {
        // TODO:データの暗号化処理
      }

      // データ登録
      JSONObject jsonObj = new JSONObject(node.toString());
      jsonObj.put(ConstItem.CONTRACT_TYPE, ConstType.CONTRACT_DEFINE);
      Document result = poolService.setPool(jsonObj, common.getCurrentTime());
      if (result == null) {
        String errMsg = "契約定義の登録に失敗しました。";
        log.error(errMsg);
        log.error(jsonObj.toString());
        return contractService.createResponse("NG", errMsg, null);
      }
    } catch (Exception e) {
      String errMsg = "予期せぬエラーが発生しました。";
      log.error(errMsg);
      return contractService.createResponse("NG", errMsg, null);
    }

    return contractService.createResponse("OK", null, null);
  }

  /**
   * 登録されている契約定義を全件取得する。
   *
   * @return レスポンス情報
   * @throws RablockSystemException
   */
  @RequestMapping(value = "/define/list", method = RequestMethod.GET)
  @ApiOperation(value = "登録されている契約定義を全件取得する。")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "レスポンス情報")})
  public String defineList() throws RablockSystemException {

    // 契約定義リストの取得
    try {
      JsonNode defineList = contractService.getDefineList();

      // レスポンス返却
      return contractService.createResponse("OK", null, defineList);
    } catch (JsonProcessingException e) {
      return contractService.createResponse("NG", "予期せぬエラーが発生しました。", null);
    }
  }

  /**
   * ユーザーとの契約内容をブロックチェーンに登録する。
   *
   * @param json JSON形式の契約内容情報
   * @return 正常終了：OK / 異常終了：NG
   */
  @RequestMapping(value = "/agree", method = RequestMethod.POST)
  @ApiOperation(value = "ユーザーとの契約内容をブロックチェーンに登録する。")
  @ApiParam(value = "JSON形式の契約内容情報")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "正常終了：OK / 異常終了：NG")})
  public String agree(@RequestBody String json) {

    try {
      // 登録対象データの項目チェック
      JsonNode node = contractService.checkContractAgree(json);
      if (node == null) {
        String errMsg = "不正なデータが存在するためユーザー契約情報は登録できません。";
        log.error(errMsg);
        return contractService.createResponse("NG", errMsg, null);
      }

      if ("ON".equals(config.getCryptoStatus())) {
        // TODO:データの暗号化処理
      }

      // データ登録
      JSONObject jsonObj = new JSONObject(node.toString());
      jsonObj.put(ConstItem.CONTRACT_TYPE, ConstType.CONTRACT_AGREE);
      Document result = poolService.setPool(jsonObj, common.getCurrentTime());
      if (result == null) {
        String errMsg = "ユーザー契約情報の登録に失敗しました。";
        log.error(errMsg);
        log.error(jsonObj.toString());
        contractService.createResponse("NG", errMsg, null);
      }
    } catch (Exception e) {
      String errMsg = "予期せぬエラーが発生しました。";
      log.error(errMsg, e);
      return contractService.createResponse("NG", errMsg, null);
    }

    return contractService.createResponse("OK", null, null);
  }

  /**
   * ユーザー契約情報を下記項目で検索し、一覧を返却する。 user : ユーザーID number : 契約番号
   *
   * @param user ユーザーID
   * @param number 契約番号
   * @return ユーザー契約情報一覧
   * @throws RablockSystemException
   */
  @RequestMapping(value = "/agree/list", method = RequestMethod.GET)
  @ApiOperation(value = "ユーザー契約情報を下記項目で検索し、一覧を返却する。")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "ユーザー契約情報一覧")})
  public String agreeList(
      @ApiParam(value = "ユーザーID") @RequestParam(name = "user", required = false) String user,
      @ApiParam(value = "契約番号") @RequestParam(name = "number", required = false) String number)
      throws RablockSystemException {

    // ユーザー契約一覧の取得
    JsonNode agreeList = contractService.getAgreeList(user, number);
    if (agreeList == null) {
      // エラー処理
      return contractService.createResponse("NG", "予期せぬエラーが発生しました。", null);
    }

    // レスポンス返却
    return contractService.createResponse("OK", null, agreeList);
  }

  /**
   * 指定されたoidに該当するユーザー契約情報を返却する。
   *
   * @param oid オブジェクトID
   * @return ユーザー契約情報
   * @throws RablockSystemException
   */
  @RequestMapping(value = "/agree/{oid}", method = RequestMethod.GET)
  @ApiOperation(value = "指定されたoidに該当するユーザー契約情報を返却する。")
  @ApiParam(value = "オブジェクトID")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "ユーザー契約情報")})
  public String agreeDetail(@PathVariable String oid) throws RablockSystemException {

    // ユーザー契約情報の取得
    JsonNode agreeDetail = contractService.getAgreeForOid(oid);
    if (agreeDetail == null) {
      // エラー処理
      return contractService.createResponse("NG", "予期せぬエラーが発生しました。", null);
    }

    // レスポンス返却
    return contractService.createResponse("OK", null, agreeDetail);
  }

  /**
   * ユーザーとの契約内容を実行し、結果をブロックチェーンに登録する。
   *
   * @param json JSON形式の実行パラメータ
   * @return 正常終了：OK / 異常終了：NG
   */
  @RequestMapping(value = "/execute", method = RequestMethod.POST)
  @ApiOperation(value = "ユーザーとの契約内容を実行し、結果をブロックチェーンに登録する。")
  @ApiParam(value = "JSON形式の実行パラメータ")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "正常終了：OK / 異常終了：NG")})
  public String execute(@RequestBody String json) {
    try {
      // 実行パラメータのチェック
      JsonNode param = contractService.checkExecParam(json);
      if (param == null) {
        String errMsg = "実行パラメータのデータ形式が不正です。";
        log.error(errMsg);
        return contractService.createResponse("NG", errMsg, null);
      }

      // ユーザー契約情報取得、および契約内容・契約期限チェック
      JsonNode agree = contractService.checkExecAgree(param.get("oid").asText());
      if (agree == null) {
        String errMsg = "ユーザー契約情報が不正です。";
        log.error(errMsg);
        return contractService.createResponse("NG", errMsg, null);
      }

      // 契約定義情報取得
      Optional<JsonNode> defineOrEmpty =
          contractService.checkExecDefine(
              agree.get(ConstItem.CONTRACT_NUMBER).asText(),
              param.get(ConstItem.CONTRACT_FUNC_ID).asText());
      if (defineOrEmpty.isEmpty()) {
        String errMsg = "契約定義情報が不正です。";
        log.error(errMsg);
        return contractService.createResponse("NG", errMsg, null);
      }

      // 実行対象オペレーションの実行
      return contractService.execContract(param, defineOrEmpty.get(), agree);

    } catch (Exception e) {
      String errMsg = "予期せぬエラーが発生しました。";
      log.error(errMsg);
      return contractService.createResponse("NG", errMsg, null);
    }
  }

  /**
   * 実行結果情報を下記項目で検索し、一覧を返却する。
   *
   * @param user ユーザーID
   * @param number 契約番号
   * @return 実行結果情報一覧
   * @throws RablockSystemException
   */
  @RequestMapping(value = "/result/list", method = RequestMethod.GET)
  @ApiOperation(value = "実行結果情報を下記項目で検索し、一覧を返却する。")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "実行結果情報一覧")})
  public String resultList(
      @ApiParam(value = "ユーザーID") @RequestParam(name = "user", required = false) String user,
      @ApiParam(value = "契約番号") @RequestParam(name = "number", required = false) String number)
      throws RablockSystemException {

    // 実行結果一覧の取得
    JsonNode resultList = contractService.getResultList(user, number);
    if (resultList == null) {
      // エラー処理
      return contractService.createResponse("NG", "予期せぬエラーが発生しました。", null);
    }

    // レスポンス返却
    return contractService.createResponse("OK", null, resultList);
  }

  /**
   * 指定されたoidに該当する実行結果情報を返却する。
   *
   * @param oid オブジェクトID
   * @return 実行結果情報
   * @throws RablockSystemException
   */
  @RequestMapping(value = "/result/{oid}", method = RequestMethod.GET)
  @ApiOperation(value = "指定されたoidに該当する実行結果情報を返却する。")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "実行結果情報")})
  public String resultDetail(@ApiParam(value = "オブジェクトID") @PathVariable String oid)
      throws RablockSystemException {

    // 実行結果情報の取得
    JsonNode resultDetail = contractService.getResultForOid(oid);
    if (resultDetail == null) {
      // エラー処理
      return contractService.createResponse("NG", "予期せぬエラーが発生しました。", null);
    }

    // レスポンス返却
    return contractService.createResponse("OK", null, resultDetail);
  }

  /**
   * テスト用のスタブ。
   *
   * @param json JSON形式の実行パラメータ
   * @return レスポンス
   */
  @RequestMapping(value = "/test", method = RequestMethod.POST)
  @ApiOperation(value = "テスト用のスタブ。")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "レスポンス")})
  public String test(@ApiParam(value = "JSON形式の実行パラメータ") @RequestBody String json) {
    String response =
        "{\"status\" :\"OK\", \"message\" :\"test\", \"info\" : {\"taskId\" :\"1234567890\"}}";
    return response;
  }

  @ExceptionHandler(RablockSystemException.class)
  @ResponseStatus(code = INTERNAL_SERVER_ERROR)
  public Map<String, String> handleException(final RablockSystemException e) {
    log.error(e.getMessage(), e);
    return Collections.singletonMap("message", "invalid");
  }
}

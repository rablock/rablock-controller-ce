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
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import jp.techarts.bc.constitem.ConstItem;
import jp.techarts.bc.constitem.ConstType;
import jp.techarts.bc.jsonrpc.HttpAuthenticator;
import jp.techarts.bc.prop.GetAppProperties;
import org.bson.Document;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * スマートコントラクト関連のサービスクラス<br>
 * Copyright (c) 2018 Rablock Inc.
 *
 * @author TA
 * @version 1.0
 */
@Service
public class SmartContractService {
  private static final DateTimeFormatter sdf =
      DateTimeFormatter.ofPattern("yyyy/[]M/[]d []H:[]m:[]s");

  /** ブロックサービス */
  private final BlockService blockService;

  /** プールサービス */
  private final PoolService poolService;

  /** 共通処理のサービスクラス */
  private final Common common;

  /** ロガー */
  private final Logger log = LoggerFactory.getLogger(SmartContractService.class);

  private final String cryptoStatus;

  @Autowired
  public SmartContractService(
      final GetAppProperties config,
      final BlockService blockService,
      final PoolService poolService,
      final Common common) {
    this.blockService = blockService;
    this.poolService = poolService;
    this.common = common;
    this.cryptoStatus = config.getCryptoStatus();
  }

  /**
   * 契約定義情報の項目チェックを行う。
   *
   * @param json JSON形式の契約定義情報
   * @return 契約定義情報のJsonNodeオブジェクト データ形式に異常がある場合はNULLを返却。
   */
  public Optional<JsonNode> checkContractDefine(String json) {
    // JSON文字列の変換
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode node = mapper.readTree(json);
      // 項目の存在チェック
      if (!node.has(ConstItem.DATA_TYPE)
          || !node.has(ConstItem.CONTRACT_NUMBER)
          || !node.has(ConstItem.CONTRACT_NAME)
          || !node.has(ConstItem.CONTRACT_FUNC)) {
        log.error("契約定義に必要な項目がありません。");
        return Optional.empty();
      }
      // オペレーション定義のチェック
      JsonNode func = node.get(ConstItem.CONTRACT_FUNC);
      if (!(func.isArray())) {
        // 定義がリストでない場合は不正
        log.error("オペレーション定義がリストでありません。");
        return Optional.empty();
      }
      if (func.size() == 0) {
        // 定義が空のリストの場合は不正
        log.error("オペレーション定義が存在しません。");
        return Optional.empty();
      }
      try {
        StreamSupport.stream(func.spliterator(), false)
            .map(
                elem -> {
                  if (!(elem.isObject())) {
                    // オペレーション定義が連想配列でない場合は不正
                    throw new RuntimeException("オペレーションの定義が不正です。");
                  }
                  return elem;
                })
            .forEach(
                elem -> {
                  if (!elem.has(ConstItem.CONTRACT_FUNC_ID)
                      || !elem.has(ConstItem.CONTRACT_FUNC_NAME)
                      || !elem.has(ConstItem.CONTRACT_FUNC_URL)) {
                    // オペレーションの項目が存在しない場合は不正
                    throw new RuntimeException("オペレーションの必須項目が存在しません。");
                  }
                });
      } catch (RuntimeException e) {
        log.error(e.getMessage());
        return Optional.empty();
      }
      return Optional.of(node);
    } catch (JsonProcessingException e) {
      log.error("JSON文字列のパースに失敗");
      return Optional.empty();
    }
  }

  /**
   * 契約定義情報に対して新規登録前のチェック処理を行う。
   *
   * @param define 契約定義情報
   * @return true:登録OK / false:登録NG
   * @throws RablockSystemException
   */
  public boolean checkNewDefine(JsonNode define) throws RablockSystemException {

    // 契約番号の重複チェック
    String number = define.get(ConstItem.CONTRACT_NUMBER).asText();

    // ブロックを検索
    List<Document> blockList = blockService.getByKeyValue(ConstItem.CONTRACT_NUMBER, number);
    if (!blockList.isEmpty()) {
      log.error("新規登録対象の契約番号：" + number + " はすでに登録されています。");
      return false;
    }

    // プールを検索
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put(ConstItem.CONTRACT_NUMBER, number);
    List<Document> poolList = poolService.getByKeyValue(ConstItem.CONTRACT_NUMBER, node);
    if (!poolList.isEmpty()) {
      log.error("新規登録対象の契約番号：" + number + " はすでに登録されています。");
      return false;
    }

    return true;
  }

  /**
   * 契約定義情報に対して変更・削除前のチェック処理を行う。
   *
   * @param define 契約定義情報
   * @return true:変更・削除OK / false:変更・削除NG
   * @throws RablockSystemException
   */
  public boolean checkModifyDeleteDefine(JsonNode define) throws RablockSystemException {

    // 契約番号の変更チェック
    String originalId = define.get(ConstItem.DATA_ORIGINAL_ID).asText();
    String number = define.get(ConstItem.CONTRACT_NUMBER).asText();
    if (ConstType.MODIFY.equals(define.get(ConstItem.DATA_TYPE).asText())) {
      // 変更元データ取得
      Optional<Document> objOrEmpty =
          blockService
              .findByOidinBlock(originalId)
              .or(() -> poolService.getDataByOidinPool(originalId));
      if (objOrEmpty.isEmpty()) {
        log.error("変更対象データが存在しません。");
        return false;
      }
      Document obj = objOrEmpty.get();
      // 変更前後のデータで契約番号が変わっていたらエラー
      String beforeNumber = obj.get(ConstItem.CONTRACT_NUMBER, String.class);
      if (!number.equals(beforeNumber)) {
        log.error("契約番号は変更できません。");
        return false;
      }
    }

    // 変更・削除の重複チェック
    List<String> modifyDeleteList = blockService.getModifyDeleteListinBlock();
    List<String> poolModifyDeleteList = poolService.getModifyDeleteListinPool();
    modifyDeleteList.addAll(poolModifyDeleteList);
    if (modifyDeleteList.contains(originalId)) {
      log.error("対象データはすでに変更されたか削除されています。");
      return false;
    }

    return true;
  }

  /**
   * ユーザー契約情報の項目チェックを行う。
   *
   * @param json JSON形式のユーザー契約情報
   * @return ユーザー契約情報のJsonNodeオブジェクト データ形式に異常がある場合はNULLを返却。
   * @throws RablockSystemException
   */
  public JsonNode checkContractAgree(String json) throws RablockSystemException {
    final ObjectMapper mapper = new ObjectMapper();
    // JSON文字列の変換
    try {
      final JsonNode node = mapper.readTree(json);
      // 項目チェック
      if (!node.has(ConstItem.DATA_TYPE)
          || !node.has(ConstItem.CONTRACT_USER)
          || !node.has(ConstItem.CONTRACT_NUMBER)
          || !node.has(ConstItem.CONTRACT_AGREE_ID)) {
        log.error("ユーザー契約に必要な項目がありません。");
        return null;
      }

      if (ConstType.MODIFY.equals(node.get(ConstItem.DATA_TYPE).asText())
          || ConstType.DELETE.equals(node.get(ConstItem.DATA_TYPE).asText())) {
        // 変更・削除の重複チェック
        String originalId = node.get(ConstItem.DATA_ORIGINAL_ID).asText();
        List<String> modifyDeleteList = blockService.getModifyDeleteListinBlock();
        List<String> poolModifyDeleteList = poolService.getModifyDeleteListinPool();
        modifyDeleteList.addAll(poolModifyDeleteList);
        if (modifyDeleteList.contains(originalId)) {
          log.error("対象データはすでに変更されたか削除されています。");
          return null;
        }

        // ユーザー契約IDの変更チェック
        String agreeId = node.get(ConstItem.CONTRACT_AGREE_ID).asText();
        if (ConstType.MODIFY.equals(node.get(ConstItem.DATA_TYPE).asText())) {
          // 変更元データ取得
          Optional<Document> objOrEmpty =
              blockService
                  .findByOidinBlock(originalId)
                  .or(() -> poolService.getDataByOidinPool(originalId));
          if (objOrEmpty.isEmpty()) {
            log.error("変更対象データが存在しません。");
            return null;
          }
          Document obj = objOrEmpty.get();
          // 変更前後のデータでユーザー契約IDが変わっていたらエラー
          String beforeAgreeId = obj.get(ConstItem.CONTRACT_AGREE_ID, String.class);
          if (!agreeId.equals(beforeAgreeId)) {
            log.error("ユーザー契約IDは変更できません。");
            return null;
          }
        }
      }

      // 契約開始・終了日時のチェック
      LocalDateTime startDate;
      LocalDateTime endDate;
      if (node.hasNonNull(ConstItem.CONTRACT_START_DATE)
          && !"".equals(node.get(ConstItem.CONTRACT_START_DATE).asText())) {
        // 開始日時をDate型に変更
        startDate = LocalDateTime.parse(node.get(ConstItem.CONTRACT_START_DATE).asText(), sdf);
      } else {
        startDate = null;
      }
      if (node.hasNonNull(ConstItem.CONTRACT_END_DATE)
          && !"".equals(node.get(ConstItem.CONTRACT_END_DATE).asText())) {
        // 終了日時をDate型に変更
        endDate = LocalDateTime.parse(node.get(ConstItem.CONTRACT_END_DATE).asText(), sdf);
      } else {
        endDate = null;
      }
      if (startDate != null && endDate != null) {
        if (!endDate.isAfter(startDate)) {
          // 終了日時が開始日時より過去の場合はエラー
          log.error("契約終了日時：" + endDate + " が契約開始日時：" + startDate + "より過去です。");
          return null;
        }
      }
      return node;
    } catch (JsonProcessingException e) {
      log.error("JSON文字列のパースに失敗");
      return null;
    }
  }

  /**
   * 実行パラメータの項目チェックを行う。
   *
   * @param json JSON形式の実行パラメータ
   * @return 実行パラメータのJsonNodeオブジェクト データ形式に異常がある場合はNULLを返却。
   */
  public JsonNode checkExecParam(String json) {
    final ObjectMapper mapper = new ObjectMapper();
    // JSON文字列の変換
    try {
      final JsonNode node = mapper.readTree(json);
      // 項目チェック
      if (!node.has("oid") || !node.has(ConstItem.CONTRACT_FUNC_ID)) {
        log.error("実行パラメータが不正です。");
        return null;
      }
      return node;
    } catch (JsonProcessingException e) {
      log.error("JSON文字列のパースに失敗");
      return null;
    }
  }

  /**
   * 指定されオブジェクトIDをもとにユーザー契約情報を取得し、 契約内容のチェックを行う。
   *
   * @param oid オブジェクトID
   * @return 有効なユーザー契約情報 存在しないまたは有効でない場合はNULLを返却。
   * @throws RablockSystemException
   */
  public JsonNode checkExecAgree(String oid) throws RablockSystemException {

    // ユーザー契約情報取得
    Optional<Document> objOrEmpty =
        blockService.findByOidinBlock(oid).or(() -> poolService.getDataByOidinPool(oid));
    if (objOrEmpty.isEmpty()) {
      log.error("指定されたユーザー契約情報は存在しません。");
      return null;
    }
    Document obj = objOrEmpty.get();

    // ユーザー契約情報のチェック
    String contract = obj.get(ConstItem.CONTRACT_TYPE, String.class);
    if (!ConstType.CONTRACT_AGREE.equals(contract)) {
      // 契約タイプが"agree"でない場合はエラー
      log.error("指定されたデータはユーザー契約情報ではありません。");
      return null;
    }

    try {
      // 有効期限チェック
      String start = obj.get(ConstItem.CONTRACT_START_DATE, String.class);
      LocalDateTime startDate =
          (start != null && !"".equals(start)) ? LocalDateTime.parse(start, sdf) : null;
      String end = obj.get(ConstItem.CONTRACT_END_DATE, String.class);
      LocalDateTime endDate =
          (end != null && !"".equals(end)) ? LocalDateTime.parse(end, sdf) : null;
      LocalDateTime now = LocalDateTime.now();
      if (startDate != null && now.isBefore(startDate)) {
        // 現在時刻が契約開始日時より前の場合はエラー
        log.error("まだ契約が開始されていません。");
        return null;
      }
      if (endDate != null && now.isAfter(endDate)) {
        // 現在時刻が契約終了日時より後の場合はエラー
        log.error("契約期間が終了しています。");
        return null;
      }
    } catch (DateTimeParseException e) {
      log.info("Invalid date format", e);
      return null;
    }

    final ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.readTree(obj.toJson());
    } catch (JsonProcessingException e) {
      log.error("予期せぬエラーが発生しました。");
      return null;
    }
  }

  /**
   * 指定され契約番号に該当する契約定義情報を取得し、 定義内容のチェックを行う。
   *
   * @param number 契約番号
   * @param funcid オペレーションID
   * @return 契約定義情報 情報が存在しない、または実行対象のオペレーションが存在しない場合はNULLを返却。
   * @throws RablockSystemException
   */
  public Optional<JsonNode> checkExecDefine(String number, String funcid)
      throws RablockSystemException {

    // 契約定義情報の取得
    Optional<JsonNode> define = getDefineForNumber(number);
    if (define.isEmpty()) {
      log.error("契約番号：" + number + "に該当する契約定義情報が存在しません。");
      return define;
    }

    // 対象オペレーションの存在チェック
    Optional<JsonNode> func = getFunctionForId(funcid, define.get());
    if (func.isEmpty()) {
      log.error("オペレーションID：" + funcid + "に該当するオペレーションは存在しません。");
      return func;
    }

    return define;
  }

  /**
   * 登録されているすべての契約定義情報を返却する。
   *
   * @return 契約定義リスト
   * @throws RablockSystemException
   * @throws JsonProcessingException
   * @throws JsonMappingException
   */
  public JsonNode getDefineList() throws RablockSystemException, JsonProcessingException {
    ArrayNode list = JsonNodeFactory.instance.arrayNode();
    ObjectMapper mapper = new ObjectMapper();

    // ブロックから検索
    List<Document> blockList =
        blockService.getByKeyValue(ConstItem.CONTRACT_TYPE, ConstType.CONTRACT_DEFINE);
    for (Document elem : blockList) {
      JsonNode json = mapper.readTree(elem.toJson());
      list.add(json);
    }

    // プールから検索
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put(ConstItem.CONTRACT_TYPE, ConstType.CONTRACT_DEFINE);
    List<Document> poolList = poolService.getByKeyValue(ConstItem.CONTRACT_TYPE, node);
    for (Document elem : poolList) {
      JsonNode json = mapper.readTree(elem.toJson());
      list.add(json);
    }

    return list;
  }

  /**
   * 指定された契約番号に該当する契約定義情報を返却する。
   *
   * @param number 契約番号
   * @return 契約定義情報
   * @throws RablockSystemException
   */
  public Optional<JsonNode> getDefineForNumber(String number) throws RablockSystemException {
    if (number == null) {
      return Optional.empty();
    }

    ObjectMapper mapper = new ObjectMapper();
    try {
      // ブロックから検索
      final List<Document> blockDataList =
          blockService.getByKeyValue(ConstItem.CONTRACT_TYPE, ConstType.CONTRACT_DEFINE);
      final List<Document> filteredBlockDataList =
          blockDataList.stream()
              .filter(elem -> number.equals(elem.get(ConstItem.CONTRACT_NUMBER)))
              .collect(Collectors.toList());
      if (filteredBlockDataList.size() > 0) {
        final Document elem = filteredBlockDataList.get(0);
        return Optional.of(mapper.readTree(elem.toJson()));
      }

      // ブロックになければプールを検索
      ObjectNode node = JsonNodeFactory.instance.objectNode();
      node.put(ConstItem.CONTRACT_TYPE, ConstType.CONTRACT_DEFINE);
      List<Document> poolList = poolService.getByKeyValue(ConstItem.CONTRACT_TYPE, node);
      List<Document> filteredPoolList =
          poolList.stream()
              .filter(elem -> number.equals(elem.get(ConstItem.CONTRACT_NUMBER)))
              .collect(Collectors.toList());
      if (filteredPoolList.size() > 0) {
        final Document elem = filteredPoolList.get(0);
        return Optional.of(mapper.readTree(elem.toJson()));
      }
    } catch (JsonProcessingException e) {
      log.error("JSON文字列のパースに失敗");
    }
    return Optional.empty();
  }

  /**
   * 指定されたユーザーID、契約番号に該当する、ユーザー契約一覧を返却する。 ただし、ユーザーID/契約番号がnullまたは空文字の場合は、その項目を検索キーから除外する。
   *
   * @param user ユーザーID
   * @param number 契約番号
   * @return ユーザー契約一覧
   * @throws RablockSystemException
   */
  public JsonNode getAgreeList(String user, String number) throws RablockSystemException {
    ObjectMapper mapper = new ObjectMapper();

    try {
      ArrayNode list = JsonNodeFactory.instance.arrayNode();
      // ブロックから検索
      Optional<JsonProcessingException> exBlock =
          blockService.getByKeyValue(ConstItem.CONTRACT_TYPE, ConstType.CONTRACT_AGREE).stream()
              .filter(
                  elem ->
                      user == null
                          || "".equals(user)
                          || user.equals(elem.get(ConstItem.CONTRACT_USER)))
              .filter(
                  elem ->
                      number == null
                          || "".equals(number)
                          || number.equals(elem.get(ConstItem.CONTRACT_NUMBER)))
              .map(
                  elem -> {
                    try {
                      JsonNode json = mapper.readTree(elem.toJson());
                      list.add(json);
                      return null;
                    } catch (JsonProcessingException e) {
                      return e;
                    }
                  })
              .filter(x -> x != null)
              .findFirst();
      if (exBlock.isPresent()) {
        throw exBlock.get();
      }
      // プールから検索
      ObjectNode node = JsonNodeFactory.instance.objectNode();
      node.put(ConstItem.CONTRACT_TYPE, ConstType.CONTRACT_AGREE);
      Optional<JsonProcessingException> exPool =
          poolService.getByKeyValue(ConstItem.CONTRACT_TYPE, node).stream()
              .filter(
                  elem ->
                      user == null
                          || "".equals(user)
                          || user.equals(elem.get(ConstItem.CONTRACT_USER)))
              .filter(
                  elem ->
                      number == null
                          || "".equals(number)
                          || number.equals(elem.get(ConstItem.CONTRACT_NUMBER)))
              .map(
                  elem -> {
                    try {
                      JsonNode json = mapper.readTree(elem.toJson());
                      list.add(json);
                      return null;
                    } catch (JsonProcessingException e) {
                      return e;
                    }
                  })
              .filter(x -> x != null)
              .findFirst();
      if (exPool.isPresent()) {
        throw exPool.get();
      }

      return list;
    } catch (JsonProcessingException e) {
      log.error("JSON文字列のパースに失敗");
      return null;
    }
  }

  /**
   * 指定されたoidに該当するユーザー契約情報を返却する。
   *
   * @param oid オブジェクトID
   * @return ユーザー契約情報
   * @throws RablockSystemException
   */
  public JsonNode getAgreeForOid(String oid) throws RablockSystemException {

    // ブロックから検索
    Optional<Document> dataOrEmpty =
        blockService
            .findByOidinBlock(oid)
            .or(
                () ->
                    // ブロックにない場合はプールを検索
                    poolService.getDataByOidinPool(oid));

    // JsonNodeオブジェクトに変換
    JsonNode node = JsonNodeFactory.instance.objectNode();
    if (dataOrEmpty.isPresent()) {
      Document data = dataOrEmpty.get();
      if (ConstType.CONTRACT_AGREE.equals(data.get(ConstItem.CONTRACT_TYPE))) {
        try {
          ObjectMapper mapper = new ObjectMapper();
          node = mapper.readTree(data.toJson());
        } catch (JsonProcessingException e) {
          log.error("JSON文字列のパースに失敗");
          return null;
        }
      }
    }

    return node;
  }

  /**
   * 指定された契約のオペレーションを実行する。
   *
   * @param execParam 実行パラメータ
   * @param define 契約定義情報
   * @param agree ユーザー契約情報
   * @return レスポンス
   * @throws RablockSystemException
   */
  public String execContract(JsonNode execParam, JsonNode define, JsonNode agree)
      throws RablockSystemException {
    // 実行結果格納
    ObjectNode execLog = JsonNodeFactory.instance.objectNode();
    execLog.put(ConstItem.DATA_TYPE, ConstType.NEW);
    execLog.put(ConstItem.CONTRACT_TYPE, ConstType.CONTRACT_RESULT);
    execLog.put(ConstItem.CONTRACT_USER, agree.get(ConstItem.CONTRACT_USER).asText());
    execLog.put(ConstItem.CONTRACT_NUMBER, define.get(ConstItem.CONTRACT_NUMBER).asText());
    execLog.put(ConstItem.CONTRACT_NAME, define.get(ConstItem.CONTRACT_NAME).asText());
    execLog.put(ConstItem.CONTRACT_AGREE_ID, agree.get(ConstItem.CONTRACT_AGREE_ID).asText());
    execLog.put(ConstItem.CONTRACT_AGREE_NAME, agree.get(ConstItem.CONTRACT_AGREE_NAME).asText());
    execLog.put(ConstItem.CONTRACT_EXEC_DATE, common.getCurrentTime());

    // 対象オペレーションのURL取得
    final String funcid = execParam.get(ConstItem.CONTRACT_FUNC_ID).asText();
    final Optional<JsonNode> funcOrEmpty = getFunctionForId(funcid, define);
    if (funcOrEmpty.isEmpty()) {
      // 該当するオペレーションが存在しない
      String errMsg = "オペレーションID：" + funcid + "に該当するオペレーションが存在しません。";
      execLog.put(ConstItem.CONTRACT_EXEC_RESULT, "NG");
      execLog.put(ConstItem.CONTRACT_EXEC_MSG, errMsg);
      log.error(errMsg);
      return createResponse("NG", errMsg, null);
    }
    final JsonNode func = funcOrEmpty.get();
    final String funcUrl = func.get(ConstItem.CONTRACT_FUNC_URL).asText();
    execLog.put(ConstItem.CONTRACT_FUNC_ID, funcid);
    execLog.put(ConstItem.CONTRACT_FUNC_NAME, func.get(ConstItem.CONTRACT_FUNC_NAME).asText());
    execLog.put(ConstItem.CONTRACT_FUNC_URL, func.get(ConstItem.CONTRACT_FUNC_URL).asText());

    // 送信データの作成
    ObjectNode param = JsonNodeFactory.instance.objectNode();
    param.setAll((ObjectNode) agree);
    param.setAll((ObjectNode) execParam);
    String postData = param.toString();

    // 実行
    String result = null; // TODO Should be refactored later.
    try {
      URL url = new URL(funcUrl);

      // コネクションの生成
      HttpURLConnection con = (HttpURLConnection) url.openConnection();
      try (AutoCloseable conc = () -> con.disconnect()) {
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        con.setRequestProperty("Accept-Language", "jp");
        con.setRequestProperty("Content-Type", "application/JSON; charset=utf-8");
        con.setRequestProperty("Content-Length", String.valueOf(postData.length()));
        // ユーザ認証情報の設定
        HttpAuthenticator httpAuth = new HttpAuthenticator("RaBlock", "xx7URRS6LwxF");
        Authenticator.setDefault(httpAuth);

        // データ送信
        OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
        out.write(postData);
        out.flush();
        con.connect();

        int httpStatus = con.getResponseCode();
        if (httpStatus == HttpURLConnection.HTTP_OK) {
          // 通信に成功した場合、レスポンスデータを取得
          final InputStream in = con.getInputStream();
          String encoding = con.getContentEncoding();
          if (null == encoding) {
            encoding = "UTF-8";
          }
          String res;
          try (final InputStreamReader inReader = new InputStreamReader(in, encoding);
              final BufferedReader bufReader = new BufferedReader(inReader)) {
            res = bufReader.lines().collect(Collectors.joining());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode resJson = mapper.readTree(res);
            JsonNode info;
            if (resJson.has("info")) {
              info = resJson.get("info");
              execLog.set(ConstItem.CONTRACT_EXEC_INFO, info);
            } else {
              info = null;
            }
            result =
                createResponse(
                    resJson.get("status").asText(), resJson.get("message").asText(), info);
            execLog.put(ConstItem.CONTRACT_EXEC_RESULT, resJson.get("status").asText());
            execLog.put(ConstItem.CONTRACT_EXEC_MSG, resJson.get("message").asText());
          }
        } else {
          // 通信が失敗した場合
          String errMsg = "通信が失敗しました。HTTPステータスコード：" + httpStatus;
          execLog.put(ConstItem.CONTRACT_EXEC_RESULT, "NG");
          execLog.put(ConstItem.CONTRACT_EXEC_MSG, errMsg);
          log.error(errMsg);
          result = createResponse("NG", errMsg, null);
        }
      }
    } catch (Exception e) {
      String errMsg = "通信中に予期せぬエラーが発生しました。";
      execLog.put(ConstItem.CONTRACT_EXEC_RESULT, "NG");
      execLog.put(ConstItem.CONTRACT_EXEC_MSG, errMsg);
      log.error(errMsg);
      result = createResponse("NG", errMsg, null);
    }

    // 実行結果のデータ登録
    if ("ON".equals(this.cryptoStatus)) {
      // TODO:データの暗号化処理
    }
    JSONObject res = new JSONObject(execLog.toString());
    Document pool = poolService.setPool(res, common.getCurrentTime());
    if (pool == null) {
      String errMsg = "実行結果の登録に失敗しました。";
      log.error(errMsg);
      result = createResponse("NG", errMsg, null);
    }
    return result;
  }

  /**
   * 指定されたユーザーID、契約番号に該当する、実行結果一覧を返却する。 ただし、ユーザーID/契約番号がnullまたは空文字の場合は、その項目を検索キーから除外する。
   *
   * @param user ユーザーID
   * @param number 契約番号
   * @return 実行結果一覧
   * @throws RablockSystemException
   */
  public JsonNode getResultList(String user, String number) throws RablockSystemException {
    ArrayNode list = JsonNodeFactory.instance.arrayNode();
    ObjectMapper mapper = new ObjectMapper();
    try {
      // ブロックから検索
      Optional<JsonProcessingException> blockList =
          blockService.getByKeyValue(ConstItem.CONTRACT_TYPE, ConstType.CONTRACT_RESULT).stream()
              .filter(
                  elem ->
                      user == null
                          || "".equals(user)
                          || user.equals(elem.get(ConstItem.CONTRACT_USER)))
              .filter(
                  elem ->
                      number == null
                          || "".equals(number)
                          || number.equals(elem.get(ConstItem.CONTRACT_NUMBER)))
              .map(
                  elem -> {
                    try {
                      JsonNode json = mapper.readTree(elem.toJson());
                      list.add(json);
                      return null;
                    } catch (JsonProcessingException e) {
                      return e;
                    }
                  })
              .filter(x -> x != null)
              .findFirst();
      if (blockList.isPresent()) {
        throw blockList.get();
      }

      // プールから検索
      ObjectNode node = JsonNodeFactory.instance.objectNode();
      node.put(ConstItem.CONTRACT_TYPE, ConstType.CONTRACT_RESULT);
      Optional<JsonProcessingException> resultSearchInPool =
          poolService.getByKeyValue(ConstItem.CONTRACT_TYPE, node).stream()
              .filter(
                  elem ->
                      user == null
                          || "".equals(user)
                          || user.equals(elem.get(ConstItem.CONTRACT_USER)))
              .filter(
                  elem ->
                      number == null
                          || "".equals(number)
                          || number.equals(elem.get(ConstItem.CONTRACT_NUMBER)))
              .map(
                  elem -> {
                    try {
                      JsonNode json = mapper.readTree(elem.toJson());
                      list.add(json);
                      return null;
                    } catch (JsonProcessingException e) {
                      return e;
                    }
                  })
              .filter(x -> x != null)
              .findFirst();
      if (resultSearchInPool.isPresent()) {
        throw resultSearchInPool.get();
      }
    } catch (JsonProcessingException e) {
      log.error("JSON文字列のパースに失敗");
      return null;
    }

    return list;
  }

  /**
   * 指定されたoidに該当する実行結果情報を返却する。
   *
   * @param oid オブジェクトID
   * @return 実行結果情報
   * @throws RablockSystemException
   */
  public JsonNode getResultForOid(String oid) throws RablockSystemException {

    // ブロックから検索
    Optional<Document> dataOrEmpty =
        blockService
            .findByOidinBlock(oid)
            .or(
                () ->
                    // ブロックにない場合はプールを検索
                    poolService.getDataByOidinPool(oid));

    // JsonNodeオブジェクトに変換
    JsonNode node = JsonNodeFactory.instance.objectNode();
    if (dataOrEmpty.isPresent()) {
      Document data = dataOrEmpty.get();
      if (ConstType.CONTRACT_RESULT.equals(data.get(ConstItem.CONTRACT_TYPE))) {
        try {
          ObjectMapper mapper = new ObjectMapper();
          node = mapper.readTree(data.toJson());
        } catch (JsonProcessingException e) {
          log.error("JSON文字列のパースに失敗");
          return null;
        }
      }
    }

    return node;
  }

  /**
   * 返却するレスポンス(JSON形式)を生成する。
   *
   * @param status 処理結果（OK/NG）
   * @param message メッセージ
   * @param info 付加情報
   * @return レスポンス文字列（JSON形式）
   */
  public String createResponse(String status, String message, JsonNode info) {

    ObjectNode response = JsonNodeFactory.instance.objectNode();
    response.put("status", status);
    if (message != null && !"".equals(message)) {
      response.put("message", message);
    }
    if (info != null) {
      response.set("info", info);
    }

    return response.toString();
  }

  /**
   * JSON文字列から指定されたキーに該当する値を取得する。
   *
   * @param json JSON文字列
   * @return キーに該当する値をwrapしたOptionalオブジェクト
   */
  public Optional<String> getValueFromJson(String json, String key) {
    if (json == null || "".equals(json)) {
      return Optional.empty();
    }

    try {
      // 変換
      ObjectMapper mapper = new ObjectMapper();
      JsonNode node = mapper.readTree(json);

      // 項目存在チェック
      if (node.has(key) && !node.get(key).isNull() && !"".equals(node.get(key).asText())) {
        return Optional.of(node.get(key).asText());
      }
    } catch (JsonProcessingException e) {
      log.error("JSONへの変換に失敗しました。");
    }

    return Optional.empty();
  }

  /**
   * 契約定義情報をもとに、指定されたオペレーションIDに該当するオペレーション情報を返却する。
   *
   * @param funcid オペレーションID
   * @param define 契約定義情報
   * @return オペレーション情報 存在しない場合はNULLを返却する。
   */
  private Optional<JsonNode> getFunctionForId(String funcid, JsonNode define) {
    JsonNode functions = define.get(ConstItem.CONTRACT_FUNC);
    Spliterator<JsonNode> spliterator =
        Spliterators.spliteratorUnknownSize(functions.iterator(), 0);
    return StreamSupport.stream(spliterator, false)
        .filter(elem -> funcid.equals(elem.get(ConstItem.CONTRACT_FUNC_ID).asText()))
        .findFirst();
  }
}

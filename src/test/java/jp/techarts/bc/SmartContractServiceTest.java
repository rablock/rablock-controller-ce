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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.List;
import jp.techarts.bc.constitem.ConstItem;
import jp.techarts.bc.test.SampleMethod;
import junit.framework.AssertionFailedError;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * スマートコントラクト関連サービスクラスのテストクラス<br>
 * Copyright (c) 2018 Rablock Inc.
 *
 * @author TA
 * @version 1.0
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class SmartContractServiceTest {

  /** テスト用メソッド */
  @Autowired private SampleMethod test;

  /** プールサービス */
  @Autowired private PoolService poolService;

  /** 【テスト対象】スマートコントラクトサービス */
  @Autowired private SmartContractService target;

  /**
   * [ケース１：正常ケース] 正常な契約定義情報の場合、戻り値としてテストデータに対応するJSONObjectが返却される。
   *
   * @throws JSONException
   */
  @Test
  public void testCheckContractDefine_case1() {
    // テストデータ
    String testData =
        "{\"type\":\"new\",\"contract\":\"define\",\"number\":\"00001\",\"name\":\"スマートコントラクト\","
            + "\"functions\":[{\"funcid\":\"lock\",\"funcname\":\"ロック\",\"funcurl\":\"http://www.yahoo.co.jp\"},"
            + "{\"funcid\":\"unlock\",\"funcname\":\"アンロック\",\"funcurl\":\"http://www.google.com\"}]}";

    // テスト
    JsonNode result = target.checkContractDefine(testData).orElseThrow(AssertionFailedError::new);

    // 検証
    assertEquals("new", result.get("type").asText());
    assertEquals("define", result.get("contract").asText());
    assertEquals("00001", result.get("number").asText());
    assertEquals("スマートコントラクト", result.get("name").asText());
    JsonNode functions = result.get("functions");
    JsonNode func1 = functions.get(0);
    assertEquals("lock", func1.get("funcid").asText());
    assertEquals("ロック", func1.get("funcname").asText());
    assertEquals("http://www.yahoo.co.jp", func1.get("funcurl").asText());
    JsonNode func2 = functions.get(1);
    assertEquals("unlock", func2.get("funcid").asText());
    assertEquals("アンロック", func2.get("funcname").asText());
    assertEquals("http://www.google.com", func2.get("funcurl").asText());
  }

  /** [ケース２：異常ケース] JSON形式でないデータの場合、戻り値としてNULLが返却される。 */
  @Test
  public void testCheckContractDefine_case2() {
    // テストデータ
    String testData = "test";

    // テスト
    JsonNode result = target.checkContractDefine(testData).orElse(null);

    // 検証
    assertNull(result);
  }

  /** [ケース３：異常ケース] データ種別(type)が存在しないデータの場合、戻り値としてNULLが返却される。 */
  @Test
  public void testCheckContractDefine_case3() {
    // テストデータ
    String testData =
        "{\"contract\":\"define\",\"number\":\"00001\",\"name\":\"スマートコントラクト\","
            + "\"functions\":[{\"funcid\":\"lock\",\"funcname\":\"ロック\",\"funcurl\":\"http://www.yahoo.co.jp\"},"
            + "{\"funcid\":\"unlock\",\"funcname\":\"アンロック\",\"funcurl\":\"http://www.google.com\"}]}";

    // テスト
    JsonNode result = target.checkContractDefine(testData).orElse(null);

    // 検証
    assertNull(result);
  }

  /** [ケース４：異常ケース] 契約番号(number)が存在しないデータの場合、戻り値としてNULLが返却される。 */
  @Test
  public void testCheckContractDefine_case4() {
    // テストデータ
    String testData =
        "{\"type\":\"new\",\"contract\":\"define\",\"name\":\"スマートコントラクト\","
            + "\"functions\":[{\"funcid\":\"lock\",\"funcname\":\"ロック\",\"funcurl\":\"http://www.yahoo.co.jp\"},"
            + "{\"funcid\":\"unlock\",\"funcname\":\"アンロック\",\"funcurl\":\"http://www.google.com\"}]}";

    // テスト
    JsonNode result = target.checkContractDefine(testData).orElse(null);

    // 検証
    assertNull(result);
  }

  /** [ケース５：異常ケース] 契約名称(name)が存在しないデータの場合、戻り値としてNULLが返却される。 */
  @Test
  public void testCheckContractDefine_case5() {
    // テストデータ
    String testData =
        "{\"type\":\"new\",\"contract\":\"define\",\"number\":\"00001\","
            + "\"functions\":[{\"funcid\":\"lock\",\"funcname\":\"ロック\",\"funcurl\":\"http://www.yahoo.co.jp\"},"
            + "{\"funcid\":\"unlock\",\"funcname\":\"アンロック\",\"funcurl\":\"http://www.google.com\"}]}";

    // テスト
    JsonNode result = target.checkContractDefine(testData).orElse(null);

    // 検証
    assertNull(result);
  }

  /** [ケース６：異常ケース] オペレーション定義(functions)が存在しないデータの場合、戻り値としてNULLが返却される。 */
  @Test
  public void testCheckContractDefine_case6() {
    // テストデータ
    String testData =
        "{\"type\":\"new\",\"contract\":\"define\",\"number\":\"00001\",\"name\":\"スマートコントラクト\"}";

    // テスト
    JsonNode result = target.checkContractDefine(testData).orElse(null);

    // 検証
    assertNull(result);
  }

  /** [ケース７：異常ケース] オペレーション定義が空データの場合、戻り値としてNULLが返却される。 */
  @Test
  public void testCheckContractDefine_case7() {
    // テストデータ
    String testData =
        "{\"type\":\"new\",\"contract\":\"define\",\"number\":\"00001\",\"name\":\"スマートコントラクト\","
            + "\"functions\":[]}";

    // テスト
    JsonNode result = target.checkContractDefine(testData).orElse(null);

    // 検証
    assertNull(result);
  }

  /** [ケース８：異常ケース] オペレーション定義のオペレーションID(funcid)が存在しない場合、戻り値としてNULLが返却される。 */
  @Test
  public void testCheckContractDefine_case8() {
    // テストデータ
    String testData =
        "{\"type\":\"new\",\"contract\":\"define\",\"number\":\"00001\",\"name\":\"スマートコントラクト\","
            + "\"functions\":[{\"funcname\":\"ロック\",\"funcurl\":\"http://www.yahoo.co.jp\"},"
            + "{\"funcid\":\"unlock\",\"funcname\":\"アンロック\",\"funcurl\":\"http://www.google.com\"}]}";

    // テスト
    JsonNode result = target.checkContractDefine(testData).orElse(null);

    // 検証
    assertNull(result);
  }

  /** [ケース９：異常ケース] オペレーション定義のオペレーション名(funcname)が存在しない場合、戻り値としてNULLが返却される。 */
  @Test
  public void testCheckContractDefine_case9() {
    // テストデータ
    String testData =
        "{\"type\":\"new\",\"contract\":\"define\",\"number\":\"00001\",\"name\":\"スマートコントラクト\","
            + "\"functions\":[{\"funcid\":\"lock\",\"funcname\":\"ロック\",\"funcurl\":\"http://www.yahoo.co.jp\"},"
            + "{\"funcid\":\"unlock\",\"funcurl\":\"http://www.google.com\"}]}";

    // テスト
    JsonNode result = target.checkContractDefine(testData).orElse(null);

    // 検証
    assertNull(result);
  }

  /** [ケース１０：異常ケース] オペレーション定義のオペレーションURL(funcurl)が存在しない場合、戻り値としてNULLが返却される。 */
  @Test
  public void testCheckContractDefine_case10() {
    // テストデータ
    String testData =
        "{\"type\":\"new\",\"contract\":\"define\",\"number\":\"00001\",\"name\":\"スマートコントラクト\","
            + "\"functions\":[{\"funcid\":\"lock\",\"funcname\":\"ロック\"},"
            + "{\"funcid\":\"unlock\",\"funcname\":\"アンロック\",\"funcurl\":\"http://www.google.com\"}]}";

    // テスト
    JsonNode result = target.checkContractDefine(testData).orElse(null);

    // 検証
    assertNull(result);
  }

  /**
   * [ケース１：正常ケース] 契約番号の重複がない場合、trueが返却される。
   *
   * @throws IOException
   * @throws RablockSystemException
   */
  @Test
  public void testCheckNewDefine_case1() throws IOException, RablockSystemException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testData =
        "{\"type\":\"new\",\"contract\":\"define\",\"number\":\"00001\",\"name\":\"スマートコントラクト\","
            + "\"functions\":[{\"funcid\":\"lock\",\"funcname\":\"ロック\",\"funcurl\":\"http://www.yahoo.co.jp\"},"
            + "{\"funcid\":\"unlock\",\"funcname\":\"アンロック\",\"funcurl\":\"http://www.google.com\"}]}";

    // テスト
    ObjectMapper mapper = new ObjectMapper();
    JsonNode json = mapper.readTree(testData);
    boolean result = target.checkNewDefine(json);

    // 検証
    assertTrue(result);
  }

  /**
   * [ケース２：異常ケース] 契約番号の重複がある場合、falseが返却される。
   *
   * @throws IOException
   * @throws RablockSystemException
   */
  @Test
  public void testCheckNewDefine_case2() throws IOException, RablockSystemException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testData =
        "{\"type\":\"new\",\"contract\":\"define\",\"number\":\"00001\",\"name\":\"スマートコントラクト\","
            + "\"functions\":[{\"funcid\":\"lock\",\"funcname\":\"ロック\",\"funcurl\":\"http://www.yahoo.co.jp\"},"
            + "{\"funcid\":\"unlock\",\"funcname\":\"アンロック\",\"funcurl\":\"http://www.google.com\"}]}";

    // テスト前に同一契約番号のデータを登録
    poolService.setPool(new JSONObject(testData), "2018/11/16 12:00:00");

    // テスト
    ObjectMapper mapper = new ObjectMapper();
    JsonNode json = mapper.readTree(testData);
    boolean result = target.checkNewDefine(json);

    // 検証
    assertTrue(!result);

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース１：正常ケース] 契約定義情報の変更で、変更前後で契約番号の変更は無しの場合、trueが返却される。
   *
   * @throws RablockSystemException
   * @throws JSONException
   * @throws IOException
   */
  @Test
  public void testCheckModifyDeleteDefine_case1()
      throws JSONException, RablockSystemException, IOException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testDataPre =
        "{\"type\":\"new\",\"contract\":\"define\",\"number\":\"00001\",\"name\":\"スマートコントラクト\","
            + "\"functions\":[{\"funcid\":\"lock\",\"funcname\":\"ロック\",\"funcurl\":\"http://www.yahoo.co.jp\"},"
            + "{\"funcid\":\"unlock\",\"funcname\":\"アンロック\",\"funcurl\":\"http://www.google.com\"}]}";
    String testDataAfter =
        "{\"type\":\"modify\",\"contract\":\"define\",\"number\":\"00001\",\"name\":\"スマートコントラクト\","
            + "\"functions\":[{\"funcid\":\"lock\",\"funcname\":\"ロック\",\"funcurl\":\"http://www.yahoo.co.jp\"},"
            + "{\"funcid\":\"unlock\",\"funcname\":\"アンロック\",\"funcurl\":\"http://www.google.com\"}]}";

    // テスト前に契約定義のデータを登録
    poolService.setPool(new JSONObject(testDataPre), "2018/11/16 12:00:00");
    // 登録したデータのオブジェクトID取得
    List<Document> poolList = poolService.getAllPool();
    String oid = test.getOid(poolList.get(0));
    // 登録対象のデータにoriginal_idとして上記で取得したオブジェクトIDを設定しておく
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode after = (ObjectNode) mapper.readTree(testDataAfter);
    after.put("original_id", oid);

    // テスト
    boolean result = target.checkModifyDeleteDefine(after);

    // 検証
    assertTrue(result);

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース２：異常ケース] 契約定義情報の変更で、変更前後で契約番号の変更がありの場合、falseが返却される。
   *
   * @throws RablockSystemException
   * @throws IOException
   */
  @Test
  public void testCheckModifyDeleteDefine_case2() throws RablockSystemException, IOException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testDataPre =
        "{\"type\":\"new\",\"contract\":\"define\",\"number\":\"00001\",\"name\":\"スマートコントラクト\","
            + "\"functions\":[{\"funcid\":\"lock\",\"funcname\":\"ロック\",\"funcurl\":\"http://www.yahoo.co.jp\"},"
            + "{\"funcid\":\"unlock\",\"funcname\":\"アンロック\",\"funcurl\":\"http://www.google.com\"}]}";
    String testDataAfter =
        "{\"type\":\"modify\",\"contract\":\"define\",\"number\":\"00002\",\"name\":\"スマートコントラクト\","
            + "\"functions\":[{\"funcid\":\"lock\",\"funcname\":\"ロック\",\"funcurl\":\"http://www.yahoo.co.jp\"},"
            + "{\"funcid\":\"unlock\",\"funcname\":\"アンロック\",\"funcurl\":\"http://www.google.com\"}]}";

    // テスト前に契約定義のデータを登録
    poolService.setPool(new JSONObject(testDataPre), "2018/11/16 12:00:00");
    // 登録したデータのオブジェクトID取得
    List<Document> poolList = poolService.getAllPool();
    String oid = test.getOid(poolList.get(0));
    // 登録対象のデータにoriginal_idとして上記で取得したオブジェクトIDを設定しておく
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode after = (ObjectNode) mapper.readTree(testDataAfter);
    after.put("original_id", oid);

    // テスト
    boolean result = target.checkModifyDeleteDefine(after);

    // 検証
    assertTrue(!result);

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース３：異常ケース] 契約定義情報の変更で、変更元データがすでに変更されていた場合、falseが返却される。
   *
   * @throws RablockSystemException
   * @throws JSONException
   * @throws IOException
   */
  @Test
  public void testCheckModifyDeleteDefine_case3()
      throws JSONException, RablockSystemException, IOException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testDataPre =
        "{\"type\":\"new\",\"contract\":\"define\",\"number\":\"00001\",\"name\":\"スマートコントラクト\","
            + "\"functions\":[{\"funcid\":\"lock\",\"funcname\":\"ロック\",\"funcurl\":\"http://www.yahoo.co.jp\"},"
            + "{\"funcid\":\"unlock\",\"funcname\":\"アンロック\",\"funcurl\":\"http://www.google.com\"}]}";
    String testDataAfter =
        "{\"type\":\"modify\",\"contract\":\"define\",\"number\":\"00001\",\"name\":\"スマートコントラクト\","
            + "\"functions\":[{\"funcid\":\"lock\",\"funcname\":\"ロック\",\"funcurl\":\"http://www.yahoo.co.jp\"},"
            + "{\"funcid\":\"unlock\",\"funcname\":\"アンロック\",\"funcurl\":\"http://www.google.com\"}]}";
    String testDataMod =
        "{type:\"modify\",contract:\"define\",number:\"00001\",name:\"テスト\","
            + "functions:[{funcid:\"lock\",funcname:\"ロック\",funcurl:\"http://www.yahoo.co.jp\"},"
            + "{funcid:\"unlock\",funcname:\"アンロック\",funcurl:\"http://www.google.com\"}]}";

    // テスト前に契約定義のデータを登録、そして変更しておく
    poolService.setPool(new JSONObject(testDataPre), "2018/11/16 12:00:00");
    List<Document> poolList = poolService.getAllPool();
    String oid = test.getOid(poolList.get(0));
    JSONObject mod = new JSONObject(testDataMod);
    mod.put("original_id", oid);
    poolService.setPool(mod, "2018/11/16 12:01:00");

    // テスト
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode after = (ObjectNode) mapper.readTree(testDataAfter);
    after.put("original_id", oid);
    boolean result = target.checkModifyDeleteDefine(after);

    // 検証
    assertTrue(!result);

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース１：正常ケース] 正常なユーザー契約情報の場合、戻り値としてテストデータに対応するJSONObjectが返却される。
   *
   * @throws RablockSystemException
   * @throws JSONException
   */
  @Test
  public void testCheckContractAgree_case1() throws RablockSystemException {
    // テストデータ
    String testData =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"agreeId\":\"agr001\",\"startDate\":\"2018/11/18 00:00:00\",\"endDate\":\"2050/11/30 23:59:59\"}";

    // テスト
    JsonNode result = target.checkContractAgree(testData);

    // 検証
    assertEquals("new", result.get("type").asText());
    assertEquals("agree", result.get("contract").asText());
    assertEquals("user001", result.get("user").asText());
    assertEquals("00001", result.get("number").asText());
    assertEquals("agr001", result.get("agreeId").asText());
    assertEquals("2018/11/18 00:00:00", result.get("startDate").asText());
    assertEquals("2050/11/30 23:59:59", result.get("endDate").asText());
  }

  /**
   * [ケース２：異常ケース] JSON形式でないデータの場合、戻り値としてNULLが返却される。
   *
   * @throws RablockSystemException
   */
  @Test
  public void testCheckContractAgree_case2() throws RablockSystemException {
    // テストデータ
    String testData = "test";

    // テスト
    JsonNode result = target.checkContractAgree(testData);

    // 検証
    assertNull(result);
  }

  /**
   * [ケース３：異常ケース] データ種別(type)が存在しないデータの場合、戻り値としてNULLが返却される。
   *
   * @throws RablockSystemException
   */
  @Test
  public void testCheckContractAgree_case3() throws RablockSystemException {
    // テストデータ
    String testData =
        "{\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"startDate\":\"2018/11/18 00:00:00\",\"endDate\":\"2018/11/30 23:59:59\"}";

    // テスト
    JsonNode result = target.checkContractAgree(testData);

    // 検証
    assertNull(result);
  }

  /**
   * [ケース４：異常ケース] ユーザー(user)が存在しないデータの場合、戻り値としてNULLが返却される。
   *
   * @throws RablockSystemException
   */
  @Test
  public void testCheckContractAgree_case4() throws RablockSystemException {
    // テストデータ
    String testData =
        "{\"type\":\"new\",\"contract\":\"agree\","
            + "\"number\":\"00001\",\"startDate\":\"2018/11/18 00:00:00\",\"endDate\":\"2018/11/30 23:59:59\"}";

    // テスト
    JsonNode result = target.checkContractAgree(testData);

    // 検証
    assertNull(result);
  }

  /**
   * [ケース５：異常ケース] 契約番号(number)が存在しないデータの場合、戻り値としてNULLが返却される。
   *
   * @throws RablockSystemException
   */
  @Test
  public void testCheckContractAgree_case5() throws RablockSystemException {
    // テストデータ
    String testData =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"startDate\":\"2018/11/18 00:00:00\",\"endDate\":\"2018/11/30 23:59:59\"}";

    // テスト
    JsonNode result = target.checkContractAgree(testData);

    // 検証
    assertNull(result);
  }

  /**
   * [ケース６：異常ケース] 開始日時・終了日時の日付形式が不正の場合、戻り値としてNULLが返却される。
   *
   * @throws RablockSystemException
   */
  @Test
  public void testCheckContractAgree_case6() throws RablockSystemException {
    // テストデータ
    String testData =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"startDate\":\"20181118 00:00:00\",\"endDate\":\"20181130 23:59:59\"}";

    // テスト
    JsonNode result = target.checkContractAgree(testData);

    // 検証
    assertNull(result);
  }

  /**
   * [ケース７：異常ケース] 終了日時が開始日時より過去の場合、戻り値としてNULLが返却される。
   *
   * @throws RablockSystemException
   */
  @Test
  public void testCheckContractAgree_case7() throws RablockSystemException {
    // テストデータ
    String testData =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"startDate\":\"2018/11/18 00:00:00\",\"endDate\":\"2018/10/30 23:59:59\"}";

    // テスト
    JsonNode result = target.checkContractAgree(testData);

    // 検証
    assertNull(result);
  }

  /**
   * [ケース８：正常ケース] 正常なユーザー契約情報の変更の場合、戻り値としてテストデータに対応するJSONObjectが返却される。
   *
   * @throws RablockSystemException
   * @throws JSONException
   */
  @Test
  public void testCheckContractAgree_case8() throws JSONException, RablockSystemException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テスト前にユーザー契約のデータを登録、そして変更しておく
    String testDataPre =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"agreeId\":\"arg001\",\"startDate\":\"2018/11/18 00:00:00\",\"endDate\":\"2018/11/30 23:59:59\"}";
    poolService.setPool(new JSONObject(testDataPre), "2018/11/16 12:00:00");
    List<Document> poolList = poolService.getAllPool();
    String oid = test.getOid(poolList.get(0));

    // テストデータ
    String testData =
        "{\"type\":\"modify\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"agreeId\":\"arg001\",\"startDate\":\"2018/11/18 00:00:00\",\"endDate\":\"2050/11/30 23:59:59\",\"original_id\":\""
            + oid
            + "\"}";

    // テスト
    JsonNode result = target.checkContractAgree(testData);

    // 検証
    assertEquals("modify", result.get("type").asText());
    assertEquals("agree", result.get("contract").asText());
    assertEquals("user001", result.get("user").asText());
    assertEquals("00001", result.get("number").asText());
    assertEquals("arg001", result.get("agreeId").asText());
    assertEquals("2018/11/18 00:00:00", result.get("startDate").asText());
    assertEquals("2050/11/30 23:59:59", result.get("endDate").asText());
  }

  /**
   * [ケース９：異常ケース] すでに変更されたユーザー契約情報の変更の場合、戻り値としてNULLが返却される。
   *
   * @throws JSONException
   * @throws RablockSystemException
   */
  @Test
  public void testCheckContractAgree_case9() throws JSONException, RablockSystemException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testDataPre =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"agreeId\":\"arg001\",\"startDate\":\"2018/11/18 00:00:00\",\"endDate\":\"2018/11/30 23:59:59\"}";
    String testDataMod =
        "{\"type\":\"modify\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"agreeId\":\"arg001\",\"startDate\":\"2018/11/18 00:00:00\",\"endDate\":\"2050/11/30 23:59:59\"}";
    String testDataAfter =
        "{\"type\":\"modify\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"agreeId\":\"arg001\",\"startDate\":\"2018/11/18 00:00:00\",\"endDate\":\"2050/11/30 23:59:59\"}";

    // テスト前に契約定義のデータを登録、そして変更しておく
    poolService.setPool(new JSONObject(testDataPre), "2018/11/16 12:00:00");
    List<Document> poolList = poolService.getAllPool();
    String oid = test.getOid(poolList.get(0));
    JSONObject mod = new JSONObject(testDataMod);
    mod.put("original_id", oid);
    poolService.setPool(mod, "2018/11/16 12:01:00");

    // テスト
    JSONObject after = new JSONObject(testDataAfter);
    after.put("original_id", oid);
    JsonNode result = target.checkContractAgree(after.toString());

    // 検証
    assertNull(result);

    // 後処理
    test.poolDataDelete();
  }

  /** [ケース１：正常ケース] 正常な実行パラメータの場合、戻り値としてテストデータに対応するJSONObjectが返却される。 */
  @Test
  public void testCheckExecParam_case1() {
    // テストデータ
    String testData = "{\"oid\":\"5beaa851753c77214b1c60a5\",\"funcid\":\"lock\"}";

    // テスト
    JsonNode result = target.checkExecParam(testData);

    // 検証
    assertEquals("5beaa851753c77214b1c60a5", result.get("oid").asText());
    assertEquals("lock", result.get("funcid").asText());
  }

  /** [ケース２：異常ケース] オブジェクトIDが存在しない実行パラメータの場合、戻り値としてNULLが返却される。 */
  @Test
  public void testCheckExecParam_case2() {
    // テストデータ
    String testData = "{\"funcid\":\"lock\"}";

    // テスト
    JsonNode result = target.checkExecParam(testData);

    // 検証
    assertNull(result);
  }

  /** [ケース３：異常ケース] オペレーションIDが存在しない実行パラメータの場合、戻り値としてNULLが返却される。 */
  @Test
  public void testCheckExecParam_case3() {
    // テストデータ
    String testData = "{\"oid\":\"5beaa851753c77214b1c60a5\"}";

    // テスト
    JsonNode result = target.checkExecParam(testData);

    // 検証
    assertNull(result);
  }

  /** [ケース４：異常ケース] JSON形式でないデータの場合、戻り値としてNULLが返却される。 */
  @Test
  public void testCheckExecParam_case4() {
    // テストデータ
    String testData = "test";

    // テスト
    JsonNode result = target.checkExecParam(testData);

    // 検証
    assertNull(result);
  }

  /**
   * [ケース１：正常ケース] 指定されたオブジェクトIDに該当する 有効なユーザー契約情報がすでに登録されている場合、そのユーザー契約情報のJSONオブジェクトが返却される。
   *
   * @throws RablockSystemException
   */
  @Test
  public void testCheckExecAgree_case1() throws JSONException, RablockSystemException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testData =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"startDate\":\"2018/11/1 00:00:00\",\"endDate\":\"2050/11/30 23:59:59\"}";
    // テスト前にユーザー契約のデータを登録、および登録データのオブジェクトIDを取得
    poolService.setPool(new JSONObject(testData), "2018/11/16 12:00:00");
    List<Document> poolList = poolService.getAllPool();
    String oid = test.getOid(poolList.get(0));

    // テスト
    JsonNode result = target.checkExecAgree(oid);

    // 検証
    assertEquals("new", result.get("type").asText());
    assertEquals("agree", result.get("contract").asText());
    assertEquals("user001", result.get("user").asText());
    assertEquals("00001", result.get("number").asText());
    assertEquals("2018/11/1 00:00:00", result.get("startDate").asText());
    assertEquals("2050/11/30 23:59:59", result.get("endDate").asText());

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース２：異常ケース] 指定されたオブジェクトIDに該当する有効なユーザー契約情報が登録されていない場合、NULLが返却される。
   *
   * @throws RablockSystemException
   */
  @Test
  public void testCheckExecAgree_case2() throws RablockSystemException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テスト
    JsonNode result = target.checkExecAgree("5beaa851753c77214b1c60a5");

    // 検証
    assertNull(result);

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース３：異常ケース] 指定されたオブジェクトIDに該当する情報がユーザー契約情報でない場合（契約種別が"agree"でない）、NULLが返却される。
   *
   * @throws RablockSystemException
   */
  @Test
  public void testCheckExecAgree_case3() throws JSONException, RablockSystemException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testData =
        "{type:\"new\",contract:\"define\",number:\"00001\",name:\"スマートコントラクト\","
            + "functions:[{funcid:\"lock\",funcname:\"ロック\",funcurl:\"http://www.yahoo.co.jp\"},"
            + "{funcid:\"unlock\",funcname:\"アンロック\",funcurl:\"http://www.google.com\"}]}";
    // テスト前にユーザー契約のデータを登録、および登録データのオブジェクトIDを取得
    poolService.setPool(new JSONObject(testData), "2018/11/16 12:00:00");
    List<Document> poolList = poolService.getAllPool();
    String oid = test.getOid(poolList.get(0));

    // テスト
    JsonNode result = target.checkExecAgree(oid);

    // 検証
    assertNull(result);

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース４：異常ケース] 指定されたオブジェクトIDに該当するユーザー契約情報で、 契約開始日時、または契約終了日時の日付形式が不正の場合、NULLが返却される。
   *
   * @throws RablockSystemException
   */
  @Test
  public void testCheckExecAgree_case4() throws JSONException, RablockSystemException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testData =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"startDate\":\"2018/11/8\",\"endDate\":\"20181130 23:59:59\"}";
    // テスト前にユーザー契約のデータを登録、および登録データのオブジェクトIDを取得
    poolService.setPool(new JSONObject(testData), "2018/11/16 12:00:00");
    List<Document> poolList = poolService.getAllPool();
    String oid = test.getOid(poolList.get(0));

    // テスト
    JsonNode result = target.checkExecAgree(oid);

    // 検証
    assertNull(result);

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース５：異常ケース] 指定されたオブジェクトIDに該当するユーザー契約情報で、 現在日時が契約開始日以前の場合、NULLが返却される。
   *
   * @throws RablockSystemException
   */
  @Test
  public void testCheckExecAgree_case5() throws JSONException, RablockSystemException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testData =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"startDate\":\"2050/11/18 00:00:00\",\"endDate\":\"2050/11/30 23:59:59\"}";
    // テスト前にユーザー契約のデータを登録、および登録データのオブジェクトIDを取得
    poolService.setPool(new JSONObject(testData), "2018/11/16 12:00:00");
    List<Document> poolList = poolService.getAllPool();
    String oid = test.getOid(poolList.get(0));

    // テスト
    JsonNode result = target.checkExecAgree(oid);

    // 検証
    assertNull(result);

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース６：異常ケース] 指定されたオブジェクトIDに該当するユーザー契約情報で、 現在日時が契約終了日以降の場合、NULLが返却される。
   *
   * @throws RablockSystemException
   */
  @Test
  public void testCheckExecAgree_case6() throws JSONException, RablockSystemException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testData =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"startDate\":\"2018/10/18 00:00:00\",\"endDate\":\"2018/10/30 23:59:59\"}";
    // テスト前にユーザー契約のデータを登録、および登録データのオブジェクトIDを取得
    poolService.setPool(new JSONObject(testData), "2018/11/16 12:00:00");
    List<Document> poolList = poolService.getAllPool();
    String oid = test.getOid(poolList.get(0));

    // テスト
    JsonNode result = target.checkExecAgree(oid);

    // 検証
    assertNull(result);

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース７：正常ケース] 指定されたオブジェクトIDに該当する
   * 有効なユーザー契約情報(契約開始日時の指定なし)がすでに登録されている場合、そのユーザー契約情報のJSONオブジェクトが返却される。
   *
   * @throws RablockSystemException
   */
  @Test
  public void testCheckExecAgree_case7() throws JSONException, RablockSystemException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testData =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"startDate\":\"\",\"endDate\":\"2050/11/30 23:59:59\"}";
    // テスト前にユーザー契約のデータを登録、および登録データのオブジェクトIDを取得
    poolService.setPool(new JSONObject(testData), "2018/11/16 12:00:00");
    List<Document> poolList = poolService.getAllPool();
    String oid = test.getOid(poolList.get(0));

    // テスト
    JsonNode result = target.checkExecAgree(oid);

    // 検証
    assertEquals("new", result.get("type").asText());
    assertEquals("agree", result.get("contract").asText());
    assertEquals("user001", result.get("user").asText());
    assertEquals("00001", result.get("number").asText());
    assertEquals("", result.get("startDate").asText());
    assertEquals("2050/11/30 23:59:59", result.get("endDate").asText());

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース８：正常ケース] 指定されたオブジェクトIDに該当する
   * 有効なユーザー契約情報(契約終了日時の指定なし)がすでに登録されている場合、そのユーザー契約情報のJSONオブジェクトが返却される。
   *
   * @throws RablockSystemException
   */
  @Test
  public void testCheckExecAgree_case8() throws JSONException, RablockSystemException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testData =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"startDate\":\"2018/10/18 00:00:00\",\"endDate\":\"\"}";
    // テスト前にユーザー契約のデータを登録、および登録データのオブジェクトIDを取得
    poolService.setPool(new JSONObject(testData), "2018/11/16 12:00:00");
    List<Document> poolList = poolService.getAllPool();
    String oid = test.getOid(poolList.get(0));

    // テスト
    JsonNode result = target.checkExecAgree(oid);

    // 検証
    assertEquals("new", result.get("type").asText());
    assertEquals("agree", result.get("contract").asText());
    assertEquals("user001", result.get("user").asText());
    assertEquals("00001", result.get("number").asText());
    assertEquals("2018/10/18 00:00:00", result.get("startDate").asText());
    assertEquals("", result.get("endDate").asText());

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース９：正常ケース] 指定されたオブジェクトIDに該当する
   * 有効なユーザー契約情報(契約開始・終了日時の指定なし)がすでに登録されている場合、そのユーザー契約情報のJSONオブジェクトが返却される。
   *
   * @throws RablockSystemException
   */
  @Test
  public void testCheckExecAgree_case9() throws JSONException, RablockSystemException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testData =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"startDate\":\"\",\"endDate\":\"\"}";
    // テスト前にユーザー契約のデータを登録、および登録データのオブジェクトIDを取得
    poolService.setPool(new JSONObject(testData), "2018/11/16 12:00:00");
    List<Document> poolList = poolService.getAllPool();
    String oid = test.getOid(poolList.get(0));

    // テスト
    JsonNode result = target.checkExecAgree(oid);

    // 検証
    assertEquals("new", result.get("type").asText());
    assertEquals("agree", result.get("contract").asText());
    assertEquals("user001", result.get("user").asText());
    assertEquals("00001", result.get("number").asText());
    assertEquals("", result.get("startDate").asText());
    assertEquals("", result.get("endDate").asText());

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース１：正常ケース] 指定された契約番号に該当する有効な契約定義情報がすでに登録され、
   * 指定されたオペレーションIDに該当するオペレーション定義がある場合、その契約定義情報のJSONオブジェクトが返却される。
   *
   * @throws RablockSystemException
   */
  @Test
  public void testCheckExecDefine_case1() throws JSONException, RablockSystemException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testData =
        "{type:\"new\",contract:\"define\",number:\"00001\",name:\"スマートコントラクト\","
            + "functions:[{funcid:\"lock\",funcname:\"ロック\",funcurl:\"http://www.yahoo.co.jp\"},"
            + "{funcid:\"unlock\",funcname:\"アンロック\",funcurl:\"http://www.google.com\"}]}";
    // テスト前に契約定義のデータを登録、および登録データのオブジェクトIDを取得
    poolService.setPool(new JSONObject(testData), "2018/11/16 12:00:00");

    // テスト
    JsonNode result =
        target.checkExecDefine("00001", "unlock").orElseThrow(AssertionFailedError::new);

    // 検証
    assertEquals("new", result.get("type").asText());
    assertEquals("define", result.get("contract").asText());
    assertEquals("00001", result.get("number").asText());
    assertEquals("スマートコントラクト", result.get("name").asText());
    JsonNode functions = result.get("functions");
    JsonNode func1 = functions.get(0);
    assertEquals("lock", func1.get("funcid").asText());
    assertEquals("ロック", func1.get("funcname").asText());
    assertEquals("http://www.yahoo.co.jp", func1.get("funcurl").asText());
    JsonNode func2 = functions.get(1);
    assertEquals("unlock", func2.get("funcid").asText());
    assertEquals("アンロック", func2.get("funcname").asText());
    assertEquals("http://www.google.com", func2.get("funcurl").asText());

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース２：異常ケース] 指定された契約番号に該当する契約定義情報が存在しない場合、NULLが返却される
   *
   * @throws RablockSystemException
   */
  @Test
  public void testCheckExecDefine_case2() throws JSONException, RablockSystemException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testData =
        "{type:\"new\",contract:\"define\",number:\"00001\",name:\"スマートコントラクト\","
            + "functions:[{funcid:\"lock\",funcname:\"ロック\",funcurl:\"http://www.yahoo.co.jp\"},"
            + "{funcid:\"unlock\",funcname:\"アンロック\",funcurl:\"http://www.google.com\"}]}";
    // テスト前に契約定義のデータを登録、および登録データのオブジェクトIDを取得
    poolService.setPool(new JSONObject(testData), "2018/11/16 12:00:00");

    // テスト
    JsonNode result = target.checkExecDefine("99999", "unlock").orElse(null);

    // 検証
    assertNull(result);

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース３：異常ケース] 指定された契約番号に該当する有効な契約定義情報がすでに登録されているが、 指定されたオペレーションIDに該当するオペレーション定義がない場合、NULLが返却される。
   *
   * @throws RablockSystemException
   */
  @Test
  public void testCheckExecDefine_case3() throws JSONException, RablockSystemException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testData =
        "{type:\"new\",contract:\"define\",number:\"00001\",name:\"スマートコントラクト\","
            + "functions:[{funcid:\"lock\",funcname:\"ロック\",funcurl:\"http://www.yahoo.co.jp\"},"
            + "{funcid:\"unlock\",funcname:\"アンロック\",funcurl:\"http://www.google.com\"}]}";
    // テスト前に契約定義のデータを登録、および登録データのオブジェクトIDを取得
    poolService.setPool(new JSONObject(testData), "2018/11/16 12:00:00");

    // テスト
    JsonNode result = target.checkExecDefine("00001", "status").orElse(null);

    // 検証
    assertNull(result);

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース１：正常ケース] 指定された契約番号に該当する契約定義情報がすでに登録されている場合、その契約定義情報のJSONオブジェクトが返却される。
   *
   * @throws RablockSystemException
   */
  @Test
  public void testGetDefineForNumber_case1() throws JSONException, RablockSystemException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testData1 =
        "{type:\"new\",contract:\"define\",number:\"00001\",name:\"スマートコントラクト\","
            + "functions:[{funcid:\"lock\",funcname:\"ロック\",funcurl:\"http://www.yahoo.co.jp\"},"
            + "{funcid:\"unlock\",funcname:\"アンロック\",funcurl:\"http://www.google.com\"}]}";
    String testData2 =
        "{type:\"new\",contract:\"define\",number:\"abcde\",name:\"スマートコントラクト\","
            + "functions:[{funcid:\"lock\",funcname:\"ロック\",funcurl:\"http://www.yahoo.co.jp\"},"
            + "{funcid:\"unlock\",funcname:\"アンロック\",funcurl:\"http://www.google.com\"}]}";
    // テスト前に契約定義のデータを登録、および登録データのオブジェクトIDを取得
    poolService.setPool(new JSONObject(testData1), "2018/11/16 12:00:00");
    poolService.setPool(new JSONObject(testData2), "2018/11/16 12:00:00");

    // テスト
    JsonNode result = target.getDefineForNumber("abcde").orElseThrow(AssertionFailedError::new);

    // 検証
    assertEquals("new", result.get("type").asText());
    assertEquals("define", result.get("contract").asText());
    assertEquals("abcde", result.get("number").asText());
    assertEquals("スマートコントラクト", result.get("name").asText());
    JsonNode functions = result.get("functions");
    JsonNode func1 = functions.get(0);
    assertEquals("lock", func1.get("funcid").asText());
    assertEquals("ロック", func1.get("funcname").asText());
    assertEquals("http://www.yahoo.co.jp", func1.get("funcurl").asText());
    JsonNode func2 = functions.get(1);
    assertEquals("unlock", func2.get("funcid").asText());
    assertEquals("アンロック", func2.get("funcname").asText());
    assertEquals("http://www.google.com", func2.get("funcurl").asText());

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース２：正常ケース] 指定された契約番号に該当する契約定義情報が登録されていない場合、NULLが返却される。
   *
   * @throws RablockSystemException
   */
  @Test
  public void testGetDefineForNumber_case2() throws JSONException, RablockSystemException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testData1 =
        "{type:\"new\",contract:\"define\",number:\"00001\",name:\"スマートコントラクト\","
            + "functions:[{funcid:\"lock\",funcname:\"ロック\",funcurl:\"http://www.yahoo.co.jp\"},"
            + "{funcid:\"unlock\",funcname:\"アンロック\",funcurl:\"http://www.google.com\"}]}";
    String testData2 =
        "{type:\"new\",contract:\"define\",number:\"abcde\",name:\"スマートコントラクト\","
            + "functions:[{funcid:\"lock\",funcname:\"ロック\",funcurl:\"http://www.yahoo.co.jp\"},"
            + "{funcid:\"unlock\",funcname:\"アンロック\",funcurl:\"http://www.google.com\"}]}";
    // テスト前に契約定義のデータを登録、および登録データのオブジェクトIDを取得
    poolService.setPool(new JSONObject(testData1), "2018/11/16 12:00:00");
    poolService.setPool(new JSONObject(testData2), "2018/11/16 12:00:00");

    // テスト
    JsonNode result = target.getDefineForNumber("99999").orElse(null);

    // 検証
    assertNull(result);

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース２：正常ケース] 契約番号にNULLを指定した場合、戻り値としてNULLが返却される。
   *
   * @throws RablockSystemException
   */
  @Test
  public void testGetDefineForNumber_case3() throws JSONException, RablockSystemException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testData1 =
        "{type:\"new\",contract:\"define\",number:\"00001\",name:\"スマートコントラクト\","
            + "functions:[{funcid:\"lock\",funcname:\"ロック\",funcurl:\"http://www.yahoo.co.jp\"},"
            + "{funcid:\"unlock\",funcname:\"アンロック\",funcurl:\"http://www.google.com\"}]}";
    String testData2 =
        "{type:\"new\",contract:\"define\",number:\"abcde\",name:\"スマートコントラクト\","
            + "functions:[{funcid:\"lock\",funcname:\"ロック\",funcurl:\"http://www.yahoo.co.jp\"},"
            + "{funcid:\"unlock\",funcname:\"アンロック\",funcurl:\"http://www.google.com\"}]}";
    // テスト前に契約定義のデータを登録、および登録データのオブジェクトIDを取得
    poolService.setPool(new JSONObject(testData1), "2018/11/16 12:00:00");
    poolService.setPool(new JSONObject(testData2), "2018/11/16 12:00:00");

    // テスト
    JsonNode result = target.getDefineForNumber(null).orElse(null);

    // 検証
    assertNull(result);

    // 後処理
    test.poolDataDelete();
  }

  //    /**
  //     * [ケース１：正常ケース]
  //     * 有効な契約定義情報、ユーザー契約情報および実行対象のオペレーションIDを指定した場合、その実行結果のJSONObjectが返却される。
  //     * 本テストは、スタブとして定義している自身の"/contract/test"にアクセスするため、サーバーを起動後に実行すること。
  //     */
  //    @Test
  //    public void testExecContract_case1() throws JSONException {
  //        // 前処理
  //        test.blockDataDelete();
  //        test.poolDataDelete();
  //
  //        // テストデータ
  //        String testDefine =
  // "{\"type\":\"new\",\"contract\":\"define\",\"number\":\"00001\",\"name\":\"スマートコントラクト\","
  //                +
  // "\"functions\":[{\"funcid\":\"lock\",\"funcname\":\"ロック\",\"funcurl\":\"http://localhost:9000/contract/test\"},"
  //                +
  // "{\"funcid\":\"unlock\",\"funcname\":\"アンロック\",\"funcurl\":\"http://localhost:9000/contract/test2\"}]}";
  //        String testAgree = "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
  //                + "\"number\":\"00001\",\"startDate\":\"2018/11/1
  // 00:00:00\",\"endDate\":\"2050/11/30 23:59:59\"}";
  //        String param = "{\"oid\" : \"5c08ce43014cc5241e947450\",\"funcid\" : \"lock\"}";
  //
  //        try {
  //            // テスト
  //            ObjectMapper mapper = new ObjectMapper();
  //            String result = target.execContract(mapper.readTree(param),
  // mapper.readTree(testDefine), mapper.readTree(testAgree));
  //
  //            // 検証
  //            JsonNode res = mapper.readTree(result);
  //            assertEquals("OK", res.get("status").asText());
  //            assertEquals("test", res.get("message").asText());
  //            JsonNode info = res.get("info");
  //            assertEquals("1234567890", info.get("taskId").asText());
  //
  //            ObjectNode node = JsonNodeFactory.instance.objectNode();
  //            node.put(ConstItem.CONTRACT_TYPE, "result");
  //            List<DBObject> pool = poolService.getByKeyValue(ConstItem.CONTRACT_TYPE, node);
  //            DBObject dbo = pool.get(0);
  //            assertEquals("new", dbo.get("type"));
  //            assertEquals("result", dbo.get("contract"));
  //            assertEquals("user001", dbo.get("user"));
  //            assertEquals("00001", dbo.get("number"));
  //            assertEquals("スマートコントラクト", dbo.get("name"));
  //            assertEquals("lock", dbo.get("funcid"));
  //            assertEquals("ロック", dbo.get("funcname"));
  //            assertEquals("http://localhost:9000/contract/test", dbo.get("funcurl"));
  //            assertEquals("OK", dbo.get("result"));
  //            assertEquals("test", dbo.get("resultMsg"));
  //
  //        } catch (JsonProcessingException e) {
  //            e.printStackTrace();
  //        }
  //
  //        // 後処理
  //        test.poolDataDelete();
  //    }

  /**
   * [ケース２：異常ケース] 存在しないオペレーションIDを指定した場合、その実行結果のJSONObjectが返却される。
   *
   * @throws RablockSystemException
   * @throws IOException
   */
  @Test
  public void testExecContract_case2() throws JSONException, RablockSystemException, IOException {
    // テストデータ
    String testDefine =
        "{\"type\":\"new\",\"contract\":\"define\",\"number\":\"00001\",\"name\":\"スマートコントラクト\","
            + "\"functions\":[{\"funcid\":\"lock\",\"funcname\":\"ロック\",\"funcurl\":\"http://localhost:9000/contract/test\"},"
            + "{\"funcid\":\"unlock\",\"funcname\":\"アンロック\",\"funcurl\":\"http://localhost:9000/contract/test2\"}]}";
    String testAgree =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"agreeId\":\"arg001\",\"agreeName\":\"arg001\",\"startDate\":\"2018/11/1 00:00:00\",\"endDate\":\"2050/11/30 23:59:59\"}";
    String param = "{\"oid\" : \"5c08ce43014cc5241e947450\",\"funcid\" : \"nofunction\"}";

    // テスト
    ObjectMapper mapper = new ObjectMapper();
    String result =
        target.execContract(
            mapper.readTree(param), mapper.readTree(testDefine), mapper.readTree(testAgree));

    // 検証
    JsonNode res = mapper.readTree(result);
    assertEquals("NG", res.get("status").asText());
    assertEquals("オペレーションID：nofunctionに該当するオペレーションが存在しません。", res.get("message").asText());
  }

  //    /**
  //     * [ケース３：異常ケース]
  //     * 実行対象のURLにアクセスし404エラーとなった場合、その実行結果のJSONObjectが返却される。
  //     * 本テストは、サーバーを起動後に実行すること。
  //     */
  //    @Test
  //    public void testExecContract_case3() throws JSONException {
  //        // テストデータ
  //        String testDefine =
  // "{\"type\":\"new\",\"contract\":\"define\",\"number\":\"00001\",\"name\":\"スマートコントラクト\","
  //                +
  // "\"functions\":[{\"funcid\":\"lock\",\"funcname\":\"ロック\",\"funcurl\":\"http://localhost:9000/contract/test\"},"
  //                +
  // "{\"funcid\":\"unlock\",\"funcname\":\"アンロック\",\"funcurl\":\"http://localhost:9000/contract/test2\"}]}";
  //        String testAgree = "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
  //                + "\"number\":\"00001\",\"startDate\":\"2018/11/1
  // 00:00:00\",\"endDate\":\"2050/11/30 23:59:59\"}";
  //        String param = "{\"oid\" : \"5c08ce43014cc5241e947450\",\"funcid\" : \"unlock\"}";
  //
  //        try {
  //            // テスト
  //            ObjectMapper mapper = new ObjectMapper();
  //            String result = target.execContract(mapper.readTree(param),
  // mapper.readTree(testDefine), mapper.readTree(testAgree));
  //
  //            // 検証
  //            JsonNode res = mapper.readTree(result);
  //            assertEquals("NG", res.get("status").asText());
  //            assertEquals("通信が失敗しました。HTTPステータスコード：404", res.get("message").asText());
  //
  //            ObjectNode node = JsonNodeFactory.instance.objectNode();
  //            node.put(ConstItem.CONTRACT_TYPE, "result");
  //            List<DBObject> pool = poolService.getByKeyValue(ConstItem.CONTRACT_TYPE, node);
  //            DBObject dbo = pool.get(0);
  //            assertEquals("new", dbo.get("type"));
  //            assertEquals("result", dbo.get("contract"));
  //            assertEquals("user001", dbo.get("user"));
  //            assertEquals("00001", dbo.get("number"));
  //            assertEquals("スマートコントラクト", dbo.get("name"));
  //            assertEquals("unlock", dbo.get("funcid"));
  //            assertEquals("アンロック", dbo.get("funcname"));
  //            assertEquals("http://localhost:9000/contract/test2", dbo.get("funcurl"));
  //            assertEquals("NG", dbo.get("result"));
  //            assertEquals("通信が失敗しました。HTTPステータスコード：404", dbo.get("resultMsg"));
  //
  //        } catch (JsonProcessingException e) {
  //            e.printStackTrace();
  //        }
  //    }

  /**
   * [ケース４：正常ケース] 予期せぬエラー（実行対象のURLにアクセスできない）となった場合、その実行結果のJSONObjectが返却される。
   *
   * @throws RablockSystemException
   * @throws IOException
   */
  @Test
  public void testExecContract_case4() throws JSONException, RablockSystemException, IOException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testDefine =
        "{\"type\":\"new\",\"contract\":\"define\",\"number\":\"00001\",\"name\":\"スマートコントラクト\","
            + "\"functions\":[{\"funcid\":\"lock\",\"funcname\":\"ロック\",\"funcurl\":\"http://test:9000/contract/test\"},"
            + "{\"funcid\":\"unlock\",\"funcname\":\"アンロック\",\"funcurl\":\"http://localhost:9000/contract/test2\"}]}";
    String testAgree =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"agreeId\":\"arg001\",\"agreeName\":\"テスト\",\"startDate\":\"2018/11/1 00:00:00\",\"endDate\":\"2050/11/30 23:59:59\"}";
    String param = "{\"oid\" : \"5c08ce43014cc5241e947450\",\"funcid\" : \"lock\"}";

    // テスト
    ObjectMapper mapper = new ObjectMapper();
    String result =
        target.execContract(
            mapper.readTree(param), mapper.readTree(testDefine), mapper.readTree(testAgree));

    // 検証
    JsonNode res = mapper.readTree(result);
    assertEquals("NG", res.get("status").asText());
    assertEquals("通信中に予期せぬエラーが発生しました。", res.get("message").asText());

    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put(ConstItem.CONTRACT_TYPE, "result");
    List<Document> pool = poolService.getByKeyValue(ConstItem.CONTRACT_TYPE, node);
    Document dbo = pool.get(0);
    assertEquals("new", dbo.get("type"));
    assertEquals("result", dbo.get("contract"));
    assertEquals("user001", dbo.get("user"));
    assertEquals("00001", dbo.get("number"));
    assertEquals("スマートコントラクト", dbo.get("name"));
    assertEquals("lock", dbo.get("funcid"));
    assertEquals("ロック", dbo.get("funcname"));
    assertEquals("http://test:9000/contract/test", dbo.get("funcurl"));
    assertEquals("NG", dbo.get("result"));
    assertEquals("通信中に予期せぬエラーが発生しました。", dbo.get("resultMsg"));

    // 後処理
    test.poolDataDelete();
  }
}

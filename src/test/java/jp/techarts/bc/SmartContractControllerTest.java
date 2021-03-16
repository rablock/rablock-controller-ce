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
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.List;
import jp.techarts.bc.constitem.ConstItem;
import jp.techarts.bc.test.SampleMethod;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * スマートコントラクト関連コントローラークラスのテストクラス<br>
 * Copyright (c) 2018 Rablock Inc.
 *
 * @author TA
 * @version 1.0
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class SmartContractControllerTest {

  /** テスト用メソッド */
  @Autowired private SampleMethod test;

  /** プールサービス */
  @Autowired private PoolService poolService;

  /** 【テスト対象】スマートコントラクトコントローラー */
  @Autowired private SmartContractController target;

  /**
   * [ケース１：正常ケース] 正常な契約定義情報の新規登録。
   *
   * @throws IOException
   * @throws RablockSystemException
   */
  @Test
  public void testDefine_case1() throws JSONException, IOException, RablockSystemException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testData =
        "{\"type\":\"new\",\"contract\":\"define\",\"number\":\"00001\",\"name\":\"スマートコントラクト\","
            + "\"functions\":[{\"funcid\":\"lock\",\"funcname\":\"ロック\",\"funcurl\":\"http://www.yahoo.co.jp\"},"
            + "{\"funcid\":\"unlock\",\"funcname\":\"アンロック\",\"funcurl\":\"http://www.google.com\"}]}";

    // テスト
    String result = target.define(testData);

    // 検証
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(result);
    assertEquals("OK", node.get("status").asText());
    List<Document> poolList = poolService.getAllPool();
    Document dbo = poolList.get(0);
    assertEquals("new", dbo.get("type"));
    assertEquals("define", dbo.get("contract"));
    assertEquals("00001", dbo.get("number"));
    assertEquals("スマートコントラクト", dbo.get("name"));
    List<Document> functions = dbo.getList("functions", Document.class);
    Document func1 = functions.get(0);
    assertEquals("lock", func1.get("funcid"));
    assertEquals("ロック", func1.get("funcname"));
    assertEquals("http://www.yahoo.co.jp", func1.get("funcurl"));
    Document func2 = functions.get(1);
    assertEquals("unlock", func2.get("funcid"));
    assertEquals("アンロック", func2.get("funcname"));
    assertEquals("http://www.google.com", func2.get("funcurl"));

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース２：正常ケース] 正常な契約定義情報の変更登録。
   *
   * @throws RablockSystemException
   * @throws IOException
   */
  @Test
  public void testDefine_case2() throws JSONException, RablockSystemException, IOException {
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
            + "\"functions\":[{\"funcid\":\"lock\",\"funcname\":\"ロック\",\"funcurl\":\"http://www.yahoo.co.jp\"}]}";

    // テスト前に契約定義のデータを登録
    poolService.setPool(new JSONObject(testDataPre), "2018/11/16 12:00:00");
    // 登録したデータのオブジェクトID取得
    List<Document> poolList = poolService.getAllPool();
    String oid = test.getOid(poolList.get(0));
    // 登録対象のデータにoriginal_idとして上記で取得したオブジェクトIDを設定しておく
    JSONObject after = new JSONObject(testDataAfter);
    after.put("original_id", oid);

    // テスト
    String result = target.define(after.toString());

    // 検証
    ObjectMapper mapper = new ObjectMapper();
    JsonNode res = mapper.readTree(result);
    assertEquals("OK", res.get("status").asText());
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put(ConstItem.CONTRACT_NUMBER, "00001");
    List<Document> pool = poolService.getByKeyValue(ConstItem.CONTRACT_NUMBER, node);
    Document dbo = pool.get(0);
    assertEquals("modify", dbo.get("type"));
    assertEquals("define", dbo.get("contract"));
    assertEquals("00001", dbo.get("number"));
    assertEquals("スマートコントラクト", dbo.get("name"));
    List<Document> functions = dbo.getList("functions", Document.class);
    Document func1 = functions.get(0);
    assertEquals("lock", func1.get("funcid"));
    assertEquals("ロック", func1.get("funcname"));
    assertEquals("http://www.yahoo.co.jp", func1.get("funcurl"));

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース３：正常ケース] 正常な契約定義情報の削除登録。
   *
   * @throws RablockSystemException
   * @throws IOException
   */
  @Test
  public void testDefine_case3() throws JSONException, RablockSystemException, IOException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testDataPre =
        "{\"type\":\"new\",\"contract\":\"define\",\"number\":\"00001\",\"name\":\"スマートコントラクト\","
            + "\"functions\":[{\"funcid\":\"lock\",\"funcname\":\"ロック\",\"funcurl\":\"http://www.yahoo.co.jp\"},"
            + "{\"funcid\":\"unlock\",\"funcname\":\"アンロック\",\"funcurl\":\"http://www.google.com\"}]}";
    String testDataAfter =
        "{\"type\":\"delete\",\"contract\":\"define\",\"number\":\"00001\",\"name\":\"スマートコントラクト\","
            + "\"functions\":[{\"funcid\":\"lock\",\"funcname\":\"ロック\",\"funcurl\":\"http://www.yahoo.co.jp\"},"
            + "{\"funcid\":\"unlock\",\"funcname\":\"アンロック\",\"funcurl\":\"http://www.google.com\"}]}";

    // テスト前に契約定義のデータを登録
    poolService.setPool(new JSONObject(testDataPre), "2018/11/16 12:00:00");
    // 登録したデータのオブジェクトID取得
    List<Document> poolList = poolService.getAllPool();
    String oid = test.getOid(poolList.get(0));
    // 登録対象のデータにoriginal_idとして上記で取得したオブジェクトIDを設定しておく
    JSONObject after = new JSONObject(testDataAfter);
    after.put("original_id", oid);

    // テスト
    String result = target.define(after.toString());

    // 検証
    ObjectMapper mapper = new ObjectMapper();
    JsonNode res = mapper.readTree(result);
    assertEquals("OK", res.get("status").asText());
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put(ConstItem.CONTRACT_NUMBER, "00001");
    List<Document> pool = poolService.getByKeyValue(ConstItem.CONTRACT_NUMBER, node);
    assertTrue(pool.isEmpty());

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース１：正常ケース] 正常なユーザー契約情報の新規登録。
   *
   * @throws RablockSystemException
   * @throws IOException
   */
  @Test
  public void testAgree_case1() throws JSONException, RablockSystemException, IOException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testData =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"agreeId\":\"arg001\",\"agreeName\":\"テスト\",\"startDate\":\"2018/11/18 00:00:00\",\"endDate\":\"2050/11/30 23:59:59\"}";

    // テスト
    String result = target.agree(testData);

    // 検証
    ObjectMapper mapper = new ObjectMapper();
    JsonNode res = mapper.readTree(result);
    assertEquals("OK", res.get("status").asText());
    List<Document> poolList = poolService.getAllPool();
    Document dbo = poolList.get(0);
    assertEquals("new", dbo.get("type"));
    assertEquals("agree", dbo.get("contract"));
    assertEquals("user001", dbo.get("user"));
    assertEquals("00001", dbo.get("number"));
    assertEquals("arg001", dbo.get("agreeId"));
    assertEquals("テスト", dbo.get("agreeName"));
    assertEquals("2018/11/18 00:00:00", dbo.get("startDate"));
    assertEquals("2050/11/30 23:59:59", dbo.get("endDate"));

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース２：正常ケース] 正常なユーザー契約情報の変更登録。
   *
   * @throws IOException
   * @throws RablockSystemException
   */
  @Test
  public void testAgree_case2() throws JSONException, IOException, RablockSystemException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testDataPre =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"agreeId\":\"arg001\",\"agreeName\":\"テスト\",\"startDate\":\"2018/11/18 00:00:00\",\"endDate\":\"2050/11/30 23:59:59\"}";
    // テストデータ
    String testDataAfter =
        "{\"type\":\"modify\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"agreeId\":\"arg001\",\"agreeName\":\"テスト1\",\"startDate\":\"2018/11/10 00:00:00\",\"endDate\":\"2050/11/20 23:59:59\"}";

    // テスト前に契約定義のデータを登録
    poolService.setPool(new JSONObject(testDataPre), "2018/11/16 12:00:00");
    // 登録したデータのオブジェクトID取得
    List<Document> poolList = poolService.getAllPool();
    String oid = test.getOid(poolList.get(0));
    // 登録対象のデータにoriginal_idとして上記で取得したオブジェクトIDを設定しておく
    JSONObject after = new JSONObject(testDataAfter);
    after.put("original_id", oid);

    // テスト
    String result = target.agree(after.toString());

    // 検証
    ObjectMapper mapper = new ObjectMapper();
    JsonNode res = mapper.readTree(result);
    assertEquals("OK", res.get("status").asText());
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put(ConstItem.CONTRACT_TYPE, "agree");
    List<Document> pool = poolService.getByKeyValue(ConstItem.CONTRACT_TYPE, node);
    Document dbo = pool.get(0);
    assertEquals("modify", dbo.get("type"));
    assertEquals("agree", dbo.get("contract"));
    assertEquals("user001", dbo.get("user"));
    assertEquals("00001", dbo.get("number"));
    assertEquals("arg001", dbo.get("agreeId"));
    assertEquals("テスト1", dbo.get("agreeName"));
    assertEquals("2018/11/10 00:00:00", dbo.get("startDate"));
    assertEquals("2050/11/20 23:59:59", dbo.get("endDate"));

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース３：正常ケース] 正常なユーザー契約情報の削除登録。
   *
   * @throws IOException
   * @throws RablockSystemException
   */
  @Test
  public void testAgree_case3() throws JSONException, IOException, RablockSystemException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testDataPre =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"agreeId\":\"arg001\",\"agreeName\":\"テスト\",\"startDate\":\"2018/11/18 00:00:00\",\"endDate\":\"2050/11/30 23:59:59\"}";
    String testDataAfter =
        "{\"type\":\"delete\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"agreeId\":\"arg001\",\"agreeName\":\"テスト\",\"startDate\":\"2018/11/18 00:00:00\",\"endDate\":\"2050/11/30 23:59:59\"}";

    // テスト前に契約定義のデータを登録
    poolService.setPool(new JSONObject(testDataPre), "2018/11/16 12:00:00");
    // 登録したデータのオブジェクトID取得
    List<Document> poolList = poolService.getAllPool();
    String oid = test.getOid(poolList.get(0));
    // 登録対象のデータにoriginal_idとして上記で取得したオブジェクトIDを設定しておく
    JSONObject after = new JSONObject(testDataAfter);
    after.put("original_id", oid);

    // テスト
    String result = target.agree(after.toString());

    // 検証
    ObjectMapper mapper = new ObjectMapper();
    JsonNode res = mapper.readTree(result);
    assertEquals("OK", res.get("status").asText());
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put(ConstItem.CONTRACT_TYPE, "agree");
    List<Document> pool = poolService.getByKeyValue(ConstItem.CONTRACT_TYPE, node);
    assertTrue(pool.isEmpty());

    // 後処理
    test.poolDataDelete();
  }

  //    /**
  //     * [ケース１：正常ケース]
  //     * 正常な契約実行。
  //     * 本テストは、スタブとして定義している自身の"/contract/test"にアクセスするため、サーバーを起動後に実行すること。
  //     */
  //    @Test
  //    public void testExecute_case1() throws JSONException {
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
  //
  //        // テスト前にデータを登録
  //        poolService.setPool(new JSONObject(testDefine), "2018/11/16 12:00:00");
  //        poolService.setPool(new JSONObject(testAgree), "2018/11/16 12:00:00");
  //        ObjectNode node = JsonNodeFactory.instance.objectNode();
  //        node.put(ConstItem.CONTRACT_TYPE, "agree");
  //        List<String> pool = poolService.getByKeyValue(ConstItem.CONTRACT_TYPE, node);
  //        String oid = test.getOid(pool.get(0));
  //        String testExec = "{\"oid\":\"" + oid + "\",\"funcid\":\"lock\"}";
  //
  //        // テスト
  //        String result = target.execute(testExec);
  //
  //        // 検証
  //        try {
  //            ObjectMapper mapper = new ObjectMapper();
  //            JsonNode res = mapper.readTree(result);
  //            assertEquals("OK", res.get("status").asText());
  //            ObjectNode n = JsonNodeFactory.instance.objectNode();
  //            n.put(ConstItem.CONTRACT_TYPE, "result");
  //            List<Document> p = poolService.getByKeyValue(ConstItem.CONTRACT_TYPE, n);
  //            Document log = p.get(0);
  //
  //            assertEquals("new", log.get("type"));
  //            assertEquals("result", log.get("contract"));
  //            assertEquals("user001", log.get("user"));
  //            assertEquals("00001", log.get("number"));
  //            assertEquals("スマートコントラクト", log.get("name"));
  //            assertEquals("lock", log.get("funcid"));
  //            assertEquals("ロック", log.get("funcname"));
  //            assertEquals("http://localhost:9000/contract/test", log.get("funcurl"));
  //            assertEquals("OK", log.get("result"));
  //            assertEquals("test", log.get("resultMsg"));
  //        } catch (JsonProcessingException e) {
  //            e.printStackTrace();
  //        }
  //
  //        // 後処理
  //        test.poolDataDelete();
  //    }

  /**
   * [ケース１：正常ケース] 複数件の契約定義情報を取得。
   *
   * @throws RablockSystemException
   * @throws IOException
   */
  @Test
  public void testDefineList_case1() throws JSONException, RablockSystemException, IOException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testDefine1 =
        "{\"type\":\"new\",\"contract\":\"define\",\"number\":\"00001\",\"name\":\"スマートコントラクト\","
            + "\"functions\":[{\"funcid\":\"lock\",\"funcname\":\"ロック\",\"funcurl\":\"http://localhost:9000/contract/test\"}]}";
    String testDefine2 =
        "{\"type\":\"new\",\"contract\":\"define\",\"number\":\"00002\",\"name\":\"テスト\",\"functions\":[]}";

    // テスト前にデータを登録
    poolService.setPool(new JSONObject(testDefine1), "2018/11/16 12:00:00");
    poolService.setPool(new JSONObject(testDefine2), "2018/11/16 12:00:00");

    // テスト
    String result = target.defineList();

    // 検証
    ObjectMapper mapper = new ObjectMapper();
    JsonNode res = mapper.readTree(result);
    assertEquals("OK", res.get("status").asText());
    JsonNode list = res.get("info");
    assertEquals(2, list.size());

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース２：正常ケース] 契約定義情報が０件の場合。
   *
   * @throws IOException
   * @throws RablockSystemException
   */
  @Test
  public void testDefineList_case2() throws JSONException, IOException, RablockSystemException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テスト
    String result = target.defineList();

    // 検証
    ObjectMapper mapper = new ObjectMapper();
    JsonNode res = mapper.readTree(result);
    assertEquals("OK", res.get("status").asText());
    JsonNode list = res.get("info");
    assertEquals(0, list.size());
  }

  /**
   * [ケース１：正常ケース] 複数件のユーザー契約情報を取得。
   *
   * @throws RablockSystemException
   * @throws IOException
   */
  @Test
  public void testAgreeList_case1() throws JSONException, RablockSystemException, IOException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testAgree1 =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"startDate\":\"2018/11/1 00:00:00\",\"endDate\":\"2050/11/30 23:59:59\"}";
    String testAgree2 =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user002\","
            + "\"number\":\"00001\",\"startDate\":\"2018/11/1 00:00:00\",\"endDate\":\"2050/11/30 23:59:59\"}";
    String testAgree3 =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00002\",\"startDate\":\"2018/11/1 00:00:00\",\"endDate\":\"2050/11/30 23:59:59\"}";

    // テスト前にデータを登録
    poolService.setPool(new JSONObject(testAgree1), "2018/11/16 12:00:00");
    poolService.setPool(new JSONObject(testAgree2), "2018/11/16 12:00:00");
    poolService.setPool(new JSONObject(testAgree3), "2018/11/16 12:00:00");

    // テスト
    String user = "user001";
    String result = target.agreeList(user, null);

    // 検証
    ObjectMapper mapper = new ObjectMapper();
    JsonNode res = mapper.readTree(result);
    assertEquals("OK", res.get("status").asText());
    JsonNode list = res.get("info");
    assertEquals(2, list.size());

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース２：正常ケース] ユーザー契約情報が０件の場合。
   *
   * @throws RablockSystemException
   * @throws IOException
   */
  @Test
  public void testAgreeList_case2() throws JSONException, RablockSystemException, IOException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testAgree1 =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"startDate\":\"2018/11/1 00:00:00\",\"endDate\":\"2050/11/30 23:59:59\"}";
    String testAgree2 =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user002\","
            + "\"number\":\"00001\",\"startDate\":\"2018/11/1 00:00:00\",\"endDate\":\"2050/11/30 23:59:59\"}";
    String testAgree3 =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00002\",\"startDate\":\"2018/11/1 00:00:00\",\"endDate\":\"2050/11/30 23:59:59\"}";

    // テスト前にデータを登録
    poolService.setPool(new JSONObject(testAgree1), "2018/11/16 12:00:00");
    poolService.setPool(new JSONObject(testAgree2), "2018/11/16 12:00:00");
    poolService.setPool(new JSONObject(testAgree3), "2018/11/16 12:00:00");

    // テスト
    String user = "user001";
    String number = "00003";
    String result = target.agreeList(user, number);

    // 検証
    ObjectMapper mapper = new ObjectMapper();
    JsonNode res = mapper.readTree(result);
    assertEquals("OK", res.get("status").asText());
    JsonNode list = res.get("info");
    assertEquals(0, list.size());

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース１：正常ケース] ユーザー契約情報を取得。
   *
   * @throws RablockSystemException
   * @throws IOException
   */
  @Test
  public void testAgreeDetail_case1() throws JSONException, RablockSystemException, IOException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testAgree1 =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"startDate\":\"2018/11/1 00:00:00\",\"endDate\":\"2050/11/30 23:59:59\"}";

    // テスト前にデータを登録
    poolService.setPool(new JSONObject(testAgree1), "2018/11/16 12:00:00");
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put(ConstItem.CONTRACT_TYPE, "agree");
    List<Document> pool = poolService.getByKeyValue(ConstItem.CONTRACT_TYPE, node);
    String oid = test.getOid(pool.get(0));

    // テスト
    String result = target.agreeDetail(oid);

    // 検証
    ObjectMapper mapper = new ObjectMapper();
    JsonNode res = mapper.readTree(result);
    assertEquals("OK", res.get("status").asText());
    JsonNode detail = res.get("info");
    assertEquals("new", detail.get("type").asText());
    assertEquals("agree", detail.get("contract").asText());
    assertEquals("user001", detail.get("user").asText());
    assertEquals("00001", detail.get("number").asText());
    assertEquals("2018/11/1 00:00:00", detail.get("startDate").asText());
    assertEquals("2050/11/30 23:59:59", detail.get("endDate").asText());

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース２：正常ケース] ユーザー契約情報が取得できない。
   *
   * @throws RablockSystemException
   * @throws IOException
   */
  @Test
  public void testAgreeDetail_case2() throws JSONException, RablockSystemException, IOException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testAgree1 =
        "{\"type\":\"new\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"startDate\":\"2018/11/1 00:00:00\",\"endDate\":\"2050/11/30 23:59:59\"}";

    // テスト前にデータを登録
    poolService.setPool(new JSONObject(testAgree1), "2018/11/16 12:00:00");
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put(ConstItem.CONTRACT_TYPE, "agree");
    List<Document> pool = poolService.getAllPool();
    String oid = test.getOid(pool.get(0));

    // テスト
    String result = target.agreeDetail(oid);

    // 検証
    ObjectMapper mapper = new ObjectMapper();
    JsonNode res = mapper.readTree(result);
    assertEquals("OK", res.get("status").asText());
    JsonNode detail = res.get("info");
    assertEquals(0, detail.size());

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース１：正常ケース] 複数件の実行結果情報を取得。
   *
   * @throws RablockSystemException
   * @throws IOException
   */
  @Test
  public void testResultList_case1() throws JSONException, RablockSystemException, IOException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testResult1 =
        "{\"type\":\"new\",\"contract\":\"result\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"result\":\"OK\",\"resultMsg\":\"\"}";
    String testResult2 =
        "{\"type\":\"new\",\"contract\":\"result\",\"user\":\"user002\","
            + "\"number\":\"00001\",\"result\":\"NG\",\"resultMsg\":\"error\"}";
    String testResult3 =
        "{\"type\":\"new\",\"contract\":\"result\",\"user\":\"user001\","
            + "\"number\":\"00002\",\"result\":\"OK\",\"resultMsg\":\"\"}";

    // テスト前にデータを登録
    poolService.setPool(new JSONObject(testResult1), "2018/11/16 12:00:00");
    poolService.setPool(new JSONObject(testResult2), "2018/11/16 12:00:00");
    poolService.setPool(new JSONObject(testResult3), "2018/11/16 12:00:00");

    // テスト
    String user = "user001";
    String result = target.resultList(user, null);

    // 検証
    ObjectMapper mapper = new ObjectMapper();
    JsonNode res = mapper.readTree(result);
    assertEquals("OK", res.get("status").asText());
    JsonNode list = res.get("info");
    assertEquals(2, list.size());

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース２：正常ケース] 実行結果情報が０件の場合。
   *
   * @throws RablockSystemException
   * @throws IOException
   */
  @Test
  public void testResultList_case2() throws JSONException, RablockSystemException, IOException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testResult1 =
        "{\"type\":\"new\",\"contract\":\"result\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"result\":\"OK\",\"resultMsg\":\"\"}";
    String testResult2 =
        "{\"type\":\"new\",\"contract\":\"result\",\"user\":\"user002\","
            + "\"number\":\"00001\",\"result\":\"NG\",\"resultMsg\":\"error\"}";
    String testResult3 =
        "{\"type\":\"new\",\"contract\":\"result\",\"user\":\"user001\","
            + "\"number\":\"00002\",\"result\":\"OK\",\"resultMsg\":\"\"}";

    // テスト前にデータを登録
    poolService.setPool(new JSONObject(testResult1), "2018/11/16 12:00:00");
    poolService.setPool(new JSONObject(testResult2), "2018/11/16 12:00:00");
    poolService.setPool(new JSONObject(testResult3), "2018/11/16 12:00:00");

    // テスト
    String user = "user001";
    String number = "00003";
    String result = target.agreeList(user, number);

    // 検証
    ObjectMapper mapper = new ObjectMapper();
    JsonNode res = mapper.readTree(result);
    assertEquals("OK", res.get("status").asText());
    JsonNode list = res.get("info");
    assertEquals(0, list.size());

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース１：正常ケース] 実行結果情報を取得。
   *
   * @throws RablockSystemException
   * @throws IOException
   */
  @Test
  public void testResultDetail_case1() throws JSONException, RablockSystemException, IOException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testResult =
        "{\"type\":\"new\",\"contract\":\"result\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"result\":\"OK\",\"resultMsg\":\"\"}";

    // テスト前にデータを登録
    poolService.setPool(new JSONObject(testResult), "2018/11/16 12:00:00");
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put(ConstItem.CONTRACT_TYPE, "result");
    List<Document> pool = poolService.getByKeyValue(ConstItem.CONTRACT_TYPE, node);
    String oid = test.getOid(pool.get(0));

    // テスト
    String result = target.resultDetail(oid);

    // 検証
    ObjectMapper mapper = new ObjectMapper();
    JsonNode res = mapper.readTree(result);
    assertEquals("OK", res.get("status").asText());
    JsonNode detail = res.get("info");
    assertEquals("new", detail.get("type").asText());
    assertEquals("result", detail.get("contract").asText());
    assertEquals("user001", detail.get("user").asText());
    assertEquals("00001", detail.get("number").asText());
    assertEquals("OK", detail.get("result").asText());
    assertEquals("", detail.get("resultMsg").asText());

    // 後処理
    test.poolDataDelete();
  }

  /**
   * [ケース２：正常ケース] 実行結果情報が取得できない。
   *
   * @throws IOException
   * @throws RablockSystemException
   */
  @Test
  public void testResultDetail_case2() throws JSONException, IOException, RablockSystemException {
    // 前処理
    test.blockDataDelete();
    test.poolDataDelete();

    // テストデータ
    String testResult =
        "{\"type\":\"new\",\"contract\":\"agree\",\"user\":\"user001\","
            + "\"number\":\"00001\",\"result\":\"OK\",\"resultMsg\":\"\"}";

    // テスト前にデータを登録
    poolService.setPool(new JSONObject(testResult), "2018/11/16 12:00:00");
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put(ConstItem.CONTRACT_TYPE, "agree");
    List<Document> pool = poolService.getAllPool();
    String oid = test.getOid(pool.get(0));

    // テスト
    String result = target.resultDetail(oid);

    // 検証
    ObjectMapper mapper = new ObjectMapper();
    JsonNode res = mapper.readTree(result);
    assertEquals("OK", res.get("status").asText());
    JsonNode detail = res.get("info");
    assertEquals(0, detail.size());

    // 後処理
    test.poolDataDelete();
  }
}

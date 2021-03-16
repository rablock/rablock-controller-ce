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
import static org.junit.Assert.fail;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import jp.techarts.bc.constitem.ConstItem;
import jp.techarts.bc.test.SampleData;
import jp.techarts.bc.test.SampleMethod;
import jp.techarts.bc.test.SetUpData;
import org.bson.BsonArray;
import org.bson.BsonString;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GetControllerTest {

  @Autowired private GetController controller;
  @Autowired private SampleMethod test;
  @Autowired SetUpData set;

  @Before
  public void initialize()
      throws InterruptedException, NoSuchAlgorithmException, UnsupportedEncodingException,
          RablockSystemException {
    // DB削除
    Thread.sleep(250);
    test.blockDataDelete();
    Thread.sleep(250);
    test.poolDataDelete();
    Thread.sleep(250);

    set.createGenBlock();
    set.createTestPoolData();
  }

  @Test
  public void getJsonテスト_Json形式でない場合() throws IOException, RablockSystemException {
    try {
      controller.getJson("{\"int_item\": }");
      fail("Should be raised JsonParseException");
    } catch (JsonParseException e) {
      /* Should come here. */
    }
  }

  @Test
  public void getJsonテスト_データがpoolのみに存在_String型() throws IOException, RablockSystemException {
    String res_type = "";
    String res_user_id = "";
    String res_testItem = "";
    String res_oid = "";
    String res_1001 = controller.getJson("{\"user_id\":\"1001\"}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res_1001.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      JsonNode iternode = iter.next();
      JsonNode oidNode = iternode.get("_id");

      res_type = iternode.get("type").asText();
      res_user_id = iternode.get("user_id").asText();
      res_testItem = iternode.get("testItem1").asText();
      res_oid = oidNode.get("$oid").asText();
      count++;
    }

    List<Document> list = test.getAllPoolTestData();
    // 1001の最新データは２レコード目
    String oid = test.getOid(list.get(1));
    assertEquals(oid, res_oid);
    assertEquals("modify", res_type);
    assertEquals("1001", res_user_id);
    assertEquals("TEST", res_testItem);
    // 検索で取得できるのは最新の１件のみ
    assertEquals(1, count);
  }

  @Test
  public void getJsonテスト_データがblockのみに存在_String型() throws IOException, RablockSystemException {
    set.createTestBlockData();
    String res_6001 = controller.getJson("{\"user_id\":\"6001\"}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res_6001.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(1, count);
  }

  @Test
  public void getJsonテスト_データがpool_block両方に存在_String型() throws IOException, RablockSystemException {
    set.createTestBlockData();
    String res_5001 = controller.getJson("{\"user_id\":\"5001\"}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res_5001.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(2, count);
  }

  @Test
  public void getJsonテスト_データが存在しない場合_String型() throws IOException, RablockSystemException {
    String res = controller.getJson("{\"user_id\":\"10010\"}").replace(" ", "");
    assertEquals("[]", res);
  }

  @Test
  public void getJsonテスト_データがpoolのみに存在_Int型() throws IOException, RablockSystemException {
    set.createTestPoolDataForGetJson();
    String res = controller.getJson("{\"int_item\": 123}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(1, count);
  }

  @Test
  public void getJsonテスト_データがblockのみに存在_Int型() throws IOException, RablockSystemException {
    set.createTestBlockDataForGetJson();
    String res = controller.getJson("{\"int_item\": 123}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(1, count);
  }

  @Test
  public void getJsonテスト_データがpool_block両方に存在_Int型() throws IOException, RablockSystemException {
    set.createTestBlockDataForGetJson();
    set.createTestPoolDataForGetJson();

    String res = controller.getJson("{\"int_item\": 123}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(2, count);
  }

  @Test
  public void getJsonテスト_データが存在しない場合_Int型() throws IOException, RablockSystemException {
    String res = controller.getJson("{\"int_item\": 123}").replace(" ", "");
    assertEquals("[]", res);
  }

  @Test
  public void getJsonテスト_データがpoolのみに存在_Double型() throws IOException, RablockSystemException {
    set.createTestPoolDataForGetJson();
    String res = controller.getJson("{\"double_item\": 456.78}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(1, count);
  }

  @Test
  public void getJsonテスト_データがblockのみに存在_Double型() throws IOException, RablockSystemException {
    set.createTestBlockDataForGetJson();
    String res = controller.getJson("{\"double_item\": 456.78}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(1, count);
  }

  @Test
  public void getJsonテスト_データがpool_block両方に存在_Double型() throws IOException, RablockSystemException {
    set.createTestBlockDataForGetJson();
    set.createTestPoolDataForGetJson();

    String res = controller.getJson("{\"double_item\": 456.78}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(2, count);
  }

  @Test
  public void getJsonテスト_データが存在しない場合_Double型() throws IOException, RablockSystemException {
    String res = controller.getJson("{\"double_item\": 456.78}").replace(" ", "");
    assertEquals("[]", res);
  }

  @Test
  public void getJsonテスト_データがpoolのみに存在_Boolean型() throws IOException, RablockSystemException {
    set.createTestPoolDataForGetJson();
    String res = controller.getJson("{\"boolean_item\": true}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(1, count);
  }

  @Test
  public void getJsonテスト_データがblockのみに存在_Boolean型() throws IOException, RablockSystemException {
    set.createTestBlockDataForGetJson();
    String res = controller.getJson("{\"boolean_item\": true}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(1, count);
  }

  @Test
  public void getJsonテスト_データがpool_block両方に存在_Boolean型() throws IOException, RablockSystemException {
    set.createTestBlockDataForGetJson();
    set.createTestPoolDataForGetJson();

    String res = controller.getJson("{\"boolean_item\": true}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(2, count);
  }

  @Test
  public void getJsonテスト_データが存在しない場合_Boolean型() throws IOException, RablockSystemException {
    String res = controller.getJson("{\"boolean_item\": true}").replace(" ", "");
    assertEquals("[]", res);
  }

  @Test
  public void getJsonテスト_データがNull型() throws IOException, RablockSystemException {
    set.createTestBlockDataForGetJson();
    String res = controller.getJson("{\"null_item\": null}");
    assertEquals("nullでは検索できません", res);
  }

  @Test
  public void getJsonテスト_データがArray型() throws IOException, RablockSystemException {
    set.createTestBlockDataForGetJson();
    BsonArray testArray = new BsonArray();
    testArray.add(new BsonString("abc"));
    testArray.add(new BsonString("efg"));
    Document obj = new Document().append("array_item", testArray);
    String res = controller.getJson(obj.toJson());
    assertEquals("配列では検索できません", res);
  }

  @Test
  public void getJsonテスト_データがobject型() throws IOException, RablockSystemException {
    set.createTestBlockDataForGetJson();
    ObjectNode obj = new ObjectNode(null);
    String res = controller.getJson("{\"obj_item\": " + obj + "}");
    assertEquals("オブジェクトでは検索できません", res);
  }

  @Test
  public void 指定されたオブジェクトIDから履歴を返却テスト_poolにデータが存在() throws IOException, RablockSystemException {
    List<Document> list = test.getAllPoolTestData();
    String res_oid = test.getOid(list.get(0));
    String res_1001 = controller.getOid(res_oid);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res_1001.toString());
    JsonNode oidNode = node.get("_id");

    SampleData resultData = new SampleData();
    resultData.setType(node.get("type").asText());
    resultData.setUser_id(node.get("user_id").asText());
    resultData.setTestItem1(node.get("testItem1").asText());
    resultData.setOid(oidNode.get("$oid").asText());

    if (!resultData.getType().equals("new")) {
      resultData.setOriginal_id(node.get("original_id").asText());
    }

    String oid = test.getOid(list.get(0));
    assertEquals(oid, resultData.getOid());
    assertEquals("new", resultData.getType());
    assertEquals("1001", resultData.getUser_id());
    assertEquals("TEST", resultData.getTestItem1());
  }

  @Test
  public void 指定されたオブジェクトIDから履歴を返却テスト_blockにデータが存在() throws IOException, RablockSystemException {
    set.createTestBlockData();
    List<Document> list = test.getAllBlockTestData();
    Document block = list.get(0);
    List<Document> dataObjList = block.getList(ConstItem.BLOCK_DATA, Document.class);

    String find_oid = test.getOid(dataObjList.get(0));
    String res_5001 = controller.getOid(find_oid);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res_5001.toString());
    JsonNode oidNode = node.get("_id");

    SampleData resultData = new SampleData();
    resultData.setType(node.get("type").asText());
    resultData.setUser_id(node.get("user_id").asText());
    resultData.setTestItem1(node.get("testItem1").asText());
    resultData.setOid(oidNode.get("$oid").asText());

    if (!resultData.getType().equals("new")) {
      resultData.setOriginal_id(node.get("original_id").asText());
    }

    assertEquals(find_oid, resultData.getOid());
    assertEquals("new", resultData.getType());
    assertEquals("5001", resultData.getUser_id());
    assertEquals("TEST", resultData.getTestItem1());
  }

  @Test
  public void 指定されたオブジェクトIDが存在しない場合() throws IOException, RablockSystemException {
    String res = controller.getOid("5b7e5fceeaaa3826788c0000");
    assertEquals(" null ", res);
  }

  @Test
  public void 全データを返却テスト_pool_block() throws IOException, RablockSystemException {
    set.createTestBlockData();
    String allData = controller.getAll();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(allData.toString());
    Iterator<JsonNode> iter = node.elements();
    int count = 0;
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    // トランザクションデータ10件、ブロックのデータ3件
    assertEquals(13, count);
  }

  @Test
  public void 全データを返却テスト_poolのみにデータが存在() throws IOException, RablockSystemException {
    String allData = controller.getAll();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(allData.toString());
    Iterator<JsonNode> iter = node.elements();
    int count = 0;
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    // トランザクションデータ10件
    assertEquals(10, count);
  }

  @Test
  public void 全データを返却テスト_blockのみにデータが存在() throws IOException, RablockSystemException {
    set.createTestBlockData();
    test.poolDataDelete();
    String allData = controller.getAll();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(allData);
    Iterator<JsonNode> iter = node.elements();
    int count = 0;
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    // ブロックのデータ３件
    assertEquals(3, count);
  }

  @Test
  public void 全データを返却テスト_データが0件() throws IOException, RablockSystemException {
    test.poolDataDelete();
    String allData = controller.getAll();

    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(allData.toString());
    Iterator<JsonNode> iter = node.elements();
    int count = 0;
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(0, count);
  }

  @Test
  public void 全ブロックを返却テスト_ブロックが存在() throws IOException, RablockSystemException {
    set.createTestBlockData();
    String allData = controller.getBlock();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(allData.toString());
    Iterator<JsonNode> iter = node.elements();
    int count = 0;
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    // ブロックのデータ２件
    assertEquals(2, count);
  }

  @Test
  public void 全ブロックを返却テスト_ブロックが０件() throws IOException, RablockSystemException {
    test.blockDataDelete();
    String allData = controller.getBlock();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(allData.toString());
    Iterator<JsonNode> iter = node.elements();
    int count = 0;
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    // ブロックのデータ0件
    assertEquals(0, count);
  }

  @Test
  public void ブロック化されていないトランザクションデータを取得テスト_データが存在() throws IOException, RablockSystemException {

    String allData = controller.getPool();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(allData.toString());
    Iterator<JsonNode> iter = node.elements();
    int count = 0;
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    // トランザクションデータ10件
    assertEquals(10, count);
  }

  @Test
  public void ブロック化されていないトランザクションデータを取得テスト_データが0件() throws IOException, RablockSystemException {
    test.poolDataDelete();
    String allData = controller.getPool();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(allData.toString());
    Iterator<JsonNode> iter = node.elements();
    int count = 0;
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    // トランザクションデータ0件
    assertEquals(0, count);
  }

  @Test
  public void 伝搬済みトランザクションデータを取得テスト_データが存在() throws IOException, RablockSystemException {

    String allData = controller.getDeliveredPool();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(allData.toString());
    Iterator<JsonNode> iter = node.elements();
    int count = 0;
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    // 伝搬済みトランザクションデータ9件
    assertEquals(9, count);
  }

  @Test
  public void 伝搬済みトランザクションデータを取得テスト_データが0件() throws IOException, RablockSystemException {
    test.poolDataDelete();
    set.createTestPoolNotDeleveryData();
    String allData = controller.getDeliveredPool();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(allData.toString());
    Iterator<JsonNode> iter = node.elements();
    int count = 0;
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    // 伝搬済みトランザクションデータ0件
    assertEquals(0, count);
  }

  @Test
  public void 最後のブロックを取得_ブロックが存在する() throws IOException, RablockSystemException {
    set.createTestBlockData();
    String lastData = controller.getLastBlock();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(lastData.toString());
    assertEquals("abcdef123", node.get("hash").asText());
  }

  @Test
  public void 最後のブロックを取得_ブロックが存在しない() throws IOException, RablockSystemException {
    test.blockDataDelete();
    String result = controller.getLastBlock();
    assertEquals("null", result);
  }

  @Test
  public void 指定したオブジェクトIDから履歴を追跡テスト_poolに追跡元_先のデータが存在()
      throws IOException, RablockSystemException {

    List<Document> list = test.getAllPoolTestData();
    // 追跡元データ
    String test_oid_origin = test.getOid(list.get(8));
    String res_traking = controller.getHistory(test_oid_origin);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res_traking.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(3, count);
  }

  @Test
  public void 指定したオブジェクトIDから履歴を追跡テスト_blockに追跡元_先のデータが存在()
      throws IOException, RablockSystemException {

    set.createTestBlockData();
    List<Document> list = test.getAllBlockTestData();
    Document block = list.get(0);
    List<Document> dataObjList = block.getList(ConstItem.BLOCK_DATA, Document.class);

    // 追跡元データ
    String test_oid_origin = test.getOid(dataObjList.get(2));
    String res_traking = controller.getHistory(test_oid_origin);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res_traking.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(2, count);
  }

  @Test
  public void 指定したオブジェクトIDから履歴を追跡テスト_blockに追跡元_poolに先のデータが存在()
      throws IOException, NoSuchAlgorithmException, RablockSystemException {

    test.blockDataDelete();
    test.poolDataDelete();

    set.createGenBlock();
    set.createTrakingData_1();

    List<Document> list = test.getAllBlockTestData();
    Document block = list.get(0);
    List<Document> dataObjList = block.getList(ConstItem.BLOCK_DATA, Document.class);

    // 追跡元データ
    String test_oid_origin = test.getOid(dataObjList.get(0));
    String res_traking = controller.getHistory(test_oid_origin);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res_traking.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(2, count);
  }

  @Test
  public void 指定したオブジェクトIDから履歴を追跡テスト_poolに追跡元_blockに先のデータが存在()
      throws IOException, NoSuchAlgorithmException, RablockSystemException {

    test.blockDataDelete();
    test.poolDataDelete();

    set.createGenBlock();
    set.createTrakingData_2();

    List<Document> list = test.getAllPoolTestData();
    // 追跡元データ
    String test_oid_origin = test.getOid(list.get(0));
    String res_traking = controller.getHistory(test_oid_origin);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res_traking.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(2, count);
  }

  @Test
  public void 指定したオブジェクトIDから履歴を追跡テスト_poolにのみ追跡元() throws RablockSystemException, IOException {

    test.poolDataDelete();
    set.createTestPoolNotDeleveryData();

    List<Document> list = test.getAllPoolTestData();
    // 追跡元データ
    String test_oid_origin = test.getOid(list.get(0));
    String res_traking = controller.getHistory(test_oid_origin);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res_traking.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(1, count);
  }

  @Test
  public void 指定したオブジェクトIDから履歴を追跡テスト_blockにのみ追跡元() throws RablockSystemException, IOException {

    set.createTestBlockData();
    List<Document> list = test.getAllBlockTestData();
    Document block = list.get(0);
    List<Document> dataObjList = block.getList(ConstItem.BLOCK_DATA, Document.class);

    // 追跡元データ
    String test_oid_origin = test.getOid(dataObjList.get(0));
    String res_traking = controller.getHistory(test_oid_origin);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res_traking.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(1, count);
  }

  @Test
  public void getJsonテスト_ネストデータがpoolのみに1件存在_String型()
      throws IOException, InterruptedException, RablockSystemException {

    test.poolDataDelete();
    Thread.sleep(250);
    set.createNestTestPoolData();

    String res = controller.getJson("{\"nest.nestItem_3.nestItem_4\":\"abc\"}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(1, count);
  }

  @Test
  public void getJsonテスト_ネストデータがpoolのみに複数件存在_String型()
      throws IOException, InterruptedException, RablockSystemException {

    test.poolDataDelete();
    Thread.sleep(250);
    set.createNestTestPoolData();
    set.createNestTestPoolData();
    set.createNestTestPoolData();

    String res = controller.getJson("{\"nest.nestItem_3.nestItem_4\":\"abc\"}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(3, count);
  }

  @Test
  public void getJsonテスト_ネストデータが1Blockに1件存在_String型()
      throws IOException, InterruptedException, RablockSystemException {

    set.createNestTestBlockData();

    String res = controller.getJson("{\"nest.nestItem_3.nestItem_4\":\"abc\"}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(1, count);
  }

  @Test
  public void getJsonテスト_ネストデータが1Blockに2件存在_String型()
      throws IOException, InterruptedException, RablockSystemException {

    set.createNestTestBlockData_2();

    String res = controller.getJson("{\"nest.nestItem_3.nestItem_4\":\"abc\"}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(2, count);
  }

  @Test
  public void getJsonテスト_ネストデータが1Blockに1件_通常データ1件存在_String型()
      throws IOException, InterruptedException, RablockSystemException {

    set.createNestTestBlockData_1_1();

    String res = controller.getJson("{\"nest.nestItem_3.nestItem_4\":\"abc\"}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(1, count);
  }

  @Test
  public void getJsonテスト_ネストデータが1Blockに0件_通常データ1件存在_String型()
      throws IOException, InterruptedException, RablockSystemException {

    set.createNestTestBlockData_0_1();

    String res = controller.getJson("{\"nest.nestItem_3.nestItem_4\":\"abc\"}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(0, count);
  }

  @Test
  public void getJsonテスト_ネストデータが1Blockに1件_Poolに1件存在_String型()
      throws IOException, InterruptedException, RablockSystemException {

    set.createNestTestBlockData();
    set.createNestTestPoolData();

    String res = controller.getJson("{\"nest.nestItem_3.nestItem_4\":\"abc\"}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(2, count);
  }

  @Test
  public void getJsonテスト_ネストデータが2Blockに1件ずつ存在_String型()
      throws IOException, InterruptedException, RablockSystemException {

    set.createNestTest2BlockData();

    String res = controller.getJson("{\"nest.nestItem_3.nestItem_4\":\"abc\"}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(2, count);
  }

  @Test
  public void getJsonテスト_ネストデータがpoolのみに1件存在_int型()
      throws IOException, InterruptedException, RablockSystemException {

    test.poolDataDelete();
    Thread.sleep(250);
    set.createNestTestPoolData();

    String res = controller.getJson("{\"nest.nestItem_3.nestIntItem\":1}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(1, count);
  }

  @Test
  public void getJsonテスト_ネストデータがpoolのみに複数件存在_Int型()
      throws IOException, InterruptedException, RablockSystemException {

    test.poolDataDelete();
    Thread.sleep(250);
    set.createNestTestPoolData();
    set.createNestTestPoolData();
    set.createNestTestPoolData();

    String res = controller.getJson("{\"nest.nestItem_3.nestIntItem\":1}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(3, count);
  }

  @Test
  public void getJsonテスト_ネストデータが1Blockに1件存在_Int型()
      throws IOException, InterruptedException, RablockSystemException {

    set.createNestTestBlockData();

    String res = controller.getJson("{\"nest.nestItem_3.nestIntItem\":1}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(1, count);
  }

  @Test
  public void getJsonテスト_ネストデータが1Blockに2件存在_Int型()
      throws IOException, InterruptedException, RablockSystemException {

    set.createNestTestBlockData_2();

    String res = controller.getJson("{\"nest.nestItem_3.nestIntItem\":1}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(2, count);
  }

  @Test
  public void getJsonテスト_ネストデータが1Blockに1件_通常データ1件存在_Int型()
      throws IOException, InterruptedException, RablockSystemException {

    set.createNestTestBlockData_1_1();

    String res = controller.getJson("{\"nest.nestItem_3.nestIntItem\":1}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(1, count);
  }

  @Test
  public void getJsonテスト_ネストデータが1Blockに0件_通常データ1件存在_Int型()
      throws IOException, InterruptedException, RablockSystemException {

    set.createNestTestBlockData_0_1();

    String res = controller.getJson("{\"nest.nestItem_3.nestIntItem\":1}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(0, count);
  }

  @Test
  public void getJsonテスト_ネストデータが1Blockに1件_Poolに1件存在_Int型()
      throws IOException, InterruptedException, RablockSystemException {

    set.createNestTestBlockData();
    set.createNestTestPoolData();

    String res = controller.getJson("{\"nest.nestItem_3.nestIntItem\":1}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(2, count);
  }

  @Test
  public void getJsonテスト_ネストデータが2Blockに1件ずつ存在_Int型()
      throws IOException, InterruptedException, RablockSystemException {

    set.createNestTest2BlockData();

    String res = controller.getJson("{\"nest.nestItem_3.nestIntItem\":1}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(2, count);
  }

  @Test
  public void getJsonテスト_ネストデータがpoolのみに1件存在_Double型()
      throws IOException, InterruptedException, RablockSystemException {

    test.poolDataDelete();
    Thread.sleep(250);
    set.createNestTestPoolData();

    String res = controller.getJson("{\"nest.nestItem_3.nestDoubleItem\":1.23}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(1, count);
  }

  @Test
  public void getJsonテスト_ネストデータがpoolのみに複数件存在_Double型()
      throws IOException, InterruptedException, RablockSystemException {

    test.poolDataDelete();
    Thread.sleep(250);
    set.createNestTestPoolData();
    set.createNestTestPoolData();
    set.createNestTestPoolData();

    String res = controller.getJson("{\"nest.nestItem_3.nestDoubleItem\":1.23}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(3, count);
  }

  @Test
  public void getJsonテスト_ネストデータが1Blockに1件存在_Double型()
      throws IOException, InterruptedException, RablockSystemException {

    set.createNestTestBlockData();

    String res = controller.getJson("{\"nest.nestItem_3.nestDoubleItem\":1.23}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(1, count);
  }

  @Test
  public void getJsonテスト_ネストデータが1Blockに2件存在_Double型()
      throws IOException, InterruptedException, RablockSystemException {

    set.createNestTestBlockData_2();

    String res = controller.getJson("{\"nest.nestItem_3.nestDoubleItem\":1.23}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(2, count);
  }

  @Test
  public void getJsonテスト_ネストデータが1Blockに1件_通常データ1件存在_Double型()
      throws IOException, InterruptedException, RablockSystemException {

    set.createNestTestBlockData_1_1();

    String res = controller.getJson("{\"nest.nestItem_3.nestDoubleItem\":1.23}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(1, count);
  }

  @Test
  public void getJsonテスト_ネストデータが1Blockに0件_通常データ1件存在_Double型()
      throws IOException, InterruptedException, RablockSystemException {

    set.createNestTestBlockData_0_1();

    String res = controller.getJson("{\"nest.nestItem_3.nestDoubleItem\":1.23}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(0, count);
  }

  @Test
  public void getJsonテスト_ネストデータが1Blockに1件_Poolに1件存在_Double型()
      throws IOException, InterruptedException, RablockSystemException {

    set.createNestTestBlockData();
    set.createNestTestPoolData();

    String res = controller.getJson("{\"nest.nestItem_3.nestDoubleItem\":1.23}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(2, count);
  }

  @Test
  public void getJsonテスト_ネストデータが2Blockに1件ずつ存在_Double型()
      throws IOException, InterruptedException, RablockSystemException {

    set.createNestTest2BlockData();

    String res = controller.getJson("{\"nest.nestItem_3.nestDoubleItem\":1.23}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(2, count);
  }

  @Test
  public void getJsonテスト_ネストデータがpoolのみに1件存在_Boolean型()
      throws IOException, InterruptedException, RablockSystemException {

    test.poolDataDelete();
    Thread.sleep(250);
    set.createNestTestPoolData();

    String res = controller.getJson("{\"nest.nestItem_3.nestBooleanItem\":true}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(1, count);
  }

  @Test
  public void getJsonテスト_ネストデータがpoolのみに複数件存在_Boolean型()
      throws IOException, InterruptedException, RablockSystemException {

    test.poolDataDelete();
    Thread.sleep(250);
    set.createNestTestPoolData();
    set.createNestTestPoolData();
    set.createNestTestPoolData();

    String res = controller.getJson("{\"nest.nestItem_3.nestBooleanItem\":true}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(3, count);
  }

  @Test
  public void getJsonテスト_ネストデータが1Blockに1件存在_Boolean型()
      throws IOException, InterruptedException, RablockSystemException {

    set.createNestTestBlockData();

    String res = controller.getJson("{\"nest.nestItem_3.nestBooleanItem\":true}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(1, count);
  }

  @Test
  public void getJsonテスト_ネストデータが1Blockに2件存在_Boolean型()
      throws IOException, InterruptedException, RablockSystemException {

    set.createNestTestBlockData_2();

    String res = controller.getJson("{\"nest.nestItem_3.nestBooleanItem\":true}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(2, count);
  }

  @Test
  public void getJsonテスト_ネストデータが1Blockに1件_通常データ1件存在_Boolean型()
      throws IOException, InterruptedException, RablockSystemException {

    set.createNestTestBlockData_1_1();

    String res = controller.getJson("{\"nest.nestItem_3.nestBooleanItem\":true}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(1, count);
  }

  @Test
  public void getJsonテスト_ネストデータが1Blockに0件_通常データ1件存在_Boolean型()
      throws IOException, InterruptedException, RablockSystemException {

    set.createNestTestBlockData_0_1();

    String res = controller.getJson("{\"nest.nestItem_3.nestBooleanItem\":true}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(0, count);
  }

  @Test
  public void getJsonテスト_ネストデータが1Blockに1件_Poolに1件存在_Boolean型()
      throws IOException, InterruptedException, RablockSystemException {

    set.createNestTestBlockData();
    set.createNestTestPoolData();

    String res = controller.getJson("{\"nest.nestItem_3.nestBooleanItem\":true}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(2, count);
  }

  @Test
  public void getJsonテスト_ネストデータが2Blockに1件ずつ存在_Boolean型()
      throws IOException, InterruptedException, RablockSystemException {

    set.createNestTest2BlockData();

    String res = controller.getJson("{\"nest.nestItem_3.nestBooleanItem\":true}");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(res.toString());

    int count = 0;
    Iterator<JsonNode> iter = node.elements();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    assertEquals(2, count);
  }
}

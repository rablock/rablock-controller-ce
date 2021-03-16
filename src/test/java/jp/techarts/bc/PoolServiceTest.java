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

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import jp.techarts.bc.test.SampleMethod;
import jp.techarts.bc.test.SetUpData;
import org.bson.Document;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PoolServiceTest {

  @Autowired private PoolService service;
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
  public void トランザクションプールにデータを登録テスト() throws RablockSystemException {
    final String json = "{ \"type\" : \"new\", \"user_id\" : \"9999\", \"testItem1\" : \"TEST\"}";
    // jsonObjectに変換
    JSONObject jsonObj = new JSONObject(json);

    String settime = "2018/08/31 09:12:34";

    final List<String> itemList = new ArrayList<>();
    itemList.add("type");
    itemList.add("user_id");
    itemList.add("testItem1");

    final Document result = service.setPool(jsonObj, settime, itemList);
    final String type = result.get("type").toString();
    final String user_id = result.get("user_id").toString();
    final String testItem1 = result.get("testItem1").toString();
    settime = result.get("settime").toString();
    final Boolean deliveryF = (Boolean) result.get("deliveryF");

    assertEquals(11, test.testDataPoolCount());
    assertEquals("new", type);
    assertEquals("9999", user_id);
    assertEquals("TEST", testItem1);
    assertEquals("2018/08/31 09:12:34", settime);
    assertTrue(deliveryF); /* As tests are run under the single mode */
  }

  @Test
  public void PoolテーブルからオブジェクトIDでデータを検索テスト() throws RablockSystemException {
    final List<Document> testData = test.getAllPoolTestData();
    final Document data_1 = testData.get(0);
    final String data_1_oid = data_1.get("_id").toString();
    final Document result = service.getDataByOidinPool(data_1_oid).get();

    final String data_1_type = result.get("type", String.class);
    final String data_1_user_id = result.get("user_id", String.class);
    assertEquals("new", data_1_type);
    assertEquals("1001", data_1_user_id);
  }

  @Test
  public void 検索したオブジェクトIDが存在しない場合テスト() throws RablockSystemException {
    Document obj = service.getDataByOidinPool("5b838915eaaa382398980000").orElse(null);
    assertNull(obj);
  }

  @Test
  public void PoolテーブルからユーザーIDでデータを全て検索テスト() throws IOException, RablockSystemException {
    final String json1 = "{\"user_id\": \"1001\"}";
    final String json2 = "{\"user_id\": \"2001\"}";
    final String json3 = "{\"user_id\": \"3001\"}";
    final String json4 = "{\"user_id\": \"4001\"}";

    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode jsonnode1 = mapper.readTree(json1);
    final JsonNode jsonnode2 = mapper.readTree(json2);
    final JsonNode jsonnode3 = mapper.readTree(json3);
    final JsonNode jsonnode4 = mapper.readTree(json4);

    List<Document> resultList = service.getByKeyValue("user_id", jsonnode1);
    assertEquals(1, resultList.size());
    resultList = service.getByKeyValue("user_id", jsonnode2);
    assertEquals(0, resultList.size());
    resultList = service.getByKeyValue("user_id", jsonnode3);
    assertEquals(2, resultList.size());
    resultList = service.getByKeyValue("user_id", jsonnode4);
    assertEquals(1, resultList.size());
  }

  @Test
  public void 検索したユーザーIDが存在しない場合テスト() throws IOException, RablockSystemException {
    final ObjectMapper mapper = new ObjectMapper();

    final String json = "{\"user_id\": \"9999\"}";
    final JsonNode jsonnode = mapper.readTree(json);

    List<Document> result = service.getByKeyValue("user_id", jsonnode);
    assertEquals(0, result.size());
  }

  // @Test
  // public void PoolテーブルからユーザーID修正_削除データを検索テスト() {
  // List<String> resultList = service.getModifyDeleteListinPool("user_id",
  // "1001");
  // assertEquals(1, resultList.size());
  // resultList = service.getModifyDeleteListinPool("user_id", "2001");
  // assertEquals(1, resultList.size());
  // resultList = service.getModifyDeleteListinPool("user_id", "3001");
  // assertEquals(0, resultList.size());
  // resultList = service.getModifyDeleteListinPool("user_id", "4001");
  // assertEquals(2, resultList.size());
  // }

  // @Test
  // public void 検索したユーザーIDが存在しない場合終了するテスト2() {
  // try {
  // service.getModifyDeleteListinPool("user_id", "9999");
  // } catch (SystemExitStub.ExitException e) {
  // assertEquals(e.state, -1);
  // }
  // }

  @Test
  public void Poolコレクションのデータ全件取得テスト_データ有() throws RablockSystemException {
    final List<Document> resultList = service.getAllPool();
    assertEquals(10, resultList.size());
  }

  @Test
  public void Poolコレクションのデータ全件取得テスト_データ無() throws InterruptedException, RablockSystemException {
    test.poolDataDelete();
    Thread.sleep(250);
    final List<Document> resultList = service.getAllPool();
    assertEquals(0, resultList.size());
  }

  @Test
  public void PoolコレクションのDeliveryFがTRUEのデータ取得テスト_データ有() throws RablockSystemException {
    final List<Document> resultList = service.getDeliveredData();
    assertEquals(9, resultList.size());
  }

  @Test
  public void PoolコレクションのDeliveryFがTRUEのデータ取得テスト_データ無()
      throws InterruptedException, RablockSystemException {
    test.poolDataDelete();
    Thread.sleep(250);
    final List<Document> resultList = service.getDeliveredData();
    assertEquals(0, resultList.size());
  }
}

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

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import jp.techarts.bc.data.PoolRepository;
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
public class PoolRepositoryTest {

  @Autowired private PoolService service;
  @Autowired private PoolRepository repository;
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
  public void update_delivertyF() throws RablockSystemException {
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

    repository
        .getTranDataList()
        .parallelStream()
        .forEach(o -> repository.updateTranDeliveryStatus(o, false));

    repository
        .getTranDataObjIDList()
        .parallelStream()
        .forEach(
            oid -> {
              Boolean result2;
              try {
                result2 =
                    (Boolean) service.getDataByOidinPool(oid.toString()).get().get("deliveryF");
                assertFalse(result2);
              } catch (RablockSystemException e) {
                throw new RuntimeException(e);
              }
            });
  }
}

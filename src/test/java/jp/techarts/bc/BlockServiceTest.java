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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.PostConstruct;
import jp.techarts.bc.constitem.ConstItem;
import jp.techarts.bc.prop.GetAppProperties;
import jp.techarts.bc.test.SampleMethod;
import jp.techarts.bc.test.SetUpData;
import junit.framework.AssertionFailedError;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class BlockServiceTest {

  @Autowired private BlockService service;
  @Autowired private GetAppProperties app;
  @Autowired private SampleMethod test;
  @Autowired SetUpData set;

  private String db_host;

  /** ??????????????????_??????????????? */
  private int db_port = 0;
  /** ????????????????????? */
  private String db_name = "";
  /** ??????????????????_??????????????? */
  private String db_pass = "";
  /** ??????????????????_???????????? */
  private String db_user_name = "";
  /** ????????????????????????????????? */
  private String collection_block_name = "";

  /** ??????????????????<br> */
  @PostConstruct
  public void initAfterStartup() {
    db_host = app.getMongodb_host();
    db_port = app.getMongodb_port();
    db_name = app.getMongodb_db();
    db_pass = app.getMongodb_pass();
    db_user_name = app.getMongodb_username();
    collection_block_name = app.getMongodb_coll_block();
  }

  @Before
  public void initialize()
      throws InterruptedException, NoSuchAlgorithmException, UnsupportedEncodingException,
          RablockSystemException {

    // DB??????
    Thread.sleep(250);
    test.blockDataDelete();
    Thread.sleep(250);
    test.poolDataDelete();
    Thread.sleep(250);

    set.createGenBlock();
    set.createTestPoolData();
  }

  @Test
  public void ??????????????????????????????????????????() throws JsonProcessingException {
    // @Before????????????????????????????????????????????????
    assertEquals(1, test.testDataBlockCount());
  }

  private MongoClient getMongoClient() {
    // MongoDB??????
    MongoCredential mongoCredential =
        MongoCredential.createScramSha1Credential(db_user_name, db_name, db_pass.toCharArray());
    MongoClientSettings mongoClientSettings =
        MongoClientSettings.builder()
            .credential(mongoCredential)
            .applyToClusterSettings(
                builder -> builder.hosts(Arrays.asList(new ServerAddress(db_host, db_port))))
            .build();
    // MondoDB?????????????????????
    return MongoClients.create(mongoClientSettings);
  }

  @Test
  public void ?????????????????????????????????????????????() throws JsonProcessingException, RablockSystemException {
    try (MongoClient mongoClient = this.getMongoClient()) {
      final MongoDatabase db = mongoClient.getDatabase(db_name);
      final MongoCollection<Document> coll = db.getCollection(collection_block_name);

      // ???????????????
      Document genData = test.getGenBlock();
      String gen_hash = genData.get("hash").toString();
      Document data_0 =
          new Document("prev_hash", gen_hash)
              .append("hash", "abcdef123")
              .append("testItem1", "TEST");
      coll.insertOne(data_0);

      Document data = service.getLastBlock().orElseThrow(AssertionFailedError::new);
      String data_hash = data.get("hash").toString();
      String data_testItem1 = data.get("testItem1").toString();

      assertEquals(2, test.testDataBlockCount());
      assertEquals("abcdef123", data_hash);
      assertEquals("TEST", data_testItem1);
    }
  }

  @Test
  public void ?????????????????????????????????????????????()
      throws JsonProcessingException, InterruptedException, RablockSystemException {
    test.blockDataDelete();
    Thread.sleep(250);
    Document result = service.getLastBlock().orElse(null);
    assertNull(result);
  }

  @Test
  public void block????????????????????????oid??????????????????????????????() throws JsonProcessingException, RablockSystemException {
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);
      MongoCollection<Document> coll = db.getCollection(collection_block_name);

      List<Document> poolList = test.getAllPoolTestData();

      // ???????????????????????????
      Document genData = test.getGenBlock();
      String gen_hash = genData.get("hash").toString();

      Document block_1 =
          new Document(ConstItem.BLOCK_DATA, poolList)
              .append("prev_hash", gen_hash)
              .append("hash", "abcdef123")
              .append("testItem1", "TEST");
      coll.insertOne(block_1);

      List<Document> pooldata = test.getAllPoolTestData();
      String oid = pooldata.get(0).get("_id").toString();
      Document findObj = service.findByOidinBlock(oid).orElseThrow(AssertionFailedError::new);
      assertNotNull(findObj);

      String findObj_type = findObj.get("type").toString();
      String findObj_user_id = findObj.get("user_id").toString();
      String findObj_testItem1 = findObj.get("testItem1").toString();

      assertEquals("new", findObj_type);
      assertEquals("1001", findObj_user_id);
      assertEquals("TEST", findObj_testItem1);
    }
  }

  @Test
  public void block????????????????????????oid?????????????????????????????????????????????()
      throws JsonProcessingException, RablockSystemException {
    Document obj = service.findByOidinBlock("5b7e5fceeaaa3826788c0000").orElse(null);
    assertNull(obj);
  }

  @Test
  public void ?????????????????????????????????????????????????????????() throws JsonProcessingException, RablockSystemException {
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);
      MongoCollection<Document> coll = db.getCollection(collection_block_name);

      List<Document> poolList = test.getAllPoolTestData();

      // ???????????????????????????
      Document genData = test.getGenBlock();
      String gen_hash = genData.get("hash").toString();
      List<Document> block1_list = new ArrayList<>();
      block1_list.add(poolList.get(0));
      block1_list.add(poolList.get(1));
      block1_list.add(poolList.get(2));

      List<Document> block2_list = new ArrayList<>();
      block2_list.add(poolList.get(3));
      block2_list.add(poolList.get(4));
      block2_list.add(poolList.get(5));
      block2_list.add(poolList.get(6));
      block2_list.add(poolList.get(7));
      block2_list.add(poolList.get(8));

      // Block1??????
      Document block_0 =
          new Document("data", block1_list)
              .append("prev_hash", gen_hash)
              .append("hash", "abcdef123")
              .append("testItem1", "TEST");
      coll.insertOne(block_0);

      // Block2??????
      List<Document> blockAll = test.getAllBlockTestData();
      Document block = blockAll.get(0);
      String prev_hash = block.get("hash").toString();
      Document block_1 =
          new Document("data", block2_list)
              .append("prev_hash", prev_hash)
              .append("hash", "ghijklm456")
              .append("testItem1", "TEST");
      coll.insertOne(block_1);

      // ???????????????????????????????????????
      test.poolDataDelete();

      List<Document> findList_1001 = service.getByKeyValue("user_id", "1001");
      List<Document> findList_2001 = service.getByKeyValue("user_id", "2001");
      List<Document> findList_3001 = service.getByKeyValue("user_id", "3001");
      List<Document> findList_4001 = service.getByKeyValue("user_id", "4001");

      assertEquals(1, findList_1001.size());
      assertEquals(0, findList_2001.size());
      assertEquals(2, findList_3001.size());
      assertEquals(1, findList_4001.size());
    }
  }

  //   @Test
  //   public void ?????????????????????????????????????????????????????????????????????() throws JsonProcessingException, InterruptedException,
  // RablockSystemException {
  //     test.blockDataDelete();
  //     Thread.sleep(250);
  //     List<DBObject> obj = service.getStringByKeyValue("user_id", "9999");
  //     assertEquals(0, obj.size(), 0);
  //   }

  @Test
  public void ???????????????????????????????????????????????????() throws JsonProcessingException, RablockSystemException {
    List<Document> res = service.getByKeyValue("user_id", "100100");
    // ???????????????
    List<Document> array = new ArrayList<>();
    assertEquals(array, res);
  }

  @Test
  public void Block??????????????????????????????????????????????????????() throws JsonProcessingException, RablockSystemException {
    try (MongoClient mongoClient = this.getMongoClient()) {
      MongoDatabase db = mongoClient.getDatabase(db_name);
      MongoCollection<Document> coll = db.getCollection(collection_block_name);

      List<Document> poolList = test.getAllPoolTestData();

      // ???????????????????????????
      Document genData = test.getGenBlock();
      String gen_hash = genData.get("hash").toString();
      List<Document> block1_list = new ArrayList<>();
      block1_list.add(poolList.get(0));
      block1_list.add(poolList.get(1));
      block1_list.add(poolList.get(2));

      List<Document> block2_list = new ArrayList<>();
      block2_list.add(poolList.get(3));
      block2_list.add(poolList.get(4));
      block2_list.add(poolList.get(5));
      block2_list.add(poolList.get(6));
      block2_list.add(poolList.get(7));
      block2_list.add(poolList.get(8));

      // Block1??????
      Document block_0 =
          new Document("data", block1_list)
              .append("prev_hash", gen_hash)
              .append("hash", "abcdef123")
              .append("testItem1", "TEST");
      coll.insertOne(block_0);

      // Block2??????
      List<Document> blockAll = test.getAllBlockTestData();
      Document block = blockAll.get(0);
      String prev_hash = block.get("hash").toString();
      Document block_1 =
          new Document("data", block2_list)
              .append("prev_hash", prev_hash)
              .append("hash", "ghijklm456")
              .append("testItem1", "TEST");
      coll.insertOne(block_1);

      // ???????????????????????????????????????
      test.poolDataDelete();

      List<Document> allBlock = service.getAllBlock();
      assertEquals(3, allBlock.size());
    }
  }
}

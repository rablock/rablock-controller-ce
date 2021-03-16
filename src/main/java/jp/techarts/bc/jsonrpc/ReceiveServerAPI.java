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


package jp.techarts.bc.jsonrpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;
import jp.techarts.bc.RablockSystemException;

/**
 * 他ノードから送信されたときに受け取るインターフェースクラス<br>
 * Copyright (c) 2018 Rablock Inc.
 *
 * @author TA
 * @version 1.0
 */
@JsonRpcService("/checknode")
public interface ReceiveServerAPI {
  /**
   * トランザクション受け取ってPoolに格納する
   *
   * @param block トランザクションデータ
   * @return OK
   * @throws RablockSystemException
   */
  String receivePool(@JsonRpcParam(value = "list") JsonNode list) throws RablockSystemException;

  /**
   * ブロックを他ノードから受け取りMongoDBに格納し、ブロックに追加されたトランザクションをプールから削除する
   *
   * @param block ブロック
   * @return 受信ブロックのprev_hashと自ノードののprev_hashが一致しているか 一致:OK 不一致：Hash_NG
   * @throws RablockSystemException
   */
  String receiveBlock(@JsonRpcParam(value = "block") JsonNode block) throws RablockSystemException;

  /**
   * マイニングされたブロックを受け取りMongoDBに格納し、ブロックに追加されたトランザクションをプールから削除する
   *
   * @param block ブロック
   * @return 受信ブロックのprev_hashと自ノードののprev_hashが一致しているか 一致:OK 不一致：Hash_NG
   * @throws RablockSystemException
   */
  String receiveSendBlock(@JsonRpcParam(value = "block") JsonNode block)
      throws RablockSystemException;

  /**
   * 参加ノードにトランザクションプールのデータを送り返す
   *
   * @return トランザクションデータ全件
   * @throws RablockSystemException
   */
  String copyPool() throws RablockSystemException;

  /**
   * 参加ノードにブロックのデータを送り返す
   *
   * @return ブロック全件
   * @throws RablockSystemException
   */
  String copyBlock() throws RablockSystemException;

  /**
   * 削除するブロック受信時に自ノードから削除する。
   *
   * @param hash 削除するブロックのハッシュ値
   * @return 削除：OK 削除するブロックがない：NG
   * @throws RablockSystemException
   * @throws JsonProcessingException
   */
  String receiveDeleteBlock(@JsonRpcParam(value = "hash") String hash)
      throws RablockSystemException, JsonProcessingException;

  /**
   * 受信したハッシュ値で自ノードからブロックを検索し返却する。
   *
   * @param hash チェックするブロックのハッシュ値
   * @return そのハッシュをもつブロックを返却
   * @throws RablockSystemException
   */
  String receiveCheckBlockbyHash(@JsonRpcParam(value = "hash") String hash)
      throws JsonProcessingException, RablockSystemException;

  /**
   * 受信した親ハッシュ値で自ノードからブロックを検索し返却する。
   *
   * @param PrevHash チェックするブロックの親ハッシュ値
   * @return その親ハッシュをもつブロックを返却
   * @throws RablockSystemException
   */
  String receiveCheckBlockbyPrevHash(@JsonRpcParam(value = "hash") String hash)
      throws JsonProcessingException, RablockSystemException;

  /**
   * 自ノードのブロック件数を返却する。
   *
   * @return ブロックの件数
   * @throws RablockSystemException
   */
  int getBlockCount() throws RablockSystemException;

  /**
   * ブロックのハッシュ値の配列を返却する。
   *
   * @return ブロックの件数
   * @throws RablockSystemException
   */
  String getHashList() throws RablockSystemException;
}

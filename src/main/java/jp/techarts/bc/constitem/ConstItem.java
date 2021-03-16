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


package jp.techarts.bc.constitem;

/**
 * ブロックの定義クラス<br>
 * Copyright (c) 2018 Rablock Inc.
 *
 * @author TA
 * @version 1.0
 */
public final class ConstItem {
  /** ブロック高 */
  public static final String BLOCK_HEIGHT = "height";
  /** ブロックのデータ数 */
  public static final String BLOCK_SIZE = "size";
  /** ブロックのData */
  public static final String BLOCK_DATA = "data";
  /** ブロックのsettime */
  public static final String BLOCK_SETTIME = "settime";
  /** ブロックのtimestamp */
  public static final String BLOCK_TIMESTAMP = "timestamp";
  /** ブロックのprev_hash */
  public static final String BLOCK_PREV_HASH = "prev_hash";
  /** ブロックのhash */
  public static final String BLOCK_HASH = "hash";

  /** Dataのtype */
  public static final String DATA_TYPE = "type";
  /** Dataのsettime */
  public static final String DATA_SETTIME = "settime";

  /** 削除・更新時Dataのoriginal_id */
  public static final String DATA_ORIGINAL_ID = "original_id";

  /** スマートコントラクト：契約種別 */
  public static final String CONTRACT_TYPE = "contract";
  /** スマートコントラクト：契約番号 */
  public static final String CONTRACT_NUMBER = "number";
  /** スマートコントラクト：契約名 */
  public static final String CONTRACT_NAME = "name";
  /** スマートコントラクト：オペレーション定義 */
  public static final String CONTRACT_FUNC = "functions";
  /** スマートコントラクト：オペレーションID */
  public static final String CONTRACT_FUNC_ID = "funcid";
  /** スマートコントラクト：オペレーション名 */
  public static final String CONTRACT_FUNC_NAME = "funcname";
  /** スマートコントラクト：オペレーションURL */
  public static final String CONTRACT_FUNC_URL = "funcurl";
  /** スマートコントラクト：ユーザー */
  public static final String CONTRACT_USER = "user";
  /** スマートコントラクト：ユーザー契約ID */
  public static final String CONTRACT_AGREE_ID = "agreeId";
  /** スマートコントラクト：ユーザー契約ID */
  public static final String CONTRACT_AGREE_NAME = "agreeName";
  /** スマートコントラクト：契約開始日時 */
  public static final String CONTRACT_START_DATE = "startDate";
  /** スマートコントラクト：契約終了日時 */
  public static final String CONTRACT_END_DATE = "endDate";
  /** スマートコントラクト：実行日時 */
  public static final String CONTRACT_EXEC_DATE = "execDate";
  /** スマートコントラクト：結果コード */
  public static final String CONTRACT_EXEC_RESULT = "result";
  /** スマートコントラクト：結果メッセージ */
  public static final String CONTRACT_EXEC_MSG = "resultMsg";
  /** スマートコントラクト：結果情報 */
  public static final String CONTRACT_EXEC_INFO = "info";
}

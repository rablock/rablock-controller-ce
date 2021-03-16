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


package jp.techarts.bc.prop;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * propertyファイルの値を取得するクラス<br>
 * Copyright (c) 2018 Rablock Inc.
 *
 * @author TA
 * @version 1.0
 */
@Configuration
@PropertySource("classpath:application.properties")
public class GetAppProperties {

  /** 使用するポート番号 */
  @Value("${server.port}")
  private String port;

  /** 使用するMongoDBのポート番号 */
  @Value("${spring.data.mongodb.host}")
  private String mongodb_host;

  /** 使用するMongoDBのポート番号 */
  @Value("${spring.data.mongodb.port}")
  private int mongodb_port;

  /** 使用するDBのDB名 */
  @Value("${spring.data.mongodb.database}")
  private String mongodb_db;

  /** 使用するDBのパスワード */
  @Value("${spring.data.mongodb.password}")
  private String mongodb_pass;

  /** 使用するDBのユーザ名 */
  @Value("${spring.data.mongodb.username}")
  private String mongodb_username;

  /** 使用するプールコレクションの名前 */
  @Value("${spring.data.mongodb.collection.pool}")
  private String mongodb_coll_pool;

  /** 使用するブロックコレクションの名前 */
  @Value("${spring.data.mongodb.collection.block}")
  private String mongodb_coll_block;

  /** ダイジェスト認証 ユーザ名 */
  @Value("${digest.username}")
  private String digestUsername;

  /** ダイジェスト認証 パスワード */
  @Value("${digest.pass}")
  private String digestPass;

  /** 暗号化ON/OFF */
  @Value("${crypto.status}")
  private String cryptoStatus;

  /** 秘密鍵 */
  @Value("${private.key.file}")
  private String privateKeyFile;

  /** 公開鍵 */
  @Value("${public.key.file}")
  private String publicKeyFile;

  public String getDigestUserName() {
    return digestUsername;
  }

  public String getDigestPass() {
    return digestPass;
  }

  public String getPort() {
    return port;
  }

  public String getMongodb_host() {
    return mongodb_host;
  }

  public int getMongodb_port() {
    return mongodb_port;
  }

  public String getMongodb_db() {
    return mongodb_db;
  }

  public String getMongodb_pass() {
    return mongodb_pass;
  }

  public String getMongodb_username() {
    return mongodb_username;
  }

  public String getMongodb_coll_pool() {
    return mongodb_coll_pool;
  }

  public String getMongodb_coll_block() {
    return mongodb_coll_block;
  }

  /**
   * 暗号化する（ON）もしくはしない（OFF）を取得する
   *
   * @return cryptoStatus
   */
  public String getCryptoStatus() {
    return cryptoStatus;
  }

  /**
   * 秘密鍵のパスを取得する
   *
   * @return keyFile
   */
  public String getPrivateKeyFile() {
    return privateKeyFile;
  }

  /**
   * 公開鍵のパスを取得する
   *
   * @return keyFile
   */
  public String getPublicKeyFile() {
    return publicKeyFile;
  }
}

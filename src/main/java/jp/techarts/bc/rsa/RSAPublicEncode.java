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


package jp.techarts.bc.rsa;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import jp.techarts.bc.Common;
import jp.techarts.bc.RablockSystemException;
import jp.techarts.bc.prop.GetAppProperties;
import org.apache.commons.codec.binary.Base64;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 公開鍵での暗号化のクラス<br>
 * Copyright (c) 2018 Rablock Inc.
 *
 * @author TA
 * @version 1.0
 */
@Service
public class RSAPublicEncode {
  private static final Logger log = LoggerFactory.getLogger(RSAPublicEncode.class);

  private final Common common;

  /** 暗号の種類 */
  private static final String CIPHER_ALGORITHM = "RSA";
  /** パディング方式 */
  private static final String CIPHER_MODE = CIPHER_ALGORITHM + "/ECB/PKCS1PADDING";

  private final String publicKeyFile;

  @Autowired
  public RSAPublicEncode(final GetAppProperties config, final Common common) {
    this.common = common;
    this.publicKeyFile = config.getPublicKeyFile();
  }

  /**
   * 暗号化した文字列を返す。
   *
   * @param text 暗号化する文字列
   * @param privateKeyFilePath 公開鍵のパス
   * @return hash + result 文字列のハッシュ値と暗号化した文字列
   * @throws RablockSystemException
   */
  public String crypto(String text) throws RablockSystemException {
    try {
      final byte[] encrypted = encrypt(text.getBytes(), this.publicKeyFile);
      final String result = Base64.encodeBase64String(encrypted);
      final String hash = common.hashCal(text);
      return hash + result;
    } catch (RablockSystemException e) {
      log.warn("暗号化に失敗しました。 コード{}", 2400);
      throw e;
    }
  }

  /**
   * 暗号化した配列を返す。
   *
   * @param source 暗号化する文字列の配列
   * @param privateKeyFilePath 公開鍵のパス
   * @return result 暗号化した文字列の配列
   * @throws RablockSystemException
   */
  private byte[] encrypt(byte[] source, String publicKeyFile) throws RablockSystemException {
    try {
      byte[] keyData = readKeyFile(publicKeyFile);
      KeySpec keyspec = new X509EncodedKeySpec(keyData);
      KeyFactory keyfactory = KeyFactory.getInstance(CIPHER_ALGORITHM);
      RSAPublicKey publicKey = (RSAPublicKey) keyfactory.generatePublic(keyspec);
      Cipher cipher = Cipher.getInstance(CIPHER_MODE);
      cipher.init(Cipher.ENCRYPT_MODE, publicKey);
      return cipher.doFinal(source);
    } catch (NoSuchAlgorithmException
        | InvalidKeySpecException
        | InvalidKeyException
        | NoSuchPaddingException
        | IllegalBlockSizeException
        | BadPaddingException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * 公開鍵ファイル読み込み
   *
   * @param filePath 公開鍵のパス
   * @return data 公開鍵ファイルの内容
   * @throws RablockSystemException
   */
  private static byte[] readKeyFile(String filePath) throws RablockSystemException {
    try (FileInputStream in = new FileInputStream(filePath)) {
      byte[] data = new byte[in.available()];
      in.read(data);
      return data;
    } catch (IOException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * 配列用の暗号化
   *
   * @param nestData
   * @throws RablockSystemException
   */
  public String arrayCrypt(String arrayData) throws RablockSystemException {
    String result = "";
    JSONArray jsonArray = new JSONArray(arrayData);

    for (Object json : jsonArray) {
      Map<String, String> map = common.toMap(json.toString());
      for (Map.Entry<String, String> entry : map.entrySet()) {
        String encryptValue = this.crypto(entry.getValue());
        map.put(entry.getKey(), encryptValue);
      }
      result += common.toJson(map);
      result += ",";
    }
    result = result.substring(0, result.length() - 1);
    return "[" + result + "]";
  }

  /**
   * ネスト用の暗号化
   *
   * @param nestData
   * @throws RablockSystemException
   */
  public String nestCrypt(final String nestData) throws RablockSystemException {
    Document returnData = new Document();
    JSONArray jsonArr = new JSONArray("[" + nestData + "]");
    JSONObject array = jsonArr.getJSONObject(0);
    array
        .keySet()
        .forEach(
            key -> {
              String value = array.get(key).toString();
              if (common.nestCheck(value)) {
                returnData.append(key, nestCrypt(value));
              } else {
                value = this.crypto(value);
                returnData.append(key, value);
              }
            });
    return returnData.toJson();
  }

  /**
   * Keyが存在しない配列の暗号化
   *
   * @param arrayData
   * @return
   * @throws RablockSystemException
   */
  public String arrayNoKeyCrypt(String arrayData) throws RablockSystemException {
    String result = "";
    // TODO use jsonArray.
    JSONArray jsonArray = new JSONArray(arrayData);
    log.debug("arrayNoKeyCrypt" + arrayData);
    arrayData = arrayData.replace("[", "");
    arrayData = arrayData.replace("]", "");
    String[] array = arrayData.split(",");

    for (String s : array) {
      s = "\"" + this.crypto(s) + "\"";
      log.debug("暗号化後" + s);
    }

    result = String.join(",", array);
    log.debug("result" + result);

    return "[" + result + "]";
  }
}

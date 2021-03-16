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
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import jp.techarts.bc.RablockSystemException;
import jp.techarts.bc.prop.GetAppProperties;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 秘密鍵でのRSA復号化クラス<br>
 * Copyright (c) 2018 Technologic Arts Co.,Ltd.
 *
 * @author TA
 * @version 1.0
 */
@Service
public class RSAPrivateDecode {
  private static final Logger log = LoggerFactory.getLogger(RSAPublicEncode.class);

  private static final String CIPHER_ALGORITHM = "RSA";
  private static final String CIPHER_MODE = CIPHER_ALGORITHM + "/ECB/PKCS1PADDING";

  private final String privateKeyFile;

  @Autowired
  public RSAPrivateDecode(final GetAppProperties appProperties) {
    this.privateKeyFile = appProperties.getPrivateKeyFile();
  }
  /**
   * 復号化した文字列を返す。
   *
   * @param text 暗号化された文字列
   * @param privateKeyFilePath 秘密鍵のパス
   * @return result 復号化した文字列
   * @throws RablockSystemException
   */
  public String decrypt(String text) throws RablockSystemException {
    byte[] byteText = Base64.decodeBase64(text);
    byte[] decrypted = decrypt(byteText, this.privateKeyFile);
    return new String(decrypted);
  }

  /**
   * 暗号化した配列を返す。
   *
   * @param source 暗号化された文字列の配列
   * @param privateKeyFilePath 秘密鍵のパス
   * @return result 暗号化された文字列の配列
   * @throws RablockSystemException
   */
  public byte[] decrypt(byte[] source, String privateKeyFile) throws RablockSystemException {
    try {
      byte[] keyData = readKeyFile(privateKeyFile);
      KeySpec keyspec = new PKCS8EncodedKeySpec(keyData);
      KeyFactory keyfactory = KeyFactory.getInstance(CIPHER_ALGORITHM);
      Key privateKey = keyfactory.generatePrivate(keyspec);
      Cipher cipher = Cipher.getInstance(CIPHER_MODE);
      cipher.init(Cipher.DECRYPT_MODE, privateKey);
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
   * 秘密鍵ファイル読み込み
   *
   * @param filePath 秘密鍵のパス
   * @return data 秘密鍵ファイルの内容
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

  public static String toHexString(byte[] array) {
    return Stream.of(array).map(b -> String.format("%02X", b)).collect(Collectors.joining(" "));
  }
}

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

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.crypto.Cipher;
import org.apache.commons.codec.binary.Base64;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RSAPublicEncodeTest {
  @Autowired private RSAPublicEncode rsa;

  private static final String CIPHER_ALGORITHM = "RSA";
  private static final String CIPHER_MODE = CIPHER_ALGORITHM + "/ECB/PKCS1PADDING";
  // 公開鍵
  private static final String PUBLIC_KEY_FILE = "/Users/Tashiro/Documents/public_key.der";

  @Test
  @Ignore
  public void 暗号化テスト() throws Exception {
    String crypto = rsa.crypto("test");
    String fukugo = fukugo(crypto);
    assertEquals("test", fukugo);
  }

  //	@Test(expected = IOException.class)
  //	public void 暗号化ファイルがないとき異常終了() throws Exception{
  //		rsa.crypto("test", "");
  //	}

  public String fukugo(String st) throws Exception {

    byte[] byteText = Base64.decodeBase64(st);

    byte[] decrypted = decrypt(byteText);
    return new String(decrypted);
  }

  public byte[] decrypt(byte[] source) throws Exception {
    byte[] keyData = readKeyFile(PUBLIC_KEY_FILE);
    KeySpec keyspec = new X509EncodedKeySpec(keyData);
    KeyFactory keyfactory = KeyFactory.getInstance(CIPHER_ALGORITHM);
    Key privateKey = keyfactory.generatePublic(keyspec);
    Cipher cipher = Cipher.getInstance(CIPHER_MODE);
    cipher.init(Cipher.DECRYPT_MODE, privateKey);
    return cipher.doFinal(source);
  }

  private static byte[] readKeyFile(String filename) throws IOException {
    try (FileInputStream in = new FileInputStream(filename)) {
      byte[] data = new byte[in.available()];
      in.read(data);
      return data;
    }
  }

  public static String toHexString(byte[] array) {
    return Stream.of(array).map(b -> String.format("%02X", b)).collect(Collectors.joining(" "));
  }
}

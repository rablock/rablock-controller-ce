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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 他ノードにJSON文字列を送信するクラス<br>
 * Copyright (c) 2018 Rablock Inc.
 *
 * @author TA
 * @version 1.0
 */
@Service
public class HttpSendJSON {
  private final Logger log = LoggerFactory.getLogger(HttpSendJSON.class);

  /**
   * JSON文字列の送信
   *
   * @param strPostUrl 送信先URL
   * @param JSON 送信するJSON文字列
   * @return result 通信結果 成功：OK 失敗：NG
   * @throws IOException
   */
  public String callPost(
      String strPostUrl, String JSON, String digest_username, String digest_pass) {
    StringBuffer result = new StringBuffer();
    try {
      URL url = new URL(strPostUrl);

      // ユーザ認証情報の設定
      HttpAuthenticator httpAuth = new HttpAuthenticator(digest_username, digest_pass);
      Authenticator.setDefault(httpAuth);

      HttpURLConnection con = (HttpURLConnection) url.openConnection();
      try (AutoCloseable conc = () -> con.disconnect()) {
        con.setReadTimeout(100000);
        con.setConnectTimeout(100000);
        // HTTPリクエストコード
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        con.setRequestProperty("Accept-Language", "jp");
        // データがJSONであること、エンコードを指定する
        con.setRequestProperty("Content-Type", "application/JSON; charset=utf-8");
        // POSTデータの長さを設定
        con.setRequestProperty("Content-Length", String.valueOf(JSON.length()));
        // リクエストのbodyにJSON文字列を書き込む
        OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
        out.write(JSON);
        out.flush();
        con.connect();

        // HTTPレスポンスコード
        final int status = con.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
          // 通信に成功した
          // テキストを取得する
          final InputStream in = con.getInputStream();
          String encoding = con.getContentEncoding();
          if (null == encoding) {
            encoding = "UTF-8";
          }
          try (final InputStreamReader inReader = new InputStreamReader(in, encoding);
              final BufferedReader bufReader = new BufferedReader(inReader); ) {
            result.append(bufReader.lines().collect(Collectors.joining()));
          }
        } else {
          // 通信が失敗した場合のレスポンスコードを表示
          log.warn("通信エラー status:" + status + " コード{}", 1005);
          result.append("{\"result\" : \"NG\"}");
        }
      }
    } catch (Exception e) {
      log.error("通信エラー", e);
      result.append("{\"result\" : \"NG\"}");
    }
    return result.toString();
  }
}

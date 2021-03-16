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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 共通処理の定義クラス<br>
 * Copyright (c) 2018 Rablock Inc.
 *
 * @author TA
 * @version 1.0
 */
@Service
public class Common {
  private final Logger log = LoggerFactory.getLogger(Common.class);
  private static final DateTimeFormatter dateTimeFormatter =
      DateTimeFormatter.ofPattern("yyyy/[]M/[]d []H:[]m:[]s");
  private static final DateTimeFormatter compactFormat =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  /**
   * 現在時刻を取得する
   *
   * @return currentTime 現在時刻
   * @throws jp.techarts.bc.RablockSystemException
   */
  public String getCurrentTime() throws RablockSystemException {
    try {
      return dateTimeFormatter.format(LocalDateTime.now());
    } catch (DateTimeParseException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * タイムスタンプを取得する
   *
   * @return Timestamp タイムスタンプ
   * @throws jp.techarts.bc.RablockSystemException
   */
  public String getTimestamp() throws RablockSystemException {
    String time = "";
    try {
      Timestamp timestamp = new Timestamp(System.currentTimeMillis());
      time = String.valueOf(timestamp.getTime());
    } catch (Exception e) {
      throw new RablockSystemException("システムエラー", e);
    }
    return time;
  }

  /**
   * 文字列をハッシュ化する
   *
   * @param input ハッシュ化する文字列
   * @return result ハッシュ化した文字列
   * @throws RablockSystemException
   */
  public String hashCal(String input) throws RablockSystemException {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
      digest.reset();
      digest.update(input.getBytes("utf8"));
      return String.format("%040x", new BigInteger(1, digest.digest()));
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
      throw new RablockSystemException("システムエラー", e);
    }
  }

  /**
   * Jsonファイル読み込み、String型に変換する
   *
   * @param file 受信ファイル
   * @return json JSON形式
   * @throws IOException ファイル読み込み時に発生
   */
  public String readFile(SendDataForm file) throws IOException {
    String json = "";

    try (BufferedReader br =
        new BufferedReader(new InputStreamReader(file.getUpload_file().getInputStream()))) {
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line);
      }
      json = sb.toString();
    }
    return json;
  }

  /**
   * CSVファイル読み込み、String配列型で返す
   *
   * @param csvFile 受信ファイル
   * @return csvArray 配列
   * @throws IOException ファイル読み込み時に発生
   */
  public List<String> readCSVFile(SendDataForm csvFile) throws IOException {
    List<String> csvArray = new ArrayList<String>();

    try (BufferedReader br =
        new BufferedReader(new InputStreamReader(csvFile.getUpload_file().getInputStream()))) {
      String line;
      while ((line = br.readLine()) != null) {
        csvArray.add(line);
      }
    }

    return csvArray;
  }

  /**
   * 処理に失敗したデータを書き込み出力する
   *
   * @param errorCSVList 処理に失敗したデータのリスト
   * @param path 出力パス
   * @throws FileNotFoundException ファイルが見つからない場合に発生
   * @throws UnsupportedEncodingException 文字のエンコーディングがサポートされていない場合に発生
   */
  public void outputErrorFile(List<String> errorCSVList, String path)
      throws UnsupportedEncodingException, FileNotFoundException {
    try (PrintWriter pw =
        new PrintWriter(
            new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "utf-8")))) {
      errorCSVList.forEach(s -> pw.println(s));
    }
    log.debug("出力が完了しました。");
  }

  /**
   * ファイル名から時間を取得する
   *
   * @param fileName ファイル名
   * @return tx_time 取引時間
   * @throws NumberFormatException Long型に変換時に発生
   * @throws ArrayIndexOutOfBoundsException 不正なインデックスを使って配列がアクセスされた場合に発生
   * @throws ParseException ファイル名がyyyyMMddHHmmssでない時に発生
   */
  public String getTxTimeFromFileName(String fileName)
      throws NumberFormatException, ArrayIndexOutOfBoundsException {
    String[] fileArray = fileName.split("_");

    String time = fileArray[2];

    // ファイル名チェック
    String[] array = time.split(Pattern.quote("."));
    String fileTime = array[0];
    Long.parseLong(fileTime);
    if (fileTime.length() != 14) {
      log.info("ファイル名の桁数が不正です");
      return "NG";
    }

    LocalDateTime sdf = LocalDateTime.parse(time, compactFormat);
    return dateTimeFormatter.format(sdf);
  }

  /**
   * 送信されたJsonの項目名をリストに入れて返却
   *
   * @param json
   * @return itemList 項目名リスト (null if JSON was broken)
   * @throws jp.techarts.bc.RablockSystemException
   */
  public List<String> getItemList(String json)
      throws RablockSystemException, JsonProcessingException {
    try {
      List<String> itemList = new ArrayList<String>();
      ObjectMapper mapper = new ObjectMapper();
      Map<String, String> map = mapper.readValue(json, Map.class);
      for (Entry<String, String> entity : map.entrySet()) {
        String key = entity.getKey();
        itemList.add(key);
      }
      return itemList;
    } catch (JsonProcessingException e) {
      log.warn("データ形式が不正です。:" + json + " コード{}", 2300);
      throw e;
    }
  }

  public String decode(String value) {
    URLCodec codec = new URLCodec("UTF-8");
    try {
      value = codec.decode(value, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    } catch (DecoderException e) {
      e.printStackTrace();
    }
    return value;
  }

  /**
   * ネストのチェック("{"で始まる場合をネストと判断する)
   *
   * @param data
   * @return
   */
  public boolean nestCheck(String data) {
    return data.startsWith("{");
  }
  /**
   * 配列のチェック("["で始まる場合を配列と判断する)
   *
   * @param data
   * @return
   */
  public boolean arrayCheck(String data) {
    return data.startsWith("[");
  }

  /**
   * 配列にKeyが存在するかチェック
   *
   * @param abnormalData
   * @return true:存在する false:存在しない
   */
  public boolean arrayKeyCheck(String abnormalData) {
    return abnormalData.contains(":");
  }
  /**
   * MapをJson形式に変換
   *
   * @param map
   * @return
   */
  public String toJson(Map<String, String> map) {
    String json = "";
    ObjectMapper mapper = new ObjectMapper();
    try {
      json = mapper.writeValueAsString(map);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return json;
  }

  /**
   * Json形式をMapに変換
   *
   * @param json
   * @return
   */
  public Map<String, String> toMap(String json) {
    Map<String, String> map = new LinkedHashMap<>();
    ObjectMapper mapper = new ObjectMapper();
    try {
      map = mapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {});
    } catch (Exception e) {
      e.printStackTrace();
    }
    return map;
  }

  /**
   * ネストデータ処理
   *
   * @param getData
   * @return
   */
  public Document nestProcess(String getData, Document nestData) {

    getData = "[" + getData + "]";
    JSONArray jsonArr = new JSONArray(getData);
    JSONObject array = jsonArr.getJSONObject(0);
    Document createNestData = new Document();
    array
        .keySet()
        .forEach(
            key -> {
              String value = array.get(key).toString();

              if (!nestCheck(value)) {
                nestData.append(key, value);
              } else {
                nestData.append(key, nestProcess(value, createNestData));
              }
            });
    return nestData;
  }
}

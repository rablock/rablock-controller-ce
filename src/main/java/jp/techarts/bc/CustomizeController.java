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

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * コントローラークラス<br>
 * Copyright (c) 2018-2020 Rablock Inc.
 *
 * @author TA
 * @version 1.0
 */
@RestController
@RequestMapping("/check")
public class CustomizeController {
  private static Logger log = LoggerFactory.getLogger(CustomizeController.class);
  private static final DateTimeFormatter dateTimeFormatter =
      DateTimeFormatter.ofPattern("yyyy/[]M/[]d []H:[]m:[]s");

  private final PoolService poolService;
  private final BlockService blockService;

  @Autowired
  public CustomizeController(final PoolService poolService, final BlockService blockService) {
    this.poolService = poolService;
    this.blockService = blockService;
  }

  /**
   * 当日の履歴があるかチェックする
   *
   * @param user_id
   * @param shop_id
   * @return OK:登録可 NG:制限
   * @throws RablockSystemException
   */
  @RequestMapping("/daylimit/{user_id}/{shop_id}")
  public String checkDayLimit(@PathVariable String user_id, @PathVariable String shop_id)
      throws RablockSystemException {
    // ユーザーの履歴を取得
    List<Document> blockDataList = blockService.getByKeyValue("user_id", user_id);
    List<Document> poolList = poolService.getByKeyStringValue("user_id", user_id);

    blockDataList.addAll(poolList);

    Boolean hasNG =
        blockDataList
            .parallelStream()
            .map(
                obj -> {
                  if (obj.get("shop").equals(shop_id)) {
                    String date = obj.get("settime", String.class);
                    // 同じ店舗があったとき日付をチェックする
                    if (equalToday(date)) {
                      // 今日の日付があったらNGを返却
                      return true;
                    }
                  }
                  return false;
                })
            .filter(x -> x == true)
            .findFirst()
            .orElse(false);
    return hasNG ? "NG" : "OK";
  }

  @RequestMapping("/eventlimit/{user_id}/{shop_id}")
  public String getEventLimit(@PathVariable String user_id, @PathVariable String shop_id)
      throws RablockSystemException {
    // ユーザーの履歴を取得
    List<Document> blockDataList = blockService.getByKeyValue("user_id", user_id);
    List<Document> poolList = poolService.getByKeyStringValue("user_id", user_id);

    blockDataList.addAll(poolList);

    boolean hasNG =
        blockDataList
            .parallelStream()
            .map(obj -> obj.get("shop").equals(shop_id))
            .filter(x -> x == true)
            .findFirst()
            .orElse(false);
    return hasNG ? "NG" : "OK";
  }

  /**
   * 今日の日付と等しいか確認
   *
   * @param strDate
   * @return true 等しい false 異なる
   */
  public static boolean equalToday(String strDate) {
    Date date = toDate(LocalDateTime.parse(strDate, dateTimeFormatter));
    Date currentTime = toDate(LocalDateTime.now());

    date = truncate(date);
    currentTime = truncate(currentTime);

    return date.equals(currentTime);
  }

  /**
   * 日時の「時刻」を切り捨てる
   *
   * @param datetime 日時
   * @return 時刻を切り捨てた日付
   */
  public static Date truncate(Date datetime) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(datetime);

    return new GregorianCalendar(
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE))
        .getTime();
  }

  /**
   * Date型に変換する
   *
   * @param localDateTime
   * @return
   */
  public static Date toDate(LocalDateTime localDateTime) {
    ZoneId zone = ZoneId.systemDefault();
    ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, zone);
    Instant instant = zonedDateTime.toInstant();
    return Date.from(instant);
  }

  @ExceptionHandler(RablockSystemException.class)
  @ResponseStatus(code = INTERNAL_SERVER_ERROR)
  public Map<String, String> handleSystemException(final RablockSystemException e) {
    log.error(e.getMessage(), e);
    return Collections.singletonMap("message", "invalid");
  }
}

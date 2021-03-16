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

import java.io.Serializable;
import org.springframework.web.multipart.MultipartFile;

/**
 * ファイルを受け取るためのFormクラス<br>
 * Copyright (c) 2018 Rablock Inc.
 *
 * @author TA
 * @version 1.0
 */
public class SendDataForm implements Serializable {

  /** シリアルバージョンUID */
  private static final long serialVersionUID = 1L;

  /** 受信したファイル */
  private MultipartFile upload_file;

  /**
   * 受信したファイルを取得する。
   *
   * @return upload_file 受信したファイル
   */
  public MultipartFile getUpload_file() {
    return upload_file;
  }

  /**
   * 受信したファイルを設定する。
   *
   * @param upload_file 受信したファイル
   */
  public void setUpload_file(MultipartFile upload_file) {
    this.upload_file = upload_file;
  }

  public String toString() {
    String str = "";

    if (null != this.upload_file) {
      str +=
          "upload_file = "
              + this.upload_file.getOriginalFilename()
              + " size = "
              + this.upload_file.getSize()
              + "\n";
    }

    return str;
  }
}

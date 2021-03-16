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


package jp.techarts.bc.test;

public class SampleData {

  private String oid;
  private String type;
  private String user_id;
  private String original_id;
  private boolean deliveryF;
  private String testItem1;

  public String getOid() {
    return oid;
  }

  public void setOid(String oid) {
    this.oid = oid;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getUser_id() {
    return user_id;
  }

  public void setUser_id(String user_id) {
    this.user_id = user_id;
  }

  public String getOriginal_id() {
    return original_id;
  }

  public void setOriginal_id(String original_id) {
    this.original_id = original_id;
  }

  public String getTestItem1() {
    return testItem1;
  }

  public void setTestItem1(String testItem1) {
    this.testItem1 = testItem1;
  }

  public boolean isDeliveryF() {
    return deliveryF;
  }

  public void setDeliveryF(boolean deliveryF) {
    this.deliveryF = deliveryF;
  }
}

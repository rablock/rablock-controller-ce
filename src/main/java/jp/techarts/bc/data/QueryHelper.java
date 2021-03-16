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


package jp.techarts.bc.data;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.or;

import jp.techarts.bc.constitem.ConstType;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Component;

@Component
public class QueryHelper {

  public Bson getDBObjectDeleteOrModify(String key) {
    final Bson dataTypeEqDelete = eq(key, ConstType.DELETE);
    final Bson dataTypeEqModify = eq(key, ConstType.MODIFY);
    return or(dataTypeEqDelete, dataTypeEqModify);
  }

  public Bson getDBObjectNewOrModify(String key) {
    final Bson dataTypeEqDelete = eq(key, ConstType.NEW);
    final Bson dataTypeEqModify = eq(key, ConstType.MODIFY);
    return or(dataTypeEqDelete, dataTypeEqModify);
  }
}

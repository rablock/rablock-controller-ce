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


package jp.techarts.bc.ip;

import jp.techarts.bc.prop.GetAppProperties;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class IpConfigTest {
  @Autowired GetAppProperties ip;

  @Before
  public void initialize() throws InterruptedException {
    Thread.sleep(250);
  }

  @Test
  @Ignore
  public void プロパティファイルからの値取得テスト() {
    //		String sendIp = ip.getSendIpAddress();
    //		String secondIp = ip.getSendIpSecondAddress();
    //
    //		assertEquals(sendIp, "10.190.0.210");
    //		assertEquals(secondIp, "10.190.0.229");

  }
}

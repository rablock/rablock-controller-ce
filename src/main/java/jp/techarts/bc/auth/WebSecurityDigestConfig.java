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


package jp.techarts.bc.auth;

import jp.techarts.bc.prop.GetAppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/** ダイジェスト認証 */
@EnableWebSecurity
@Configuration
public class WebSecurityDigestConfig extends WebSecurityConfigurerAdapter {
  private final Logger log = LoggerFactory.getLogger(WebSecurityDigestConfig.class);

  private final String username;

  private final String password;

  public static final String REALM_NAME = "BLOCKCHAIN";

  @Autowired
  WebSecurityDigestConfig(final GetAppProperties app) {
    this.username = app.getDigestUserName();
    this.password = "{noop}" + app.getDigestPass();
  }

  @Bean
  @Override
  public UserDetailsService userDetailsService() {
    UserDetails userDetails = User.withUsername(username).password(password).roles("USER").build();
    InMemoryUserDetailsManager userDetailsManager = new InMemoryUserDetailsManager();
    userDetailsManager.createUser(userDetails);
    return userDetailsManager;
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {

    // CSRF無効
    http.csrf()
        .disable()
        // すべてのサービスコールにダイジェスト認証
        .authorizeRequests()
        .anyRequest()
        .authenticated()
        .and()
        .httpBasic()
        .and()
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
  }
}

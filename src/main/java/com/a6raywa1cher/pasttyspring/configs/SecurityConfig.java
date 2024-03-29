package com.a6raywa1cher.pasttyspring.configs;

import com.a6raywa1cher.pasttyspring.configs.security.JwtSecurityTokenService;
import com.a6raywa1cher.pasttyspring.configs.security.TokenOncePerRequestFilter;
import com.a6raywa1cher.pasttyspring.rest.ControllerValidations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletResponse;

@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	private JwtSecurityTokenService securityTokenService;
	private AuthenticationProvider provider;

	@Autowired
	public SecurityConfig(JwtSecurityTokenService tokenService, AuthenticationProvider provider) {
		this.securityTokenService = tokenService;
		this.provider = provider;
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.authenticationProvider(provider);
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
				.csrf().disable()
				.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				.and()
				.exceptionHandling().authenticationEntryPoint((req, rsp, e) -> rsp.sendError(HttpServletResponse.SC_UNAUTHORIZED))
				.and()
				// Add a filter to validate the tokens with every request
				.addFilterAfter(new TokenOncePerRequestFilter(securityTokenService, provider), UsernamePasswordAuthenticationFilter.class)
				// authorization requests config
				.authorizeRequests()
				.antMatchers(HttpMethod.GET, "/actuator/**").hasRole("ADMIN")
				.antMatchers("/v2/api-docs", "/webjars/**", "/swagger-resources", "/swagger-resources/**", "/swagger-ui.html").permitAll()
//				.antMatchers("/ws/file_agent").authenticated()
				.antMatchers("/script/**").permitAll()
				.antMatchers("/comment/**").permitAll()
				.antMatchers(HttpMethod.GET, "/user/*").permitAll()
				.antMatchers(HttpMethod.POST, "/script/s/{" + ControllerValidations.SCRIPT_NAME_REGEX + "}/exec").hasRole("USER")
				// allow all who are accessing "auth" and "user" service
				.antMatchers(HttpMethod.POST, "/auth/login", "/auth/get_access", "/user/reg").permitAll()
				.antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.anyRequest().authenticated();
	}
}

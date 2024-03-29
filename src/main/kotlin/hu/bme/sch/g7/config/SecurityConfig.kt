package hu.bme.sch.g7.config

import hu.bme.sch.g7.model.RoleType
import hu.gerviba.authsch.AuthSchAPI
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource


@EnableWebSecurity
@Configuration
open class SecurityConfig : WebSecurityConfigurerAdapter() {

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http.authorizeRequests()
                .antMatchers("/", "/loggedin", "/login", "/logged-out", "/api/news", "/api/events",
                        "/api/events/**", "/api/products", "/api/extra-page/**", "/api/version", "/style.css",
                        "/images/**", "/js/**", "/files/**", "/admin/logout", "/cdn/profiles/**", "/cdn/events/**", "/cdn/news/**",
                        "/countdown", "/logout", "/test", "/api/profile/profile/**", "/api/achievement/**", "/open-site")
                    .permitAll()

                .antMatchers("/api/debts", "/entrypoint", "/pay", "/export-bucketlist", "/cdn/achievement/**")
                    .hasAnyRole(RoleType.BASIC.name, RoleType.STAFF.name, RoleType.ADMIN.name, RoleType.SUPERUSER.name)

                .antMatchers("/admin/**", "/cdn/**")
                    .hasAnyRole(RoleType.STAFF.name, RoleType.ADMIN.name, RoleType.SUPERUSER.name)

                .and()
                .formLogin()
                .loginPage("/login")
        http.csrf().ignoringAntMatchers("/api/**", "/admin/sell/**")
    }

    override fun configure(auth: AuthenticationManagerBuilder?) {
        auth?.eraseCredentials(true)
    }

    @Bean
    @ConfigurationProperties(prefix = "authsch")
    fun authSchApi(): AuthSchAPI {
        return AuthSchAPI()
    }
}

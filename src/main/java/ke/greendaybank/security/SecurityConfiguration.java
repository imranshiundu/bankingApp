package ke.greendaybank.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    private final StaffTokenAuthenticationFilter staffTokenAuthenticationFilter;

    public SecurityConfiguration(StaffTokenAuthenticationFilter staffTokenAuthenticationFilter) {
        this.staffTokenAuthenticationFilter = staffTokenAuthenticationFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/health").permitAll()
                        .requestMatchers("POST", "/api/v1/accounts").hasAuthority(Permission.ACCOUNT_OPEN)
                        .requestMatchers("POST", "/api/v1/ledger/credits").hasAuthority(Permission.LEDGER_CREDIT)
                        .requestMatchers("POST", "/api/v1/ledger/movements").hasAuthority(Permission.LEDGER_MOVE)
                        .requestMatchers("GET", "/api/v1/accounts/*/balance").hasAuthority(Permission.STATEMENT_READ)
                        .requestMatchers("GET", "/api/v1/accounts/*/statement").hasAuthority(Permission.STATEMENT_READ)
                        .requestMatchers("GET", "/api/v1/high-risk-movements/pending").hasAuthority(Permission.APPROVAL_REVIEW)
                        .requestMatchers("POST", "/api/v1/high-risk-movements/*/approve").hasAuthority(Permission.APPROVAL_REVIEW)
                        .requestMatchers("POST", "/api/v1/high-risk-movements/*/reject").hasAuthority(Permission.APPROVAL_REVIEW)
                        .requestMatchers("POST", "/api/v1/high-risk-movements/*/execute").hasAuthority(Permission.APPROVAL_EXECUTE)
                        .requestMatchers("/api/v1/approvals/**").hasAuthority(Permission.APPROVAL_REVIEW)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(staffTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}

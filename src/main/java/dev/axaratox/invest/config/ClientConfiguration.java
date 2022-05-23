package dev.axaratox.invest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.tinkoff.piapi.core.InvestApi;

@Configuration
public class ClientConfiguration {

    @Bean
    public InvestApi getInvestApi(@Value("${tinkoff.api.token}") final String token) {
        return InvestApi.create(token);
    }
}

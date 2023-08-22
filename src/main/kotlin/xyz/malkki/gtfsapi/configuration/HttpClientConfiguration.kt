package xyz.malkki.gtfsapi.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.http.HttpClient

@Configuration
class HttpClientConfiguration {
    @Bean
    fun httpClient(): HttpClient {
        return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
    }
}
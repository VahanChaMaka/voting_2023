package election.monitoring.nizhny_novgorod_2023

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestTemplate

@EnableScheduling
@SpringBootApplication
class NizhnyNovgorod2023Application {
    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }
}


fun main(args: Array<String>) {
    runApplication<NizhnyNovgorod2023Application>(*args)
}


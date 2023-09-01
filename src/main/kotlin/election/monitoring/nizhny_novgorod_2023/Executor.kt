package election.monitoring.nizhny_novgorod_2023

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.*


@Component
class Executor(val restTemplate: RestTemplate) {

    private val contracts = listOf(
        "EBFgDvpUpAzpkCieyS6nC2HJquqpraScTmgQTzV6taQQ",
        "t5H68ncU3zh1KpYtdfY2VuKLzj5RdVjG7mKvJcTjPgn"
    )
    private val mapper = ObjectMapper()

    @Scheduled(fixedRate = 600_000)
    fun execute() {

        val date = DateTimeFormatter.ofPattern("dd.MM.yyyy - HH.mm.ss")
            .format(ZonedDateTime.now(ZoneId.of("Europe/Moscow")))

        File("data/$date").mkdir()

        runBlocking {
            for (contractId in contracts) {
                launch(Dispatchers.Default) {
                    val baseFolder = "data/$date/$contractId"
                    File(baseFolder).mkdir()

                    transactionList(baseFolder)
                    uikList(baseFolder, contractId)
                }
            }

            launch {
                blockList("data/$date")
            }
        }

    }

    private fun transactionList(baseFolder: String) {
        File("$baseFolder/transactions").mkdir()
        val url = "https://stat.vybory.gov.ru/api/transactions"
        val response = getResponse(url)
        writeFile(baseFolder, "transactions/list.json", response)

        val jsonResponse = mapper.readTree(response)
        val transactions = jsonResponse.get("data").get("transactions")
        for (i in 0 until transactions.size()) {
            val transactionId = transactions[i].get("id").asText()
            val transaction =
                getResponse("https://stat.vybory.gov.ru/api/transactions/$transactionId")
            writeFile(baseFolder, "transactions/$transactionId.json", transaction)
        }
    }

    private fun uikList(baseFolder: String, contractId: String) {
        File("$baseFolder/UIKs").mkdir()
        val url = "https://stat.vybory.gov.ru/api/statistics/voting/uiks/search"
        val entity = HttpEntity(
            """
            {
                "page": 0,
                "pageSize": 5000,
                "orderBy": [
                    {
                        "sortDirection": "ASC",
                        "field": "primaryUikNumber"
                    }
                ],
                "contractId": "$contractId",
                "primaryUikNumbers": null
            }
        """.trimIndent(), getHeaders()
        )
        val response =
            restTemplate.exchange(url, HttpMethod.POST, entity, String::class.java).body ?: ""
        writeFile(baseFolder, "UIKs/list.json", response)
    }

    private fun blockList(baseFolder: String) {
        File("$baseFolder/blocks").mkdir()
        val url = "https://stat.vybory.gov.ru/api/blocks/search"
        val pageSize = 20000
        var page = 0
        while (true) {
            val entity = HttpEntity(
                """
                    {
                        "page": $page,
                        "pageSize": $pageSize
                    }
                 """.trimIndent(), getHeaders()
            )
            val response =
                restTemplate.exchange(url, HttpMethod.POST, entity, String::class.java).body ?: ""
            writeFile(baseFolder, "blocks/$page.json", response)
            page++
            if (mapper.readTree(response).get("data").get("results").size() < pageSize) break
        }
    }

    private fun getResponse(url: String): String {
        val entity = HttpEntity(null, getHeaders())
        return restTemplate.exchange(url, HttpMethod.GET, entity, String::class.java).body ?: ""
    }

    private fun getHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set("User-Agent", "Mozilla/5.0")
        return headers
    }

    private fun writeFile(baseFolder: String, fileName: String, content: String) =
        File("${baseFolder}/$fileName").writeText(content)

}
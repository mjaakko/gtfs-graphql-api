package xyz.malkki.gtfsapi.configuration.graphql

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import graphql.execution.preparsed.PreparsedDocumentEntry
import org.springframework.boot.autoconfigure.graphql.GraphQlSourceBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.GraphQlSource.SchemaResourceBuilder

@Configuration(proxyBeanMethods = false)
class GraphQlConfiguration {
    @Bean
    fun queryCaching(): GraphQlSourceBuilderCustomizer {
        val cache: Cache<String, PreparsedDocumentEntry> = Caffeine.newBuilder().maximumSize(10_000).build();

        return GraphQlSourceBuilderCustomizer { builder: SchemaResourceBuilder ->
            builder.configureGraphQl { graphQlBuilder ->
                graphQlBuilder.preparsedDocumentProvider { executionInput, parseAndValidateFunction ->
                    cache.get(executionInput.query) { parseAndValidateFunction.apply(executionInput) }
                }
            }
        }
    }
}


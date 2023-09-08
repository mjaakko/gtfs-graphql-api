package xyz.malkki.gtfsapi.configuration.graphql

import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import graphql.schema.GraphQLScalarType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.RuntimeWiringConfigurer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

@Configuration
class ScalarConfiguration {
    @Bean
    fun dateScalar(): GraphQLScalarType {
        return GraphQLScalarType.newScalar()
            .name("Date")
            .coercing(object : Coercing<LocalDate, String> {
                override fun serialize(dataFetcherResult: Any, graphQLContext: GraphQLContext, locale: Locale): String {
                    return (dataFetcherResult as? LocalDate)?.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        ?: throw CoercingSerializeException("Expected a LocalDate object")
                }

                override fun parseValue(input: Any, graphQLContext: GraphQLContext, locale: Locale): LocalDate {
                    return try {
                        if (input is CharSequence) {
                            LocalDate.parse(input, DateTimeFormatter.ISO_LOCAL_DATE)
                        } else {
                            throw CoercingParseValueException("Expected a CharSequence")
                        }
                    } catch (e: DateTimeParseException) {
                        throw CoercingParseValueException("Invalid date: $input", e)
                    }
                }

                override fun parseLiteral(
                    input: Value<*>,
                    variables: CoercedVariables,
                    graphQLContext: GraphQLContext,
                    locale: Locale
                ): LocalDate {
                    return if (input is StringValue) {
                        try {
                            LocalDate.parse(input.value, DateTimeFormatter.ISO_LOCAL_DATE)
                        } catch (e: DateTimeParseException) {
                            throw CoercingParseLiteralException(e)
                        }
                    } else {
                        throw CoercingParseLiteralException("Expected a StringValue")
                    }
                }
            })
            .build()
    }

    @Bean
    fun timestampScalar(): GraphQLScalarType {
        return GraphQLScalarType.newScalar()
            .name("Timestamp")
            .coercing(object : Coercing<OffsetDateTime, String> {
                override fun serialize(dataFetcherResult: Any, graphQLContext: GraphQLContext, locale: Locale): String {
                    return (dataFetcherResult as? OffsetDateTime)?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        ?: throw CoercingSerializeException("Expected a OffsetDateTime object")
                }

                override fun parseValue(input: Any, graphQLContext: GraphQLContext, locale: Locale): OffsetDateTime {
                    return try {
                        if (input is CharSequence) {
                            OffsetDateTime.parse(input, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        } else {
                            throw CoercingParseValueException("Expected a CharSequence")
                        }
                    } catch (e: DateTimeParseException) {
                        throw CoercingParseValueException("Invalid timestamp: $input", e)
                    }
                }

                override fun parseLiteral(
                    input: Value<*>,
                    variables: CoercedVariables,
                    graphQLContext: GraphQLContext,
                    locale: Locale
                ): OffsetDateTime {
                    return if (input is StringValue) {
                        try {
                            OffsetDateTime.parse(input.value, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        } catch (e: DateTimeParseException) {
                            throw CoercingParseLiteralException(e)
                        }
                    } else {
                        throw CoercingParseLiteralException("Expected a StringValue")
                    }
                }
            })
            .build()
    }

    @Bean
    fun runtimeWiringConfigurer(scalarTypes: List<GraphQLScalarType>): RuntimeWiringConfigurer {
        return RuntimeWiringConfigurer { initialBuilder ->
            scalarTypes.fold(initialBuilder) { builder, scalarType ->
                builder.scalar(scalarType)
            }
        }
    }
}
package gmbh.wapp.stripper

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.media.*
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse
import java.util.*

typealias Visitor = (Schema<*>) -> Unit
private typealias VisitedFun = (Schema<*>) -> Boolean

fun OpenAPI.visitAllSchemas(
    visitor: Visitor
) {
    val marker = Any()
    val visited = IdentityHashMap<Any, Any>()
    val visFun: VisitedFun = { schema -> visited.put(schema, marker) == null }

    paths?.forEach { (_, pathItem) -> pathItem.visit(this, visFun, visitor) }
    components?.parameters?.forEach { (_, parameter) -> parameter.visit(this, visFun, visitor) }
    components?.responses?.forEach { (_, response) -> response.visit(this, visFun, visitor) }
}

fun OpenAPI.visitAllResponses(
    visitor: (String, ApiResponse) -> Unit
) {
    paths?.forEach { (_, pathItem) ->
        pathItem.readOperations().forEach { operation ->
            operation.responses?.forEach { (code, apiResponse) ->
                visitor(code, apiResponse)
            }
        }
    }
}

fun OpenAPI.visitAllParameters(
    visitor: (Parameter) -> Unit
) {
    paths?.forEach { (_, pathItem) ->
        pathItem.parameters?.forEach { param -> visitor(param) }
        pathItem.readOperations().forEach { operation ->
            operation.parameters?.forEach { param -> visitor(param) }
        }
    }
}

private fun PathItem.visit(openAPI: OpenAPI, visFun: VisitedFun, visitor: Visitor) {
    parameters?.forEach { param -> param.visit(openAPI, visFun, visitor) }
    readOperations().forEach { op -> op.visit(openAPI, visFun, visitor) }
}

private fun Parameter.visit(openAPI: OpenAPI, visFun: VisitedFun, visitor: Visitor) {
    schema?.visit(openAPI, visFun, visitor)
    content?.visit(openAPI, visFun, visitor)
}

private fun Operation.visit(openAPI: OpenAPI, visFun: VisitedFun, visitor: Visitor) {
    parameters?.forEach { param -> param.visit(openAPI, visFun, visitor) }
    requestBody?.content?.visit(openAPI, visFun, visitor)
    responses?.forEach { (_, response) -> response.visit(openAPI, visFun, visitor) }
}

private fun Content.visit(openAPI: OpenAPI, visFun: VisitedFun, visitor: Visitor) {
    forEach { (_, mediaType) -> mediaType.schema?.visit(openAPI, visFun, visitor) }
}

private fun ApiResponse.visit(openAPI: OpenAPI, visFun: VisitedFun, visitor: Visitor) {
    content?.visit(openAPI, visFun, visitor)
    headers?.forEach { (_, header) -> header.visit(openAPI, visFun, visitor) }
}

private fun Header.visit(openAPI: OpenAPI, visFun: VisitedFun, visitor: Visitor) {
    schema?.visit(openAPI, visFun, visitor)
    content?.visit(openAPI, visFun, visitor)
}

private fun Schema<*>.visit(openAPI: OpenAPI, visFun: VisitedFun, visitor: Visitor) {
    if (!visFun(this)) {
        return
    }
    visitor(this)

    val refType = getRefTypeName()
    if (refType != null) {
        openAPI.components?.schemas?.get(refType)?.visit(openAPI, visFun, visitor)
        return
    }

    when (this) {
        is ComposedSchema -> {
            oneOf?.forEach { s -> s.visit(openAPI, visFun, visitor) }
            allOf?.forEach { s -> s.visit(openAPI, visFun, visitor) }
            anyOf?.forEach { s -> s.visit(openAPI, visFun, visitor) }
        }
        is ArraySchema -> items?.visit(openAPI, visFun, visitor)
        is MapSchema -> (additionalProperties as? Schema<*>)?.visit(openAPI, visFun, visitor)
    }
    not?.visit(openAPI, visFun, visitor)
    properties?.forEach { (_, prop) -> prop.visit(openAPI, visFun, visitor) }
}

package gmbh.wapp.stripper

import io.swagger.parser.OpenAPIParser
import io.swagger.v3.core.util.Json
import io.swagger.v3.core.util.Yaml
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.parser.core.models.ParseOptions
import org.slf4j.LoggerFactory

class Stripper(
    val options: StripperOptions
) {

    private val logger = LoggerFactory.getLogger(Stripper::class.java)
    private val openAPI: OpenAPI

    init {
        logger.info(options.toString())
        val parseOpts = ParseOptions().apply {
            isResolve = false
            isFlatten = false
            isResolveFully = false
            isFlattenComposedSchemas = false
            isResolveCombinators = false
        }
        openAPI = OpenAPIParser().readLocation(options.input.path, emptyList(), parseOpts).openAPI
    }

    fun run() {
        openAPI.filterTags()
        openAPI.filterSecuritySchemes()
        openAPI.filterResponses()
        openAPI.filterParameters()
        openAPI.filterSchemas()
        write()
    }

    private fun OpenAPI.filterTags() {
        paths ?: return
        val removePathItems = mutableListOf<String>()
        paths.toSortedMap().forEach { (name, item) ->
            with(item) {
                if (get.shouldRemove(PathItem.HttpMethod.GET, name)) get = null
                if (put.shouldRemove(PathItem.HttpMethod.PUT, name)) put = null
                if (head.shouldRemove(PathItem.HttpMethod.HEAD, name)) head = null
                if (post.shouldRemove(PathItem.HttpMethod.POST, name)) post = null
                if (delete.shouldRemove(PathItem.HttpMethod.DELETE, name)) delete = null
                if (patch.shouldRemove(PathItem.HttpMethod.PATCH, name)) patch = null
                if (options.shouldRemove(PathItem.HttpMethod.OPTIONS, name)) options = null
                if (trace.shouldRemove(PathItem.HttpMethod.TRACE, name)) trace = null
            }
            if (item.readOperations().count() == 0) {
                removePathItems += name
            }
        }
        removePathItems.forEach(paths::remove)
        if (paths.isEmpty()) paths = null
    }

    private fun OpenAPI.filterSecuritySchemes() {
        val securitySchemes = components?.securitySchemes ?: return
        val securitySchemeKeys = mutableListOf<String>()
        securitySchemes.forEach { (name, _) ->
            val isUsed = paths?.any { (_, pathItem) ->
                pathItem.readOperations().any { operation ->
                    operation.security.orEmpty().any { requirement ->
                        requirement.containsKey(name)
                    }
                }
            } == true
            if (!isUsed) securitySchemeKeys += name
        }
        securitySchemeKeys.sorted().forEach { key ->
            logger.info("remove securityScheme $key")
            securitySchemes.remove(key)
        }
        if (components.securitySchemes.isEmpty()) components.securitySchemes = null
    }

    private fun OpenAPI.filterResponses() {
        val responses = components?.responses ?: return
        val responseKeys = responses.keys.toMutableList()
        visitAllResponses { code, _ ->
            responseKeys -= code
        }
        responseKeys.sorted().forEach { key ->
            logger.info("remove response $key")
            components.responses.remove(key)
        }
        if (components.responses.isEmpty()) components.responses = null
    }

    private fun OpenAPI.filterParameters() {
        val parameters = components?.parameters ?: return
        val parameterKeys = parameters.keys.toMutableList()
        visitAllParameters { param ->
            val name = param.`$ref`?.getRefName()
            name?.let(parameterKeys::remove)
        }
        parameterKeys.sorted().forEach { key ->
            logger.info("remove parameter $key")
            components.parameters.remove(key)
        }
        if (components.parameters.isEmpty()) components.parameters = null
    }

    private fun OpenAPI.filterSchemas() {
        val schemas = components?.schemas ?: return
        val schemaKeys = schemas.keys.toMutableList()
        visitAllSchemas { schema ->
            val name = schema.`$ref`?.getRefName()
            name?.let(schemaKeys::remove)
        }
        schemaKeys.sorted().forEach { key ->
            logger.info("remove schema $key")
            components.schemas.remove(key)
        }
        if (components.schemas.isEmpty()) components.schemas = null
    }

    private fun Operation?.shouldRemove(method: PathItem.HttpMethod, name: String): Boolean {
        this ?: return false
        val allowedTags = options.tags
        val operationTags = this.tags.orEmpty()
        return allowedTags.intersect(operationTags).isEmpty().also {
            if (it) logger.info("remove operation $method $name (tags: $operationTags)")
        }
    }

    private fun write() {
        options.output.writeText(
            when (options.type) {
                OutputType.JSON -> Json.pretty(openAPI)
                OutputType.YAML -> Yaml.pretty(openAPI)
            }
        )
    }

}
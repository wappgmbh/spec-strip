package gmbh.wapp.stripper

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema

fun OpenAPI.resolveSchema(schema: Schema<*>) =
    schema.getRefTypeName()?.let { components?.schemas?.get(it) } ?: schema

fun Schema<*>.getRefTypeName(): String? = `$ref`?.getRefName()

fun String.getRefName(): String = substringAfterLast('/')
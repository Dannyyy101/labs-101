/**
 * Rewrites the OpenAPI 3.1 spec of springdoc into the 3.0 form the kotlin generator handles:
 * - `type: ["string", "null"]` becomes `type: string, nullable: true`
 * - `type: ["object", "null"]` next to an `allOf`/`$ref` becomes a nullable reference
 * - a `oneOf` of component schemas becomes one merged schema, whose properties are only required
 *   when every variant requires them (the generator otherwise requires all of them)
 */
@Suppress("UNCHECKED_CAST")
class OpenApiNormalizer(private val spec: MutableMap<String, Any?>) {
    private val schemas: MutableMap<String, Any?> =
        ((spec["components"] as? MutableMap<String, Any?>)?.get("schemas") as? MutableMap<String, Any?>) ?: mutableMapOf()

    fun run() {
        spec["openapi"] = "3.0.3"
        normalize(spec)
        // the variants were merged, generating them as subclasses of their discriminator parent doesn't compile
        schemas.values.forEach { flattenDiscriminatorParent(it as MutableMap<String, Any?>) }
        removeUnusedDiscriminatorParents()
    }

    /** a parent nothing points to anymore is generated as an interface that doesn't compile either */
    private fun removeUnusedDiscriminatorParents() {
        val parents = schemas.filterValues { (it as? Map<*, *>)?.containsKey("discriminator") == true }.keys
        parents.forEach { name ->
            val ref = "#/components/schemas/$name"
            if (!references(spec, ref)) schemas.remove(name)
        }
    }

    private fun references(node: Any?, ref: String): Boolean = when (node) {
        is Map<*, *> -> node["\$ref"] == ref || node.values.any { references(it, ref) }
        is List<*> -> node.any { references(it, ref) }
        else -> false
    }

    private fun normalize(node: Any?) {
        when (node) {
            is MutableList<*> -> node.forEach { normalize(it) }
            is MutableMap<*, *> -> {
                val map = node as MutableMap<String, Any?>
                map.values.toList().forEach { normalize(it) }
                normalizeType(map)
                mergeOneOf(map)
            }
        }
    }

    private fun normalizeType(map: MutableMap<String, Any?>) {
        val types = map["type"] as? List<*> ?: return
        val nonNull = types.filter { it != "null" }
        if (nonNull.size != types.size) map["nullable"] = true
        if (nonNull.size != 1) {
            map.remove("type")
            return
        }
        // a nullable reference is written as type object with an allOf of the referenced schema
        if (nonNull.first() == "object" && (map.containsKey("allOf") || map.containsKey("\$ref"))) {
            map.remove("type")
            return
        }
        map["type"] = nonNull.first()
    }

    private fun mergeOneOf(map: MutableMap<String, Any?>) {
        val variants = map["oneOf"] as? List<*> ?: return
        val names = variants.map { ((it as? Map<*, *>)?.get("\$ref") as? String)?.substringAfterLast('/') }
        if (names.any { it == null }) return

        val merged = names.filterNotNull().sorted().joinToString("Or")
        if (!schemas.containsKey(merged)) {
            schemas[merged] = mergedSchema(names.filterNotNull())
        }
        map.remove("oneOf")
        map["\$ref"] = "#/components/schemas/$merged"
    }

    private fun mergedSchema(names: List<String>): MutableMap<String, Any?> {
        val properties = linkedMapOf<String, Any?>()
        var required: Set<String>? = null
        names.forEach { name ->
            val (variantProperties, variantRequired) = flatten(schemas[name] as Map<String, Any?>)
            variantProperties.forEach { (key, value) -> properties.putIfAbsent(key, value) }
            required = required?.intersect(variantRequired) ?: variantRequired
        }
        return mutableMapOf("type" to "object", "properties" to properties, "required" to (required ?: emptySet()).sorted())
    }

    /** Properties and required properties of a schema including its allOf parts. */
    private fun flatten(schema: Map<String, Any?>): Pair<Map<String, Any?>, Set<String>> {
        val properties = linkedMapOf<String, Any?>()
        val required = mutableSetOf<String>()
        (schema["allOf"] as? List<*>)?.forEach { part ->
            val partMap = part as Map<String, Any?>
            val resolved = (partMap["\$ref"] as? String)?.let { schemas[it.substringAfterLast('/')] as Map<String, Any?> } ?: partMap
            val (p, r) = flatten(resolved)
            properties.putAll(p)
            required.addAll(r)
        }
        (schema["properties"] as? Map<String, Any?>)?.let { properties.putAll(it) }
        (schema["required"] as? List<*>)?.forEach { required.add(it as String) }
        return properties to required
    }

    private fun flattenDiscriminatorParent(schema: MutableMap<String, Any?>) {
        val parts = schema["allOf"] as? List<*> ?: return
        val hasDiscriminatorParent = parts.any { part ->
            val ref = (part as Map<*, *>)["\$ref"] as? String ?: return@any false
            (schemas[ref.substringAfterLast('/')] as? Map<*, *>)?.containsKey("discriminator") == true
        }
        if (!hasDiscriminatorParent) return
        val (properties, required) = flatten(schema)
        schema.remove("allOf")
        schema["type"] = "object"
        schema["properties"] = properties
        schema["required"] = required.sorted()
    }
}

// lib/api/schema.d.ts from the OpenAPI spec of the backend (api/openapi.json), `npm run generate:api`
import fs from "node:fs"
import openapiTS, { astToString, NULL, oapiRef, tsUnion } from "openapi-typescript"

const spec = new URL("../../api/openapi.json", import.meta.url)
const output = new URL("../lib/api/schema.d.ts", import.meta.url)

const ast = await openapiTS(spec, {
    // a nullable reference is written as allOf (the Swift generator can't read oneOf with null),
    // openapi-typescript would make it `(T | null) & T`
    transform(schema) {
        const [only, ...more] = schema.allOf ?? []
        if (!only?.$ref || more.length || !schema.type?.includes?.("null")) return undefined
        return tsUnion([oapiRef(only.$ref), NULL])
    },
})
fs.writeFileSync(output, astToString(ast))

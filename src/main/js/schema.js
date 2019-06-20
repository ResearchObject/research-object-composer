const jsonpointer = require('jsonpointer');
const cache = {};
const pendingLoads = {};

// Deep-load the schema, and any referenced schemas, from the given URL.
function loadSchema(path) {
    if (pendingLoads[path]) {
        console.log("Load pending:", path);
        return pendingLoads[path];
    }
    console.log("Loading:", path);
    pendingLoads[path] = fetch(path).then(function(response) {
        return response.json();
    }).then(function(json) {
        return discoverRefs(path, json, json).then(function () {
            cache[path] = json;
            return json;
        });
    });

    return pendingLoads[path];
}


// Iterate over every property on the given object, and load any schemas referenced by $refs.
function discoverRefs(rootPath, root, obj) {
    const promises = [];
    for (const property in obj) {
        if (property === "$ref") {
            const parts = obj[property].split("#");
            const url = parts[0];
            if (url !== "") {
                promises.push(loadSchema(url));
            }
        }
        if (typeof obj[property] === 'object') {
            promises.push(discoverRefs(rootPath, root, obj[property]));
        }
    }

    return Promise.all(promises);
}

function fetchRef(schemaUrl, pointer) {
    return jsonpointer.get(cache[schemaUrl], pointer);
}

// Fully resolve a schema by inlining any $refs.
function resolveSchema(context, obj) {
    if (typeof obj !== 'object')
        return obj;

    if (obj["$ref"]) {
        const parts = obj["$ref"].split("#");
        const pointer = parts[1];
        const innerContext = parts[0] === "" ? context : parts[0];
        return resolveSchema(innerContext, fetchRef(innerContext, pointer, obj["$ref"]));
    }

    const newObj = {};
    for (const property in obj) {
        if (typeof obj[property] === 'object' && obj[property] !== null) {
            if (Array.isArray(obj[property])) {
                newObj[property] = obj[property].map((v) => resolveSchema(context, v));
            } else {
                newObj[property] = resolveSchema(context, obj[property]);
            }
        } else {
            newObj[property] = obj[property];
        }
    }

    return newObj;
}

// Create uiSchema
function buildUiSchema(obj) {
    const uiSchema = obj['$ui'] || {};

    if (obj.type === 'object' && obj.properties) {
        for (const property in obj.properties) {
            const propUiSchema = buildUiSchema(obj.properties[property]);
            if (Object.keys(propUiSchema).length) {
                uiSchema[property] = propUiSchema;
            }
        }
    } else if (obj.type === 'array' && obj.items) {
        const itemSchema = buildUiSchema(obj.items);
        if (Object.keys(itemSchema).length) {
            uiSchema.items = itemSchema;
        }
    }

    return uiSchema;
}

module.exports = {
    loadSchema: loadSchema,
    resolveSchema: resolveSchema,
    buildUiSchema: buildUiSchema
};
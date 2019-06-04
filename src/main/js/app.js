require('babel-polyfill');
const React = require('react');
const ReactDOM = require('react-dom');
import Form from "react-jsonschema-form";
const jsonpointer = require('jsonpointer');
const client = require('./client');

class App extends React.Component {
    constructor(props) {
        super(props);
        this.state = {profiles: []};
    }

    componentDidMount() {
        client({method: 'GET', path: '/profiles'}).done(response => {
            this.setState({profiles: response.entity._embedded.researchObjectProfileList});
        });
    }

    render() {
        return (
            <ProfileList profiles={this.state.profiles}/>
        )
    }
}

class ProfileList extends React.Component{
    render() {
        const profiles = this.props.profiles.map(profile =>
            <Profile key={profile._links.self.href} profile={profile}/>
        );
        return (
            <table>
                <tbody>
                <tr>
                    <th>Name</th>
                    <th>Schema</th>
                </tr>
                {profiles}
                </tbody>
            </table>
        )
    }
}

class Profile extends React.Component{
    render() {
        return (
            <tr>
                <td>{this.props.profile.name}</td>
                <td><a href={this.props.profile._links.schema.href} target="_blank">{this.props.profile._links.schema.href}</a></td>
            </tr>
        )
    }
}

const cache = {};
const pendingLoads = {};
const schemaCache = {};

window.cache = cache;
window.schemaCache = schemaCache;

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

// "Flatten" a schema by inlining any $refs.
function replaceRefs(context, obj) {
    if (obj["$ref"]) {
        const parts = obj["$ref"].split("#");
        const pointer = parts[1];
        const innerContext = parts[0] === "" ? context : parts[0];
        return replaceRefs(innerContext, fetchRef(innerContext, pointer, obj["$ref"]));
    }

    const newObj = {};
    for (const property in obj) {
        if (typeof obj[property] === 'object' && obj[property] !== null) {
            if (Array.isArray(obj[property])) {
                newObj[property] = obj[property].map((v) => replaceRefs(context, v));
            } else {
                newObj[property] = replaceRefs(context, obj[property]);
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

const defaultUiSchema = {
    _metadata: {
        'ui:title': 'Metadata',
        classNames: 'metadata',
        creators: {
            items : {
                name: {
                    'ui:placeholder': 'Name',
                    classNames: 'creator-name'
                },
                orcid: {
                    'ui:placeholder': 'ORCiD'
                },
                affiliation: {
                    'ui:placeholder': 'Affiliation'
                }
            }
        }
    },
    workflow_params: { 'ui:widget': 'textarea', 'ui:options': { rows: 5 } }
};

window.res = {};

loadSchema("/schemas/draft_task.schema.json").then(function (schema) {
    const resolved = replaceRefs("/schemas/draft_task.schema.json", schema);
    const uiSchema = Object.assign(defaultUiSchema, buildUiSchema(resolved));
    window.resolvedSchema = resolved;
    window.uiSchema = uiSchema;

    const log = (type) => console.log.bind(console, type);

    ReactDOM.render(
        <Form schema={resolved}
              uiSchema={uiSchema}
              onChange={log("changed")}
              onSubmit={log("submitted")}
              onError={log("errors")} />
        , document.getElementById("form")
    );

    ReactDOM.render(
        <App />,
        document.getElementById('react')
    );
});

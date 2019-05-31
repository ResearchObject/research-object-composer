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
            console.log(response.entity);
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
                <td><a href="{this.props.profile._links.schema.href}">{this.props.profile._links.schema.href}</a></td>
            </tr>
        )
    }
}

const cache = {};
const pendingLoads = {};
window.cache = cache;

function loadSchema(path) {
    if (pendingLoads[path]) {
        console.log("Load pending:", path);
        return pendingLoads[path];
    }
    pendingLoads[path] = fetch(path).then(function(response) {
        console.log("Loading", path);
        return response.json();
    }).then(function(json) {
        console.log("Discovering $refs!");
        return discoverRefs(path, json, json).then(function () {
            cache[path] = json;
            return json;
        });
    });

    return pendingLoads[path];
}

const schemaCache = {};
window.schemaCache = schemaCache;

function discoverRefs(rootPath, root, obj) {
    console.log("Deferencing", obj);
    const promises = [];
    for (const property in obj) {
        console.log(property);
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
    console.log(schemaUrl + '#' + pointer);
    console.log(jsonpointer.get(cache[schemaUrl], pointer));
    return jsonpointer.get(cache[schemaUrl], pointer);
}

function replaceRefs(context, obj) {
    console.log(context);
    console.log(obj);
    if (obj["$ref"]) {
        const parts = obj["$ref"].split("#");
        const pointer = parts[1];
        console.log("attempting deref");
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

loadSchema("/schemas/draft_task.schema.json").then(function (resolved) {
    const replaced = replaceRefs("/schemas/draft_task.schema.json", resolved);
    console.log(replaced);

    const log = (type) => console.log.bind(console, type);

    ReactDOM.render(
        <Form schema={replaced}
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

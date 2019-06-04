require('babel-polyfill');
const React = require('react');
const ReactDOM = require('react-dom');
import Form from "react-jsonschema-form";
const client = require('./client');
const schemaUtil = require('./schema');
const follow = require('./follow');

class App extends React.Component {
    constructor(props) {
        super(props);
        this.state = { profiles: [], researchObjects: []};
    }

    componentDidMount() {
        this.loadData();
    }

    loadData() {
        // TODO: Refactor API so "/profiles" and "/research_objects" are discoverable from root
        follow(client, '/profiles', [])
            .done(profileCollection => {
                this.setState({ profiles: profileCollection.entity._embedded.researchObjectProfileList });
            });
        follow(client, '/research_objects', [])
            .done(researchObjectCollection => {
                this.setState({ researchObjects: researchObjectCollection.entity._embedded.researchObjectSummaryList });
            });
    }

    render() {
        return (
            <div>
              <ProfileList profiles={this.state.profiles}/>
              <ResearchObjectList researchObjects={this.state.researchObjects}/>
            </div>
        )
    }
}

class ProfileList extends React.Component{
    render() {
        const profiles = this.props.profiles.map(profile =>
            <Profile key={profile._links.self.href} profile={profile}/>
        );
        return (
            <div className="profile-list-wrapper">
                <h2>Available Profiles</h2>
                <div className="profile-list">
                    {profiles}
                </div>
            </div>
        )
    }
}

class Profile extends React.Component{
    constructor(props) {
        super(props);
        this.state = { form: null };
        this.loadForm = this.loadForm.bind(this);
    }

    render() {
        return (
            <div className="profile">
                <h3 onClick={this.loadForm}>{this.props.profile.name}</h3>
                <a href={this.props.profile._links.schema.href} target="_blank">{this.props.profile._links.schema.href}</a>
                {this.state.form}
            </div>
        )
    }

    loadForm() {
        const schemaHref = this.props.profile._links.schema.href;
        schemaUtil.loadSchema(schemaHref).then((schema) => {
            const resolved = schemaUtil.resolveSchema(schemaHref, schema);
            const uiSchema = Object.assign(defaultUiSchema, schemaUtil.buildUiSchema(resolved));

            this.setState({
                form: <ResearchObjectForm schema={resolved} uiSchema={uiSchema} onSubmit={this.onSubmit}/>
            });
        });
    }

    onSubmit(x) {
        console.log(x);
    }
}

class ResearchObjectList extends React.Component{
    render() {
        const researchObjectSummaries = this.props.researchObjects.map(researchObject =>
            <ResearchObjectSummary key={researchObject._links.self.href} researchObject={researchObject}/>
        );
        return (
            <div className="research-object-list-wrapper">
                <h2>Research Objects</h2>
                <table className="table research-object-list">
                    <thead>
                    <tr>
                        <th>ID</th>
                        <th>Link</th>
                    </tr>
                    </thead>
                    <tbody>
                    {researchObjectSummaries}
                    </tbody>
                </table>
            </div>
        )
    }
}

class ResearchObjectSummary extends React.Component{
    render() {
        return (
            <tr>
                <td>{this.props.researchObject.id}</td>
                <td><a href={this.props.researchObject._links.self.href} target="_blank">{this.props.researchObject._links.self.href}</a></td>
            </tr>
        )
    }
}

class ResearchObjectForm extends React.Component{
    render() {
        return (
            <Form schema={this.props.schema}
                  uiSchema={this.props.uiSchema}
                  // onChange={log("changed")}
                  onSubmit={this.props.onSubmit}
                  // onError={log("errors")}
            />
        )
    }
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

ReactDOM.render(
    <App />,
    document.getElementById('react')
);

const React = require('react');
const ReactDOM = require('react-dom');
import Form from "react-jsonschema-form";
const client = require('./client');
const schemaUtil = require('./schema');
const follow = require('./follow');

class App extends React.Component {
    constructor(props) {
        super(props);
        this.state = { profiles: [], researchObjects: [], modal: null };
        this.loadCreateForm = this.loadCreateForm.bind(this);
        this.loadEditForm = this.loadEditForm.bind(this);
        this.cancelModal = this.cancelModal.bind(this);
        this.createResearchObject = this.createResearchObject.bind(this);
        this.readResearchObject = this.readResearchObject.bind(this);
        this.updateResearchObject = this.updateResearchObject.bind(this);
        this.deleteResearchObject = this.deleteResearchObject.bind(this);
    }

    componentDidMount() {
        this.loadProfiles();
        this.loadROs();
    }

    loadProfiles() {
        // TODO: Refactor API so "/profiles" and "/research_objects" are discoverable from root
        return follow(client, '/profiles', [])
            .done(profileCollection => {
                this.setState({ profiles: profileCollection.entity._embedded.researchObjectProfileList });
            });
    }

    loadROs() {
        return follow(client, '/research_objects', [])
            .done(researchObjectCollection => {
                this.setState({ researchObjects: researchObjectCollection.entity._embedded.researchObjectSummaryList });
            });
    }

    loadCreateForm(profile) {
        const schemaHref = profile._links.schema.href;

        schemaUtil.loadSchema(schemaHref).then((schema) => {
            const resolved = schemaUtil.resolveSchema(schemaHref, schema);
            const uiSchema = Object.assign(defaultUiSchema, schemaUtil.buildUiSchema(resolved));
            const submit = (x) => {
                this.createResearchObject(profile, x.formData);
            };

            this.setState({
                modal: <ResearchObjectForm title={"New " + profile.name}
                                           schema={resolved}
                                           uiSchema={uiSchema}
                                           onCancel={this.cancelModal}
                                           onSubmit={submit}/>
            });
        });
    }

    loadEditForm(researchObject) {
        // Load the Profile
        follow(client, researchObject._links.profile.href, [])
            .then(profile => {
                const schemaHref = profile.entity._links.schema.href;
                // Load the schema to generate the form
                return schemaUtil.loadSchema(schemaHref).then((schema) => {
                    const resolved = schemaUtil.resolveSchema(schemaHref, schema);
                    const uiSchema = Object.assign(defaultUiSchema, schemaUtil.buildUiSchema(resolved));

                    return {uiSchema, resolved};
                }).then(schemas => {
                    // Load the RO's content to populate the form
                    return follow(client, researchObject._links.self.href, []).done(researchObject => {
                        const submit = (x) => {
                            this.updateResearchObject(researchObject.entity, x.formData);
                        };

                        const title = (researchObject.entity.content &&
                            researchObject.entity.content._metadata &&
                            researchObject.entity.content._metadata.title) || ("RO " + researchObject.entity.id);

                        this.setState({
                            modal: <ResearchObjectForm title={"Editing: " + title}
                                                       schema={schemas.resolved}
                                                       uiSchema={schemas.uiSchema}
                                                       formData={researchObject.entity.content}
                                                       onCancel={this.cancelModal}
                                                       onSubmit={submit}/>
                        });
                    });
                });
            });
    }

    cancelModal() {
        this.setState({ modal: null });
    }

    createResearchObject(profile, formData) {
        client({
            method: 'POST',
            path: profile._links.researchObjects.href,
            entity: formData,
            headers: {'Content-Type': 'application/json'}
        }).then(response => {
            this.setState({ modal: null });
            return this.loadROs();
        });
    }

    readResearchObject(researchObject) {
        client({
            method: 'GET',
            path: researchObject._links.self.href,
            headers: {'Content-Type': 'application/json'}
        }).then(response => {
            this.setState({ modal: <ResearchObject researchObject={response.entity} onCancel={this.cancelModal}/> });
        });
    }

    updateResearchObject(researchObject, formData) {
        client({
            method: 'PUT',
            path: researchObject._links.content.href,
            entity: formData,
            // This request returns plain JSON
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }
        }).then(response => {
            this.setState({ modal: null });
            return this.loadROs();
        });
    }

    deleteResearchObject(researchObject) {
        client({
            method: 'DELETE',
            path: researchObject._links.self.href,
            headers: { 'Content-Type': 'application/json' }
        }).then(response => {
            return this.loadROs();
        });
    }

    render() {
        return (
            <div>
                { this.state.modal }
                <ProfileList profiles={this.state.profiles} loadForm={this.loadCreateForm}/>
                <ResearchObjectList researchObjects={this.state.researchObjects}
                                    loadForm={this.loadEditForm}
                                    readResearchObject={this.readResearchObject}
                                    deleteResearchObject={this.deleteResearchObject}
                />
            </div>
        )
    }
}

class ProfileList extends React.Component{
    render() {
        const profiles = this.props.profiles.map(profile =>
            <Profile key={profile._links.self.href} profile={profile} loadForm={this.props.loadForm}/>
        );
        return (
            <div className="profile-list-wrapper">
                <h2>Available Profiles</h2>
                <div className="profile-list row">
                    {profiles}
                </div>
            </div>
        )
    }
}

class Profile extends React.Component{
    constructor(props) {
        super(props);
        this.loadForm = this.loadForm.bind(this);
    }

    render() {
        return (
            <div className="profile col-sm-4">
                <div className="panel panel-default">
                    <div className="panel-body clearfix">
                        <div className="pull-left">{this.props.profile.name}</div>
                        <div className="btn-group pull-right" role="group">
                            <button className="btn btn-xs btn-success" onClick={this.loadForm} target="_blank">
                                <i className="glyphicon glyphicon-plus"></i> New RO
                            </button>
                            <a className="btn btn-xs btn-default" href={this.props.profile._links.schema.href} target="_blank">
                                <i className="glyphicon glyphicon-search"></i> View Schema
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        )
    }

    loadForm() {
        this.props.loadForm(this.props.profile);
    }
}

class ResearchObjectList extends React.Component{
    render() {
        const researchObjectSummaries = this.props.researchObjects.map(researchObject =>
            <ResearchObjectSummary key={researchObject._links.self.href}
                                   researchObject={researchObject}
                                   loadForm={this.props.loadForm}
                                   readResearchObject={this.props.readResearchObject}
                                   deleteResearchObject={this.props.deleteResearchObject}
            />
        );
        return (
            <div className="research-object-list-wrapper">
                <h2>Research Objects</h2>
                <table className="table research-object-list">
                    <thead>
                    <tr>
                        <th>ID</th>
                        <th>Link</th>
                        <th>Type</th>
                        <th>Actions</th>
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
    constructor(props) {
        super(props);
        this.loadForm = this.loadForm.bind(this);
        this.readResearchObject = this.readResearchObject.bind(this);
        this.deleteResearchObject = this.deleteResearchObject.bind(this);
    }

    render() {
        return (
            <tr>
                <td>{this.props.researchObject.id}</td>
                <td>
                    <a href={this.props.researchObject._links.self.href} target="_blank">
                        {this.props.researchObject._links.self.href}
                    </a>
                </td>
                <td>{this.props.researchObject.profileName}</td>
                <td>
                    <div className="btn-group" role="group">
                        <button className="btn btn-xs btn-default" onClick={this.readResearchObject}>
                            <i className="glyphicon glyphicon-search"></i> View
                        </button>

                        <button className="btn btn-xs btn-default" onClick={this.loadForm}>
                            <i className="glyphicon glyphicon-edit"></i> Edit
                        </button>

                        <button className="btn btn-xs btn-danger" onClick={this.deleteResearchObject}>
                            <i className="glyphicon glyphicon-trash"></i> Delete
                        </button>
                    </div>
                </td>
            </tr>
        )
    }

    loadForm() {
        this.props.loadForm(this.props.researchObject);
    }

    readResearchObject() {
        this.props.readResearchObject(this.props.researchObject);
    }

    deleteResearchObject() {
        if (confirm("Are you sure you want to delete this Research Object?")) {
            this.props.deleteResearchObject(this.props.researchObject);
        }
    }
}

class ResearchObjectForm extends React.Component{
    constructor(props) {
        super(props);
        this.cancel = this.cancel.bind(this);
    }

    render() {
        return (
            <div className="form-wrapper">
                <div className="blackout" onClick={this.cancel}></div>
                <div className="panel panel-default form-panel">
                    <div className="panel-heading">
                        { this.props.title }
                        <button type="button" className="close" aria-label="Close" onClick={this.cancel}>
                            <span aria-hidden="true">&times;</span>
                        </button>
                    </div>
                    <div className="panel-body">
                        <Form schema={this.props.schema}
                              uiSchema={this.props.uiSchema}
                              formData={this.props.formData}
                            //  onChange={this.recordChanges}
                            // onError={log("errors")}
                              onSubmit={this.props.onSubmit}>
                            <div>
                                <button className="btn btn-primary" type="submit">Submit</button>
                                &nbsp;
                                <button className="btn btn-default" type="button" onClick={this.cancel}>Cancel</button>
                            </div>
                        </Form>
                    </div>
                </div>
            </div>
        )
    }

    cancel() {
        if (confirm("Are you sure you wish to cancel?")) {
            this.props.onCancel();
        }
    }
}

class ResearchObject extends React.Component{
    render() {
        return (
            <div className="form-wrapper">
                <div className="blackout" onClick={this.props.onCancel}></div>
                <div className="panel panel-default form-panel">
                    <div className="panel-heading">
                        { this.props.researchObject.id }
                        <button type="button" className="close" aria-label="Close" onClick={this.props.onCancel}>
                            <span aria-hidden="true">&times;</span>
                        </button>
                    </div>
                    <div className="panel-body">
                        <div className="research-object">
                            <Metadata metadata={this.props.researchObject.content._metadata}/>
                        </div>
                    </div>
                </div>
            </div>
        )
    }
}

class Metadata extends React.Component{
    render() {
        const fields = ['title', 'description', 'license'].map((f) => {
            if (this.props.metadata[f]) {
                return (
                    <p key={f}>
                        <strong>{ (f.charAt(0).toUpperCase() + f.slice(1)) }</strong>: { this.props.metadata[f] }
                    </p>
                );
            }
        });

        const creators = this.props.metadata.creators.map((c, i) => {
            return <li key={i}><Creator creator={c}/></li>;
        });

        return (
            <div className="research-object-metadata">
                {fields}
                <div className="research-object-creators">
                    <strong>Creators</strong>
                    <ul>
                        {creators}
                    </ul>
                </div>
            </div>
        )
    }
}

class Creator extends React.Component {
    render() {
        const affiliation = this.props.creator.affiliation ? " (" + this.props.creator.affiliation + ")" : "";
        if (this.props.creator.orcid) {
            return <a href={this.props.creator.orcid} target="_blank">{this.props.creator.name + affiliation}</a>;
        } else {
            return <span>{ this.props.creator.name + affiliation }</span>;
        }
    }
}

const defaultUiSchema = {
    _metadata: {
        'ui:title': 'Metadata',
        classNames: 'metadata',
        creators: {
            items : {
                name: {
                    'ui:placeholder': 'Name*',
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

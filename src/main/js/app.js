const React = require('react');
const ReactDOM = require('react-dom');
import Form from "react-jsonschema-form";
const client = require('./client');
const schemaUtil = require('./schema');

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
        this.viewSchema = this.viewSchema.bind(this);
        this.links = {};
    }

    componentDidMount() {
        this.loadRoot().then(() => {
            this.loadProfiles();
            this.loadROs();
        });
    }

    loadRoot() {
        return client({ method: 'GET', path: '/' }).then(root => {
            this.links = root.entity._links;
        });
    }

    loadProfiles() {
        return client({ method: 'GET', path: this.links.profiles.href }).then(profileCollection => {
            this.setState({ profiles: profileCollection.entity._embedded.researchObjectProfileList });
        });
    }

    loadROs() {
        return client({ method: 'GET', path: this.links.researchObjects.href }).then(researchObjectCollection => {
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
                modal: <ResearchObjectForm title={'New "' + profile.name + '" RO'}
                                           schema={resolved}
                                           uiSchema={uiSchema}
                                           onCancel={this.cancelModal}
                                           onSubmit={submit}/>
            });
        });
    }

    loadEditForm(researchObject) {
        // Load the Profile
        return client({
            method: 'GET',
            path: researchObject._links.profile.href
        }).then(profile => {
            const schemaHref = profile.entity._links.schema.href;
            // Load the schema to generate the form
            return schemaUtil.loadSchema(schemaHref).then(schema => {
                const resolved = schemaUtil.resolveSchema(schemaHref, schema);
                const uiSchema = Object.assign(defaultUiSchema, schemaUtil.buildUiSchema(resolved));

                return {uiSchema, resolved};
            }).then(schemas => {
                // Load the RO's content to populate the form
                return client({ method: 'GET', path: researchObject._links.self.href }).then(researchObject => {
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

    viewSchema(profile, resolved) {
        const schemaHref = profile._links.schema.href;
        return schemaUtil.loadSchema(schemaHref).then(schema => {
            let schemaObject = schema;
            let title = 'Schema for: ' + profile.name;

            if (resolved) {
                schemaObject = schemaUtil.resolveSchema(schemaHref, schema);
                title += ' (resolved)';
            }

            this.setState({
                modal: <SchemaJson title={title} schema={schemaObject} onCancel={this.cancelModal}/>
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
        }).then(() => {
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
            headers: { // This request returns plain JSON
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }
        }).then(() => {
            this.setState({ modal: null });
            return this.loadROs();
        });
    }

    deleteResearchObject(researchObject) {
        client({
            method: 'DELETE',
            path: researchObject._links.self.href,
            headers: { 'Content-Type': 'application/json' }
        }).then(() => {
            return this.loadROs();
        });
    }

    render() {
        return (
            <div>
                { this.state.modal }
                <ProfileList profiles={this.state.profiles} loadForm={this.loadCreateForm} viewSchema={this.viewSchema}/>
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
            <Profile key={profile._links.self.href}
                     profile={profile}
                     loadForm={this.props.loadForm}
                     viewSchema={this.props.viewSchema}/>
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
        this.viewSchema = this.viewSchema.bind(this);
        this.viewResolvedSchema = this.viewResolvedSchema.bind(this);
    }

    loadForm() {
        this.props.loadForm(this.props.profile);
    }

    viewSchema() {
        this.props.viewSchema(this.props.profile);
    }

    viewResolvedSchema() {
        this.props.viewSchema(this.props.profile, true);
    }

    render() {
        return (
            <div className="profile col-sm-4">
                <div className="panel panel-default">
                    <div className="panel-body">
                        <div className="clearfix">
                            <div className="pull-left">{this.props.profile.name}</div>
                            <button className="btn btn-xs btn-success pull-right" onClick={this.loadForm}>
                                <i className="glyphicon glyphicon-plus"></i> New RO
                            </button>
                        </div>
                        <div>
                            <button className="btn btn-xs btn-default" onClick={this.viewSchema}>
                                <i className="glyphicon glyphicon-unchecked"></i> Schema
                            </button>&nbsp;
                            <button className="btn btn-xs btn-default" onClick={this.viewResolvedSchema}>
                                <i className="glyphicon glyphicon-check"></i> Schema (resolved)
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        )
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
                        <th>Type</th>
                        <th>URL</th>
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

    isMutable() {
        return !this.props.researchObject._links.deposition;
    }

    render() {
        let depositionUrl;
        const buttons = [
            <button key="view" className="btn btn-xs btn-default" onClick={this.readResearchObject}>
                <i className="glyphicon glyphicon-search"></i> View
            </button>
        ];

        if (this.isMutable()) {
            depositionUrl = <span className="muted">n/a</span>;

            buttons.push(
                <button key="edit" className="btn btn-xs btn-default" onClick={this.loadForm}>
                    <i className="glyphicon glyphicon-edit"></i> Edit
                </button>
            );
            buttons.push(
                <button key="destroy" className="btn btn-xs btn-danger" onClick={this.deleteResearchObject}>
                    <i className="glyphicon glyphicon-trash"></i> Delete
                </button>
            );
        } else {
            depositionUrl = (
                <a href={this.props.researchObject._links.deposition.href} target="_blank">
                    {this.props.researchObject._links.deposition.href}
                </a>
            );
        }

        return (
            <tr>
                <td>{this.props.researchObject.id}</td>
                <td>{this.props.researchObject.profileName}</td>
                <td>{depositionUrl}</td>
                <td>
                    <div className="btn-group" role="group">
                        {buttons}
                    </div>
                </td>
            </tr>
        );
    }
}

function Modal(props) {
    return (
        <div className="form-wrapper">
            <div className="blackout" onClick={props.onCancel}></div>
            <div className="panel panel-default form-panel">
                <div className="panel-heading">
                    {props.title}
                    <button type="button" className="close" aria-label="Close" onClick={props.onCancel}>
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div className="panel-body">
                    <div className="research-object">
                        {props.children}
                    </div>
                </div>
            </div>
        </div>
    );
}

class ResearchObjectForm extends React.Component{
    constructor(props) {
        super(props);
        this.cancel = this.cancel.bind(this);
    }

    cancel() {
        if (confirm("Are you sure you wish to cancel?")) {
            this.props.onCancel();
        }
    }

    render() {
        return (
            <Modal title={this.props.title} onCancel={this.cancel}>
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
            </Modal>
        );
    }
}

class ResearchObject extends React.Component{
    render() {
        const content = [];
        for (const property in this.props.researchObject.content) {
            if (property !== '_metadata') {
                content.push(<Property key={property} property={property} value={this.props.researchObject.content[property]}/>);
            }
        }

        return (
            <Modal title={this.props.researchObject.id} onCancel={this.props.onCancel}>
                <div className="research-object">
                    <Metadata metadata={this.props.researchObject.content._metadata}/>
                    <hr/>
                    <div className="research-object-content">
                        {content}
                    </div>
                </div>
            </Modal>
        );
    }
}

class Metadata extends React.Component{
    render() {
        if (this.props.metadata) {
            const fields = ['title', 'description', 'license'].map((f) => {
                if (this.props.metadata[f]) {
                    return (
                        <p key={f}>
                            <strong className="property-key">{(f.charAt(0).toUpperCase() + f.slice(1))}</strong>: {this.props.metadata[f]}
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
        } else {
            return <span className="muted">No '_metadata' property found!</span>
        }
    }
}

class Creator extends React.Component {
    render() {
        const affiliation = this.props.creator.affiliation ? " (" + this.props.creator.affiliation + ")" : "";
        if (this.props.creator.orcid) {
            return <a href={this.props.creator.orcid} target="_blank">{this.props.creator.name + affiliation}</a>;
        } else {
            return <span>{this.props.creator.name + affiliation}</span>;
        }
    }
}

class Property extends React.Component {
    render() {
        return (
            <div className="property">
                <strong className="property-key">{this.props.property}</strong>: <Value value={this.props.value}></Value>
            </div>
        );
    }
}

class Value extends React.Component {
    render() {
        let value = null;
        if (Array.isArray(this.props.value)) {
            value = (
                <div className="property-array">
                    {this.props.value.map((x, i) => <div className="property-array-item" key={i}><Value value={x}/></div>)}
                </div>
            );
        } else {
            if (!this.props.value) {
                value = <span className="property-value muted">n/a</span>;
            } else {
                if (typeof this.props.value === 'object') {
                    value = [];
                    for (const property in this.props.value) {
                        value.push(<Property key={property} property={property} value={this.props.value[property]}/>);
                    }
                    value = <div className="property-value">{value}</div>;
                } else {
                    value = <span className="property-value">{this.props.value}</span>;
                }
            }

        }

        return value;
    }
}

class SchemaJson extends React.Component{
    render() {
        return (
            <Modal title={this.props.title} onCancel={this.props.onCancel}>
                <textarea className="schema-json"
                          rows="30"
                          readOnly="readonly"
                          value={JSON.stringify(this.props.schema, null, 2)}/>
            </Modal>
        );
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

ReactDOM.render(<App />, document.getElementById('react'));

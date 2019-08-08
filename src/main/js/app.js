const React = require('react');
const ReactDOM = require('react-dom');
import Form from "react-jsonschema-form";
import LoadingBar from 'react-top-loading-bar'
const _client = require('./client');
const schemaUtil = require('./schema');

class App extends React.Component {
    constructor(props) {
        super(props);
        this.state = { profiles: [], researchObjects: [], roLinks: [], roPagination: {}, modal: null };
        this.loadCreateForm = this.loadCreateForm.bind(this);
        this.loadEditForm = this.loadEditForm.bind(this);
        this.cancelModal = this.cancelModal.bind(this);
        this.createResearchObject = this.createResearchObject.bind(this);
        this.readResearchObject = this.readResearchObject.bind(this);
        this.updateResearchObject = this.updateResearchObject.bind(this);
        this.deleteResearchObject = this.deleteResearchObject.bind(this);
        this.viewSchema = this.viewSchema.bind(this);
        this.onNavigate = this.onNavigate.bind(this);
        this.links = {};
    }

    componentDidMount() {
        this.loadRoot().then(() => {
            this.loadProfiles();
            this.loadROs();
        });
    }

    perform(opts) {
        this.LoadingBar.staticStart();
        return _client(opts).finally((args) => {
            this.LoadingBar.complete();
            return args;
        })
    }

    loadSchema(path) {
        this.LoadingBar.staticStart();
        return schemaUtil.loadSchema(path).finally((args) => {
            this.LoadingBar.complete();
            return args;
        })
    }

    loadRoot() {
        return this.perform({ method: 'GET', path: '/' }).then(root => {
            this.links = root.entity._links;
        });
    }

    loadProfiles() {
        return this.perform({ method: 'GET', path: this.links.profiles.href }).then(profileCollection => {
            this.setState({ profiles: profileCollection.entity._embedded.researchObjectProfileList });
        });
    }

    loadROs() {
        return this.onNavigate(this.links.researchObjects.href);
    }

    onNavigate(location) {
        return this.perform({ method: 'GET', path: location }).then(researchObjectCollection => {
            this.setState({
                researchObjects: researchObjectCollection.entity._embedded.researchObjectSummaryList,
                roLinks: researchObjectCollection.entity._links,
                roPagination: researchObjectCollection.entity.page
            });
        })
    }

    loadCreateForm(profile) {
        const schemaHref = profile._links.schema.href;

        this.loadSchema(schemaHref).then((schema) => {
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
        return this.perform({
            method: 'GET',
            path: researchObject._links.profile.href
        }).then(profile => {
            const schemaHref = profile.entity._links.schema.href;
            // Load the schema to generate the form
            return this.loadSchema(schemaHref).then(schema => {
                const resolved = schemaUtil.resolveSchema(schemaHref, schema);
                const uiSchema = Object.assign(defaultUiSchema, schemaUtil.buildUiSchema(resolved));

                return {uiSchema, resolved};
            }).then(schemas => {
                // Load the RO's content to populate the form
                return this.perform({ method: 'GET', path: researchObject._links.self.href }).then(researchObject => {
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
        return this.loadSchema(schemaHref).then(schema => {
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
        this.perform({
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
        this.perform({
            method: 'GET',
            path: researchObject._links.self.href,
            headers: {'Content-Type': 'application/json'}
        }).then(response => {
            this.setState({ modal: <ResearchObject researchObject={response.entity} onCancel={this.cancelModal}/> });
        });
    }

    updateResearchObject(researchObject, formData) {
        this.perform({
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
        this.perform({
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
                <LoadingBar
                    color='#4583f1'
                    height={3}
                    onRef={ref => (this.LoadingBar = ref)}
                />
                { this.state.modal }
                <ProfileList profiles={this.state.profiles}
                             loadCreateForm={this.loadCreateForm}
                             viewSchema={this.viewSchema}/>
                <ResearchObjectList researchObjects={this.state.researchObjects}
                                    links={this.state.roLinks}
                                    pagination={this.state.roPagination}
                                    onNavigate={this.onNavigate}
                                    loadEditForm={this.loadEditForm}
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
                     loadCreateForm={this.props.loadCreateForm}
                     viewSchema={this.props.viewSchema}/>
        );
        return (
            <div className="profile-list-wrapper">
                <h2>Profiles</h2>
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
        this.loadCreateForm = this.loadCreateForm.bind(this);
        this.viewSchema = this.viewSchema.bind(this);
        this.viewResolvedSchema = this.viewResolvedSchema.bind(this);
    }

    loadCreateForm() {
        this.props.loadCreateForm(this.props.profile);
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
                            <button className="btn btn-xs btn-success pull-right" onClick={this.loadCreateForm}>
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
    constructor(props) {
        super(props);
        this.handleNavFirst = this.handleNavFirst.bind(this);
        this.handleNavPrev = this.handleNavPrev.bind(this);
        this.handleNavNext = this.handleNavNext.bind(this);
        this.handleNavLast = this.handleNavLast.bind(this);
    }

    handleNavFirst(e){
        e.preventDefault();
        this.props.onNavigate(this.props.links.first.href);
    }

    handleNavPrev(e) {
        e.preventDefault();
        this.props.onNavigate(this.props.links.prev.href);
    }

    handleNavNext(e) {
        e.preventDefault();
        this.props.onNavigate(this.props.links.next.href);
    }

    handleNavLast(e) {
        e.preventDefault();
        this.props.onNavigate(this.props.links.last.href);
    }

    render() {
        const researchObjectSummaries = this.props.researchObjects.map(researchObject =>
            <ResearchObjectSummary key={researchObject._links.self.href}
                                   researchObject={researchObject}
                                   loadEditForm={this.props.loadEditForm}
                                   readResearchObject={this.props.readResearchObject}
                                   deleteResearchObject={this.props.deleteResearchObject}
            />
        );

        const navLinks = [];
        // if ("first" in this.props.links) {
        //     navLinks.push(<li className="previous"><a href="#" key="first" onClick={this.handleNavFirst}><span aria-hidden="true">&larr;</span> First</a></li>);
        // }
        if ("prev" in this.props.links) {
            navLinks.push(<li className="previous"><a href="#" key="prev" onClick={this.handleNavPrev}><span aria-hidden="true">&larr;</span> Prev</a></li>);
        }
        // if ("last" in this.props.links) {
        //     navLinks.push(<li className="next"><a href="#" key="last" onClick={this.handleNavLast}>Last <span aria-hidden="true">&rarr;</span></a></li>);
        // }
        if ("next" in this.props.links) { // Yes, the order of this is correct.
            navLinks.push(<li className="next"><a href="#" key="next" onClick={this.handleNavNext}>Next <span aria-hidden="true">&rarr;</span></a></li>);
        }

        let pagination = null;
        if (this.props.pagination && this.props.pagination.number) {
            pagination =
                <div>
                    <div className="page-info clearfix row">
                        <div className="col-xs-6">Page {this.props.pagination.number} of {this.props.pagination.totalPages}</div>
                        <div className="col-xs-6 text-right">{this.props.pagination.totalElements} total Research Objects</div>
                    </div>
                    <nav>
                        <ul className="pager">
                            {navLinks}
                        </ul>
                    </nav>
                </div>;
        }

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

                {pagination}
            </div>
        )
    }
}

class ResearchObjectSummary extends React.Component{
    constructor(props) {
        super(props);
        this.loadEditForm = this.loadEditForm.bind(this);
        this.readResearchObject = this.readResearchObject.bind(this);
        this.deleteResearchObject = this.deleteResearchObject.bind(this);
    }

    loadEditForm() {
        this.props.loadEditForm(this.props.researchObject);
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
                <button key="edit" className="btn btn-xs btn-default" onClick={this.loadEditForm}>
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
        <div className="modal-wrapper">
            <div className="blackout" onClick={props.onCancel}></div>
            <div className="panel panel-default modal-panel">
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

const CHECKSUM_TYPES = [
    { type: "md5", title: "MD5", regex: /^[0-9a-f]{32}$/ },
    { type: "sha256", title: "SHA-256", regex: /^[0-9a-f]{64}$/ },
    { type: "sha512", title: "SHA-512", regex: /^[0-9a-f]{128}$/ }
];

// Define a custom component for handling the array of checksums
class ChecksumsComponent extends React.Component {
    constructor(props) {
        super(props);
        this.maxId = 0;
        this.state = { checksums: (props.formData || []).filter(Boolean).map(c => Object.assign(c, { id: ++this.maxId })) };
        this.onChange = this.onChange.bind(this);
        this.add = this.add.bind(this);
        this.delete = this.delete.bind(this);
    }

    onChange(id, updated) {
        let newState = this.state.checksums.map((c) => (c.id === id) ? Object.assign(c, updated) : c);

        this.setState({ checksums: newState },
            () => this.props.onChange(this.state.checksums)); // This passes up the state back to the Form for validation
    }

    add () {
        const newArray = this.state.checksums.concat({ type: CHECKSUM_TYPES[0].type, checksum: '', id: ++this.maxId });
        this.setState({ checksums: newArray },
            () => this.props.onChange(this.state.checksums)); // This passes up the state back to the Form for validation
    }

    delete (id) {
        this.setState({ checksums: this.state.checksums.filter((c) => c.id !== id) },
            () => this.props.onChange(this.state.checksums)); // This passes up the state back to the Form for validation
    }

    render() {
        const sums = this.state.checksums.map((c) =>
            <ChecksumForm key={c.id} type={c.type} checksum={c.checksum} id={c.id} onChange={this.onChange} onDelete={() => this.delete(c.id)}/>
        );
        return (
            <div>
                <label className="control-label">Checksums<span className="required">*</span></label>
                { sums }
                <div className="clearfix">
                    <button type="button" className="btn btn-info btn-add btn-xs col-xs-2" onClick={this.add}>
                        <i className="glyphicon glyphicon-plus"></i>
                    </button>
                </div>
            </div>
        );
    }
}


// Define a custom component for handling a single checksum
class ChecksumForm extends React.Component {
    constructor(props) {
        super(props);
    }

    onChange(name) {
        return (event) => {
            this.setState({
                [name]: event.target.value
            }, () => this.props.onChange(this.props.id, this.state));
        };
    }

    render() {
        const selectOpts = CHECKSUM_TYPES.map((c) => <option key={c.type} value={c.type}>{c.title}</option>);
        return (
            <div className="form-inline checksum-row">
                <select className="form-control" value={this.props.type} onChange={this.onChange("type")}>{ selectOpts }</select>
                <input className="form-control" size="70" type="text" value={this.props.checksum} onChange={this.onChange("checksum")}/>
                <button type="button" className="btn btn-danger array-item-remove pull-right" onClick={this.props.onDelete}>
                    <i className="glyphicon glyphicon-remove"></i>
                </button>
            </div>
        );
    }
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
                      fields={{ checksums: ChecksumsComponent }}
                    //  onChange={this.recordChanges}
                      onError={() => { window.scrollTo({ top: 0, behavior: 'smooth' }); }}
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
            <Modal title={this.props.researchObject._links.self.href} onCancel={this.props.onCancel}>
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

import React from 'react';
import {Card, CardBody, Table} from '../../components/design';

import {getServiceURL} from '../../utilities/rest';
import style from "../../components/base/page/Page.module.scss";


class TaskTreePage extends React.Component {
    state = {
        initalized: false,
        nodes: [],
        translations: []
    };


    fetch = () => {
        this.setState({
            failed: false
        });
        fetch(getServiceURL('task/tree', {
            intial: !this.state.initalized
        }), {
            method: 'GET',
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        })
            .then(response => response.json())
            .then(json => {
                const root = json.root;
                const rootProtectTimesheetsUntil = json.root.protectTimesheetsUntil;
                const rootTitle = json.root.title;
                const translations = json.translations;
                this.setState({
                    nodes: root.childs,
                    protectTimesheetsUntil: rootProtectTimesheetsUntil,
                    rootTitle: rootTitle,
                    translations: translations,
                    initialized: true
                })
            })
            .catch(() => this.setState({initialized: false, failed: true}));
    };

    render() {
        if (!this.state.nodes)
            return <div>Loading...</div>;
        const translations = this.state.translations;
        return <Card>
            <CardBody>
                <Table striped hover responsive>
                    <thead>
                    <tr>
                        <th>{translations['task']}</th>
                        <th>{translations['task.consumption']}</th>
                        <th>[Kost2]</th>
                        <th>[Auftraege]</th>
                        <th>[Kurzbeschreibung]</th>
                        <th>[Schutz bis]</th>
                        <th>[Referenz]</th>
                        <th>[Prioritaet]</th>
                        <th>[Status]</th>
                        <th>[verantwortlich]</th>
                    </tr>
                    </thead>
                    <tbody>
                    {this.state.nodes.map(task => {
                        let indent = [];
                        for (let i = 0; i < task.indent; i++) {
                            indent.push('....');
                        }
                        return (
                            <tr key={`table-body-row-${task.id}`}
                                onClick={this.handleRowClick}
                                className={style.clickable}
                            >
                                <td>{indent}{task.title}</td>
                            </tr>
                        );
                    })}
                    </tbody>
                </Table>
            </CardBody>
        </Card>
    };

    componentDidMount() {
        this.fetch()
    };


    constructor(props) {
        super(props);
        this.fetch = this.fetch.bind(this);

    }
}

export default (TaskTreePage);

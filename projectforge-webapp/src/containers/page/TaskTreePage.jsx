import React from 'react';
import {Card, CardBody, Table} from '../../components/design';
import {faFile, faFolder, faFolderOpen} from '@fortawesome/free-regular-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';

import {getServiceURL} from '../../utilities/rest';
import style from "../../components/base/page/Page.module.scss";
import history from "../../utilities/history";


class TaskTreePage extends React.Component {
    state = {
        initalized: false,
        nodes: [],
        translations: []
    };

    handleRowClick(id) {
        history.push(`/task/edit/${id}`);
    }

    handleEventClick(event, openId, closeId) {
        this.fetch(null, openId, closeId);
        event.stopPropagation();
    }

    fetch = (initial, open, close) => {
        this.setState({
            failed: false
        });
        fetch(getServiceURL('task/tree', {
            initial: initial ? initial : '',
            table: 'true', // Result expected as table not as tree.
            open: open ? open : '',
            close: close ? close : ''
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
                    initialized: true
                })
                if (translations) this.setState({translations: translations}) // Only returned on initial call.
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
                        let indent = <div className={'tree-nav-space'} style={{marginLeft: `${task.indent * 1.5}em`}}>&nbsp;</div>
                        let link = indent;
                        if (task.treeStatus === 'OPENED') {
                            link =
                                <div className={'tree-nav'} onClick={(event) => this.handleEventClick(event, null, task.id)}>
                                    {indent}
                                    <div className={'tree-link-close'}>
                                        <div className={'tree-icon'}><FontAwesomeIcon icon={faFolderOpen}/></div>
                                        {task.title}</div>
                                </div>;
                        } else if (task.treeStatus === 'CLOSED') {
                            link =
                                <div className={'tree-nav'} onClick={(event) => this.handleEventClick(event, task.id, null)}>
                                    {indent}
                                    <div className={'tree-link-close'}>
                                        <div className={'tree-icon'}><FontAwesomeIcon icon={faFolder}/></div>
                                        {task.title}</div>
                                </div>;
                        } else {
                            link = <div className={'tree-nav'}>
                                {indent}
                                <div className={'tree-leaf'}>
                                    <div className={'tree-icon'}><FontAwesomeIcon icon={faFile}/></div>
                                    {task.title}</div>
                            </div>;
                        }
                        let responsibleUser = task.responsibleUser ? task.responsibleUser.fullname : '';
                        return (
                            <tr key={`table-body-row-${task.id}`}
                                onClick={() => this.handleRowClick(task.id)}
                                className={style.clickable}
                            >
                                <td>{link}</td>
                                <td>...</td>
                                <td>...</td>
                                <td>...</td>
                                <td>{task.shortDescription}</td>
                                <td>...</td>
                                <td>{task.reference}</td>
                                <td>{task.priority}</td>
                                <td>{task.status}</td>
                                <td>{responsibleUser}</td>
                            </tr>
                        );
                    })}
                    </tbody>
                </Table>
            </CardBody>
        </Card>
    };

    componentDidMount() {
        this.fetch('true')
    };


    constructor(props) {
        super(props);
        this.fetch = this.fetch.bind(this);
        this.handleRowClick = this.handleRowClick.bind(this);
    }
}

export default (TaskTreePage);

import React from 'react';
import PropTypes from 'prop-types';
import { Alert } from 'reactstrap';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faFile, faFolder, faFolderOpen } from '@fortawesome/free-regular-svg-icons';
import { Card, CardBody, Table } from '../../components/design';

import { getServiceURL } from '../../utilities/rest';
import style from '../../components/base/page/Page.module.scss';

class TaskTreePanel extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            nodes: [],
            translations: [],
        };

        this.fetch = this.fetch.bind(this);
        this.handleRowClick = this.handleRowClick.bind(this);
    }

    componentDidMount() {
        this.fetch('true');
    }

    handleRowClick(id) {
        const { onTaskSelect } = this.props;
        if (onTaskSelect) {
            onTaskSelect(id);
        }
    }

    handleEventClick(event, openId, closeId) {
        this.fetch(null, openId, closeId);
        event.stopPropagation();
    }

    fetch(initial, open, close) {
        fetch(getServiceURL('task/tree', {
            table: 'true', // Result expected as table not as tree.
            open: open || '',
            close: close || '',
        }), {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        })
            .then(response => response.json())
            .then((json) => {
                const { root, translations } = json;
                this.setState({
                    nodes: root.childs,
                });
                if (translations) this.setState({ translations }); // Only returned on initial call.
            })
            .catch(() => this.setState({}));
    }

    render() {
        const { nodes, translations } = this.state;
        if (!nodes) {
            return <div>Loading...</div>;
        }
        return (
            <Card>
                <CardBody>
                    [ToDo: Root task]
                    <Table striped hover responsive>
                        <thead>
                            <tr>
                                <th>{translations.task}</th>
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
                            {nodes.map((task) => {
                                const indentWidth = task.indent > 0 ? task.indent * 1.5 : 0;
                                let link;
                                if (task.treeStatus === 'OPENED') {
                                    link = (
                                        <div
                                            role="presentation"
                                            className="tree-nav"
                                            style={{ marginLeft: `${indentWidth}em` }}
                                            onClick={(event) => {
                                                this.handleEventClick(event, null, task.id);
                                            }}
                                        >
                                            <div className="tree-link-close">
                                                <div className="tree-icon">
                                                    <FontAwesomeIcon icon={faFolderOpen} />
                                                </div>
                                                {task.title}
                                            </div>
                                        </div>
                                    );
                                } else if (task.treeStatus === 'CLOSED') {
                                    link = (
                                        <div
                                            role="presentation"
                                            className="tree-nav"
                                            style={{ marginLeft: `${indentWidth}em` }}
                                            onClick={(event) => {
                                                this.handleEventClick(event, task.id, null);
                                            }}
                                        >
                                            <div className="tree-link-close">
                                                <div className="tree-icon">
                                                    <FontAwesomeIcon icon={faFolder} />
                                                </div>
                                                {task.title}
                                            </div>
                                        </div>
                                    );
                                } else {
                                    link = (
                                        <div className="tree-nav">
                                            <div
                                                className="tree-leaf"
                                                style={{ marginLeft: `${indentWidth}em` }}
                                            >
                                                <div className="tree-icon">
                                                    <FontAwesomeIcon icon={faFile} />
                                                </div>
                                                {task.title}
                                            </div>
                                        </div>
                                    );
                                }
                                const responsibleUser = task.responsibleUser ? task.responsibleUser.fullname : '';
                                return (
                                    <tr
                                        key={`table-body-row-${task.id}`}
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
                    <Alert color="info">
                        {translations['task.tree.info']}
                    </Alert>
                </CardBody>
            </Card>
        );
    }
}

TaskTreePanel.propTypes = {
    onTaskSelect: PropTypes.func,
};

TaskTreePanel.defaultProps = {
    onTaskSelect: undefined,
};

export default (TaskTreePanel);

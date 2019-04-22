import React from 'react';
import PropTypes from 'prop-types';
import { Alert } from 'reactstrap';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faFile, faFolder, faFolderOpen } from '@fortawesome/free-regular-svg-icons';
import classNames from 'classnames';
import { Card, CardBody, Table } from '../../components/design';

import { getServiceURL } from '../../utilities/rest';
import style from '../../components/base/page/Page.module.scss';

class TaskTreePanel extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            nodes: [],
            translations: [],
            scrolled: false, // Scroll only once to highlighted row.
        };
        this.myScrollRef = React.createRef();

        this.fetch = this.fetch.bind(this);
        this.handleRowClick = this.handleRowClick.bind(this);
    }

    componentDidMount() {
        this.fetch('true');
    }

    componentDidUpdate() {
        const { scrolled } = this.state;
        if (this.myScrollRef.current && !scrolled) {
            window.scrollTo(0, this.myScrollRef.current.offsetTop);
            /* eslint-disable-next-line react/no-did-update-set-state */
            this.setState({ scrolled: true });
        }
    }

    handleRowClick(id, task) {
        const { onTaskSelect } = this.props;
        if (onTaskSelect) {
            onTaskSelect(id, task);
        }
    }

    handleEventClick(event, openId, closeId) {
        this.fetch(null, openId, closeId);
        event.stopPropagation();
    }

    fetch(initial, open, close) {
        const { highlightTaskId } = this.props;
        const doOpen = (initial) ? open || highlightTaskId || '' : open || '';
        fetch(getServiceURL('task/tree', {
            table: 'true', // Result expected as table not as tree.
            initial,
            open: doOpen,
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
            return <div>...</div>;
        }
        const { shortForm, highlightTaskId } = this.props;
        return (
            <Card>
                <CardBody>
                    <Table striped hover responsive>
                        <thead>
                            <tr>
                                <th>{translations.task}</th>
                                <th>{translations['task.consumption']}</th>
                                <th>[Kost2]</th>
                                {!shortForm ? <th>[Auftraege]</th> : undefined}
                                <th>[Kurzbeschreibung]</th>
                                {!shortForm ? <th>[Schutz bis]</th> : undefined}
                                {!shortForm ? <th>[Referenz]</th> : undefined}
                                {!shortForm ? <th>[Prioritaet]</th> : undefined}
                                {!shortForm ? <th>[Status]</th> : undefined}
                                {!shortForm ? <th>[verantwortlich]</th> : undefined}
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
                                const highlighted = (highlightTaskId === task.id);
                                return (
                                    <tr
                                        key={`table-body-row-${task.id}`}
                                        onClick={() => this.handleRowClick(task.id, task)}
                                        className={classNames({
                                            [style.clickable]: true,
                                            [style.highlighted]: highlighted,
                                        })}
                                        ref={highlighted ? this.myScrollRef : undefined}
                                    >
                                        <td>{link}</td>
                                        <td>...</td>
                                        <td>...</td>
                                        {!shortForm ? <td>...</td> : undefined}
                                        <td>{task.shortDescription}</td>
                                        {!shortForm ? <td>...</td> : undefined}
                                        {!shortForm ? <td>{task.reference}</td> : undefined}
                                        {!shortForm ? <td>{task.priority}</td> : undefined}
                                        {!shortForm ? <td>{task.status}</td> : undefined}
                                        {!shortForm ? <td>{responsibleUser}</td> : undefined}
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
    highlightTaskId: PropTypes.number,
    shortForm: PropTypes.bool,
};

TaskTreePanel.defaultProps = {
    onTaskSelect: undefined,
    highlightTaskId: undefined,
    shortForm: false,
};

export default (TaskTreePanel);

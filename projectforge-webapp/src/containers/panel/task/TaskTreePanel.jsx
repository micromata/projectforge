import React from 'react';
import PropTypes from 'prop-types';
/* eslint-disable-next-line object-curly-newline */
import { Alert, Button, Col, Row } from 'reactstrap';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faFile, faFolder, faFolderOpen } from '@fortawesome/free-regular-svg-icons';
import classNames from 'classnames';
/* eslint-disable-next-line object-curly-newline */
import { Card, CardBody, Input, Table } from '../../../components/design';

import { getServiceURL } from '../../../utilities/rest';
import style from '../../../components/base/page/Page.module.scss';
import Formatter from '../../../components/base/Formatter';
import LoadingContainer from '../../../components/design/loading-container';
import CheckBox from '../../../components/design/input/CheckBox';
import ConsumptionBar from './ConsumptionBar';

class TaskTreePanel extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            loading: false,
            nodes: [],
            translations: undefined,
            columnsVisibility: [],
            filter: {
                searchString: '',
                opened: true,
                notOpened: true,
                closed: false,
                deleted: false,
            },
        };
        this.myScrollRef = React.createRef();

        this.fetch = this.fetch.bind(this);
        this.handleRowClick = this.handleRowClick.bind(this);
        this.handleInputChange = this.handleInputChange.bind(this);
        this.handleCheckBoxChange = this.handleCheckBoxChange.bind(this);
        this.onSubmit = this.onSubmit.bind(this);
        this.onKeyDown = this.onKeyDown.bind(this);
        this.setFilterValue = this.setFilterValue.bind(this);
    }

    componentDidUpdate(prevProps) {
        const { translations, loading } = this.state;
        const { highlightTaskId, visible } = this.props;
        if (highlightTaskId !== prevProps.highlightTaskId // Highlighted task changed: fetch
            // Now visible and not yet fetched: fetch:
            || (!translations && !loading && visible && !prevProps.visible)) {
            this.fetch(true);
        }
    }

    onSubmit(event) {
        this.fetch();
        event.preventDefault();
    }

    onKeyDown(event) {
        if (event.keyCode === 13) {
            this.fetch();
            event.preventDefault();
            event.stopPropagation();
        }
    }

    setFilterValue(id, value, callback) {
        this.setState(({ filter }) => ({
            filter: {
                ...filter,
                [id]: value,
            },
        }), callback);
    }

    handleInputChange(event) {
        this.setFilterValue(event.target.id, event.target.value);
    }

    handleCheckBoxChange(event) {
        this.setFilterValue(event.target.id, event.target.checked, () => {
            this.fetch();
        });
    }

    handleRowClick(id, task) {
        const { onTaskSelect } = this.props;
        if (onTaskSelect) {
            onTaskSelect(id, task);
        }
    }

    handleEventClick(event, openId, closeId) {
        this.fetch(false, openId, closeId);
        event.stopPropagation();
    }

    fetch(initial, open, close) {
        this.setState({ loading: true }, () => {
            const { filter } = this.state;
            const { highlightTaskId, showRootForAdmins } = this.props;
            fetch(getServiceURL('task/tree', {
                table: true, // Result expected as table not as tree.
                initial: initial || '',
                open: open || '',
                highlightedTaskId: highlightTaskId || '',
                close: close || '',
                searchString: filter.searchString,
                opened: filter.opened,
                notOpened: filter.notOpened,
                closed: filter.closed,
                deleted: filter.deleted,
                showRootForAdmins,
            }), {
                method: 'GET',
                credentials: 'include',
                headers: {
                    Accept: 'application/json',
                },
            })
                .then(response => response.json())
                .then((json) => {
                    const {
                        root,
                        translations,
                        initFilter,
                        columnsVisibility,
                    } = json;
                    this.setState({
                        nodes: root.childs,
                        loading: false,
                        columnsVisibility: columnsVisibility || [],
                    });
                    if (initial && this.myScrollRef.current) {
                        // Scroll only once on initial call to highlighted row:
                        window.scrollTo(0, this.myScrollRef.current.offsetTop);
                    }
                    if (translations) { // Only returned on initial call.
                        this.setState({ translations });
                    }
                    if (initial && initFilter) this.setState({ filter: initFilter });
                })
                .catch(() => this.setState({ loading: false }));
        });
    }

    render() {
        const {
            filter,
            nodes,
            translations,
            loading,
            columnsVisibility,
        } = this.state;
        const { shortForm, highlightTaskId } = this.props;
        /* eslint-disable indent, react/jsx-indent, react/jsx-tag-spacing */
        const content = (translations) ? (
                <React.Fragment>
                    <Card>
                        <CardBody>
                            <Row>
                                <Col sm={5}>
                                    <Input
                                        label={translations.searchFilter}
                                        id="searchString"
                                        value={filter.searchString}
                                        onChange={this.handleInputChange}
                                        onKeyDown={this.onKeyDown}
                                    />
                                </Col>
                                <Col sm={7}>
                                    <Row>
                                        <Button
                                            color="primary"
                                            onClick={this.onSubmit}
                                        >
                                            {translations.search}
                                        </Button>
                                        <CheckBox
                                            label={translations['task.status.opened']}
                                            id="opened"
                                            onChange={this.handleCheckBoxChange}
                                            checked={filter.opened}
                                        />
                                        <CheckBox
                                            label={translations['task.status.notOpened']}
                                            id="notOpened"
                                            onChange={this.handleCheckBoxChange}
                                            checked={filter.notOpened}
                                        />
                                        <CheckBox
                                            label={translations['task.status.closed']}
                                            id="closed"
                                            onChange={this.handleCheckBoxChange}
                                            checked={filter.closed}
                                        />
                                        <CheckBox
                                            label={translations.deleted}
                                            id="deleted"
                                            onChange={this.handleCheckBoxChange}
                                            checked={filter.deleted}
                                            color="danger"
                                        />
                                    </Row>
                                </Col>
                            </Row>
                        </CardBody>
                    </Card>
                    <Card>
                        <CardBody>
                            <Table striped hover responsive>
                                <thead>
                                <tr>
                                    <th>{translations.task}</th>
                                    <th>{translations['task.consumption']}</th>
                                    {columnsVisibility.kost2 ?
                                        <th>{translations['fibu.kost2']}</th> : undefined}
                                    {!shortForm && columnsVisibility.orders
                                        ? <th>{translations['fibu.auftrag.auftraege']}</th> : undefined}
                                    <th>{translations.shortDescription}</th>
                                    {!shortForm && columnsVisibility.protectionUntil
                                        ? (
                                            <th>
                                                {translations['task.protectTimesheetsUntil.short']}
                                            </th>
                                        )
                                        : undefined}
                                    {!shortForm && columnsVisibility.reference
                                        ? <th>{translations['task.reference']}</th> : undefined}
                                    {!shortForm && columnsVisibility.priority
                                        ? <th>{translations.priority}</th> : undefined}
                                    {!shortForm
                                        ? <th>{translations.status}</th> : undefined}
                                    {!shortForm && columnsVisibility.assignedUser
                                        ? <th>{translations['task.assignedUser']}</th> : undefined}
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
                                                        <FontAwesomeIcon
                                                            icon={faFolderOpen}
                                                        />
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
                                                        <FontAwesomeIcon icon={faFolder}/>
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
                                                        <FontAwesomeIcon icon={faFile}/>
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
                                            <td>
                                                <ConsumptionBar progress={task.consumption}/>
                                            </td>
                                            {columnsVisibility.kost2 ? <td>...</td> : undefined}
                                            {!shortForm && columnsVisibility.orders
                                                ? <td>...</td> : undefined}
                                            <td>{task.shortDescription}</td>
                                            {!shortForm && columnsVisibility.protectionUntil
                                                ? (
                                                    <td>
                                                        <Formatter
                                                            formatter="DATE"
                                                            data={task.protectTimesheetsUntil}
                                                            id="date"
                                                        />
                                                    </td>
                                                )
                                                : undefined}
                                            {!shortForm && columnsVisibility.reference
                                                ? <td>{task.reference}</td> : undefined}
                                            {!shortForm && columnsVisibility.priority
                                                ? <td>{task.priority}</td> : undefined}
                                            {!shortForm
                                                ? <td>{task.status}</td> : undefined}
                                            {!shortForm && columnsVisibility.assignedUser
                                                ? <td>{responsibleUser}</td> : undefined}
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
                </React.Fragment>
            )
            : <div>...</div>;
        // Don't know, why IntelliJ's auto format fails...
        return (
            <LoadingContainer loading={loading}>
                {content}
            </LoadingContainer>
        );
    }
}

TaskTreePanel.propTypes = {
    onTaskSelect: PropTypes.func,
    highlightTaskId: PropTypes.number,
    shortForm: PropTypes.bool,
    showRootForAdmins: PropTypes.bool,
    visible: PropTypes.bool.isRequired,
};

TaskTreePanel.defaultProps = {
    onTaskSelect: undefined,
    highlightTaskId: undefined,
    shortForm: false,
    showRootForAdmins: false,
};

export default (TaskTreePanel);

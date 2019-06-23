import { faStream, faTimesCircle } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import React from 'react';
/* eslint-disable-next-line object-curly-newline */
import { Button, Collapse, Modal, ModalBody, ModalHeader } from 'reactstrap';
import PropTypes from 'prop-types';
import style from '../../../design/input/Input.module.scss';
import TaskTreePanel from '../../../../containers/panel/task/TaskTreePanel';
import FavoritesPanel from '../../../../containers/panel/FavoritesPanel';
import { getServiceURL, handleHTTPErrors } from '../../../../utilities/rest';
import taskStyle from './TaskSelect.module.scss';

class TaskSelect extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            taskTreePanelVisible: false,
            taskTreeModalHighlight: undefined,
            task: undefined,
            favorites: undefined,
        };
        this.taskPanelRef = React.createRef();

        this.setTask = this.setTask.bind(this);
        this.toggleTaskTreeModal = this.toggleTaskTreeModal.bind(this);
        this.handleClickOutside = this.handleClickOutside.bind(this);

        this.onFavoriteCreate = this.onFavoriteCreate.bind(this);
        this.onFavoriteDelete = this.onFavoriteDelete.bind(this);
        this.onFavoriteRename = this.onFavoriteRename.bind(this);
        this.onFavoriteSelect = this.onFavoriteSelect.bind(this);
        this.onFavoriteUpdate = this.onFavoriteUpdate.bind(this);
    }

    componentDidMount() {
        document.addEventListener('mousedown', this.handleClickOutside);
        const { variables } = this.props;
        this.setState({ task: variables.task });
        fetch(getServiceURL('task/favorites/list'), {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        })
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then((json) => {
                this.setState({ favorites: json });
            })
            .catch(error => alert(`Internal error: ${error}`));
    }

    componentWillUnmount() {
        document.removeEventListener('mousedown', this.handleClickOutside);
    }

    onFavoriteCreate(newFilterName) {
        fetch(getServiceURL('calendar/createNewFilter',
            { newFilterName }), {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        })
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then(this.saveUpdateResponseInState)
            .catch(error => alert(`Internal error: ${error}`));
    }

    onFavoriteDelete(id) {
        fetch(getServiceURL('calendar/deleteFilter',
            { id }), {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        })
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then(this.saveUpdateResponseInState)
            .catch(error => alert(`Internal error: ${error}`));
    }

    onFavoriteSelect(id) {
        fetch(getServiceURL('calendar/selectFilter',
            { id }), {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        })
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then(this.saveUpdateResponseInState)
            .catch(error => alert(`Internal error: ${error}`));
    }

    onFavoriteRename(id, newName) {
        console.log(id, newName);
    }

    onFavoriteUpdate(id) {
        fetch(getServiceURL('calendar/updateFilter',
            { id }), {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        })
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then(this.saveUpdateResponseInState)
            .catch(error => alert(`Internal error: ${error}`));
    }

    setTask(taskId, selectedTask) {
        if (selectedTask) { // Only given by TaskTreePanel.
            this.setState({ taskTreePanelVisible: false });
        }
        const { id, changeDataField, onKost2Changed } = this.props;
        if (!taskId) {
            this.setState({ task: undefined });
            changeDataField(id, undefined);
            onKost2Changed();
            // this.setState({ taskTreePanelVisible: false }); // Keep tree open after unselect all.
            this.setState({
                taskTreeModalHighlight: undefined, // No task highlighted, show all.
            });
            return;
        }
        fetch(getServiceURL(`task/info/${taskId}`), {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        })
            .then(response => response.json())
            .then((json) => {
                const task = json;
                this.setState({ task });
                if (task) {
                    const newTask = { id: task.id };
                    changeDataField(id, newTask);
                    if (onKost2Changed) {
                        onKost2Changed(task.kost2List);
                    }
                    this.setState({ task });
                }
            })
            .catch(() => this.setState({}));
    }

    /**
     * Alert if clicked on outside of element
     */
    handleClickOutside(event) {
        if (this.taskPanelRef && !this.taskPanelRef.current.contains(event.target)) {
            this.setState({ taskTreePanelVisible: false });
        }
    }

    toggleTaskTreeModal() {
        this.setState(prevState => ({
            taskTreePanelVisible: !prevState.taskTreePanelVisible,
            taskTreeModalHighlight: undefined, // Reset to highlight current task.
        }));
    }

    // Opens the task tree modal dialog with the given task highlighted.
    openTaskTreeModal(taskId) {
        this.setState({
            taskTreePanelVisible: true,
            taskTreeModalHighlight: taskId, // Highlight selected ancestor task.
        });
    }

    render() {
        const {
            taskTreePanelVisible,
            taskTreeModalHighlight,
            task,
            favorites,
        } = this.state;
        const {
            translations,
            showInline,
            label,
            showRootForAdmins,
        } = this.props;
        const labelElement = task ? '' : <span className={style.text}>{label}</span>;

        let taskPath;
        if (task) {
            const path = [...task.path];
            path.push(task);
            let recentAncestorId;
            taskPath = path.map((ancestor) => {
                let removeLink;
                {
                    const parentTaskId = recentAncestorId;
                    removeLink = (
                        <Button
                            color="link"
                            onClick={() => this.setTask(parentTaskId)}
                            style={{ padding: '0px' }}
                        >
                            <FontAwesomeIcon
                                icon={faTimesCircle}
                                className={style.icon}
                                color="lightGray"
                            />
                        </Button>
                    );
                }
                recentAncestorId = ancestor.id;
                const title = (ancestor.id === taskTreeModalHighlight)
                    ? <span className={taskStyle.highlighted}>{ancestor.title}</span> : (
                        <span
                            className="onclick"
                            onClick={() => this.openTaskTreeModal(ancestor.id)}
                            role="presentation"
                        >
                            {ancestor.title}
                        </span>
                    );
                return (
                    <React.Fragment key={ancestor.id}>
                        {title}
                        {' '}
                        {removeLink}
                        <span style={{
                            fontWeight: 'bold',
                            color: 'red',
                            fontSize: '1.2em',
                        }}
                        >
                            {' | '}
                        </span>
                    </React.Fragment>
                );
            });
        }
        const taskTreePanel = (
            <TaskTreePanel
                onTaskSelect={this.setTask}
                highlightTaskId={taskTreeModalHighlight || (task ? task.id : undefined)}
                shortForm
                showRootForAdmins={showRootForAdmins}
                visible={taskTreePanelVisible}
            />
        );
        const taskPanel = showInline ? (
            <Collapse
                isOpen={taskTreePanelVisible}
                style={{
                    maxHeight: '600px',
                    overflow: 'scroll',
                    scroll: 'auto',
                }}
            >
                {taskTreePanel}
            </Collapse>
        ) : (
            <Modal
                isOpen={taskTreePanelVisible}
                className="modal-xl"
                toggle={this.toggleTaskTreeModal}
                fade={false}
            >
                <ModalHeader
                    toggle={this.toggleTaskTreeModal}
                >
                    {translations['task.title.list.select']}
                </ModalHeader>
                <ModalBody>
                    {taskTreePanel}
                </ModalBody>
            </Modal>
        );
        return (
            <div ref={this.taskPanelRef}>
                {labelElement}
                {taskPath}
                <Button
                    color="link"
                    className="selectPanelIconLinks"
                    onClick={this.toggleTaskTreeModal}
                >
                    <FontAwesomeIcon
                        icon={faStream}
                        className={style.icon}
                    />
                </Button>
                <FavoritesPanel
                    onFavoriteCreate={this.onFavoriteCreate}
                    onFavoriteDelete={this.onFavoriteDelete}
                    onFavoriteRename={this.onFavoriteRename}
                    onFavoriteSelect={this.onFavoriteSelect}
                    onFavoriteUpdate={this.onFavoriteUpdate}
                    favorites={favorites}
                    translations={translations}
                />
                {taskPanel}
            </div>
        );
    }
}

TaskSelect.propTypes = {
    changeDataField: PropTypes.func.isRequired,
    data: PropTypes.shape({}).isRequired,
    variables: PropTypes.shape({}).isRequired,
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    onKost2Changed: PropTypes.func,
    showInline: PropTypes.bool,
    showRootForAdmins: PropTypes.bool,
    translations: PropTypes.shape({}).isRequired,
};

TaskSelect.defaultProps = {
    onKost2Changed: undefined,
    showInline: true,
    showRootForAdmins: false,
};

export default TaskSelect;

import { faStream } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import FavoritesPanel from '../../../../../../containers/panel/favorite/FavoritesPanel';
import TaskTreePanel from '../../../../../../containers/panel/task/TaskTreePanel';
import { getServiceURL, handleHTTPErrors } from '../../../../../../utilities/rest';
import { Button, Collapse, Modal, ModalBody, ModalHeader } from '../../../../../design';
import inputStyle from '../../../../../design/input/Input.module.scss';
import { DynamicLayoutContext } from '../../../context';
import TaskPath from './TaskPath';

function DynamicTaskSelect(
    {
        id,
        label,
        onKost2Changed,
        showInline,
        showRootForAdmins,
    },
) {
    const { setData, ui, variables } = React.useContext(DynamicLayoutContext);

    const [panelVisible, setPanelVisible] = React.useState(false);
    const [modalHighlight, setModalHighlight] = React.useState(undefined);
    const [task, setStateTask] = React.useState(undefined);
    const [favorites, setFavorites] = React.useState(undefined);
    const panelRef = React.useRef(null);

    // Handle mouse events
    React.useEffect(() => {
        const handleClickOutside = ({ target }) => {
            if (panelRef.current && !panelRef.current.contains(target)) {
                setPanelVisible(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);

        setStateTask(variables.task);

        fetch(
            getServiceURL('task/favorites/list'),
            {
                method: 'GET',
                credentials: 'include',
                headers: {
                    Accept: 'application/json',
                },
            },
        )
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then(json => setFavorites(json))
            .catch(error => alert(`Internal error: ${error}`));

        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    return React.useMemo(() => {
        const setTask = (taskId, selectedTask) => {
            if (selectedTask) {
                setPanelVisible(false);
            }

            if (!taskId) {
                setStateTask(undefined);
                setData({ [id]: undefined });

                // Emit onKost2Changed handler if defined.
                if (onKost2Changed) {
                    onKost2Changed();
                }

                setModalHighlight(undefined);

                return;
            }

            fetch(
                getServiceURL(`task/info/${taskId}`),
                {
                    method: 'GET',
                    credentials: 'include',
                    headers: {
                        Accept: 'application/json',
                    },
                },
            )
                .then(handleHTTPErrors)
                .then(response => response.json())
                .then((json) => {
                    setStateTask(json);

                    if (json) {
                        setData({ [id]: { id: json.id } });

                        if (onKost2Changed) {
                            onKost2Changed(json.kost2List);
                        }

                        setStateTask(json);
                    }
                });
        };

        const handleFavoriteCreate = (name) => {
            if (!task) {
                // Do nothing: can't set none existing task as favorite.
                return;
            }

            fetch(
                getServiceURL(
                    'task/favorites/create',
                    {
                        name,
                        taskId: task.id,
                    },
                ),
                {
                    method: 'GET',
                    credentials: 'include',
                    headers: {
                        Accept: 'application/json',
                    },
                },
            )
                .then(handleHTTPErrors)
                .then(response => response.json())
                .then(setFavorites)
                .catch(error => alert(`Internal error: ${error}`));
        };

        const handleFavoriteDelete = favoriteId => fetch(
            getServiceURL('task/favorites/delete', { id: favoriteId }),
            {
                method: 'GET',
                credentials: 'include',
                headers: {
                    Accept: 'application/json',
                },
            },
        )
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then(setFavorites)
            .catch(error => alert(`Internal error: ${error}`));

        const handleFavoriteSelect = favoriteId => fetch(
            getServiceURL('task/favorites/select', { id: favoriteId }),
            {
                method: 'GET',
                credentials: 'include',
                headers: {
                    Accept: 'application/json',
                },
            },
        )
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then(setTask)
            .catch(error => alert(`Internal error: ${error}`));

        const handleFavoriteRename = (favoriteId, newName) => fetch(
            getServiceURL(
                'task/favorites/rename',
                {
                    id: favoriteId,
                    newName,
                },
            ),
            {
                method: 'GET',
                credentials: 'include',
                headers: {
                    Accept: 'application/json',
                },
            },
        )
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then(setFavorites)
            .catch(error => alert(`Internal error: ${error}`));

        const toggleModal = () => {
            setPanelVisible(!panelVisible);
            setModalHighlight(undefined); // Reset to highlight current task.
        };

        // Opens the task tree modal dialog with the given task highlighted.
        const openModal = (taskId) => {
            setPanelVisible(true);
            setModalHighlight(taskId); // Highlight selected ancestor task.
        };

        const treePanel = (
            <TaskTreePanel
                highlightTaskId={modalHighlight || (task ? task.id : undefined)}
                onTaskSelect={setTask}
                shortForm
                showRootForAdmins={showRootForAdmins}
                visible={panelVisible}
            />
        );

        return (
            <div>
                {task
                    ? (
                        <TaskPath
                            path={[...task.path, task]}
                            openModal={openModal}
                            setTask={setTask}
                        />
                    )
                    : <span className={inputStyle.text}>{label}</span>}
                <Button
                    color="link"
                    className="selectPanelIconLinks"
                    onClick={toggleModal}
                >
                    <FontAwesomeIcon
                        icon={faStream}
                        className={inputStyle.icon}
                    />
                </Button>
                <FavoritesPanel
                    onFavoriteDelete={handleFavoriteDelete}
                    onFavoriteRename={handleFavoriteRename}
                    onFavoriteSelect={handleFavoriteSelect}
                    onFavoriteCreate={handleFavoriteCreate}
                    favorites={favorites}
                    translations={ui.translations}
                    htmlId="taskFavoritesPopover"
                />
                {showInline
                    ? (
                        <Collapse
                            isOpen={panelVisible}
                            style={{
                                maxHeight: '600px',
                                overflow: 'scroll',
                                scroll: 'auto',
                            }}
                        >
                            {treePanel}
                        </Collapse>
                    )
                    : (
                        <Modal
                            isOpen={panelVisible}
                            className="modal-xl"
                            toggle={toggleModal}
                            fade={false}
                        >
                            <ModalHeader toggle={toggleModal}>
                                {ui.translations['task.title.list.select']}
                            </ModalHeader>
                            <ModalBody>
                                {treePanel}
                            </ModalBody>
                        </Modal>
                    )}
            </div>
        );
    }, [panelVisible, modalHighlight, task, panelRef, favorites]);
}

DynamicTaskSelect.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    onKost2Changed: PropTypes.func,
    showInline: PropTypes.bool,
    showRootForAdmins: PropTypes.bool,
};

DynamicTaskSelect.defaultProps = {
    onKost2Changed: undefined,
    showInline: true,
    showRootForAdmins: false,
};

export default DynamicTaskSelect;

import { faStream } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import FavoritesPanel from '../../../../../../containers/panel/favorite/FavoritesPanel';
import TaskTreePanel from '../../../../../../containers/panel/task/TaskTreePanel';
import { useClickOutsideHandler } from '../../../../../../utilities/hooks';
import { getServiceURL, handleHTTPErrors } from '../../../../../../utilities/rest';
import { Button, Collapse } from '../../../../../design';
import inputStyle from '../../../../../design/input/Input.module.scss';
import { DynamicLayoutContext } from '../../../context';
import TaskPath from './TaskPath';
import taskStyle from './TaskSelect.module.scss';

function DynamicTaskSelect(
    {
        id,
        label,
        onKost2Changed,
        showRootForAdmins,
    },
) {
    const { setData, ui, variables } = React.useContext(DynamicLayoutContext);

    const [panelVisible, setPanelVisible] = React.useState(false);
    const [modalHighlight, setModalHighlight] = React.useState(undefined);
    const [task, setStateTask] = React.useState(undefined);
    const [favorites, setFavorites] = React.useState(undefined);
    const panelRef = React.useRef(null);

    // Handling Mouse Events
    useClickOutsideHandler(panelRef, () => setPanelVisible(false), panelVisible);

    const fetchFavorites = (action, params = {}, callback = setFavorites) => fetch(
        getServiceURL(`task/favorites/${action}`, params),
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
        .then(callback)
        .catch(error => alert(`Internal error: ${error}`));

    // Initial Fetch
    React.useEffect(() => {
        fetchFavorites('list');
    }, []);

    React.useEffect(() => {
        if (variables.task) {
            setStateTask(variables.task);
        }
    }, [variables]);

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
            if (task) {
                fetchFavorites('create', {
                    name,
                    taskId: task.id,
                });
            }
        };
        const handleFavoriteDelete = favoriteId => fetchFavorites('delete', { id: favoriteId });
        const handleFavoriteSelect = favoriteId => fetchFavorites('select', { id: favoriteId }, setTask);
        const handleFavoriteRename = (favoriteId, newName) => fetchFavorites('rename', {
            id: favoriteId,
            newName,
        });

        const toggleModal = () => {
            setPanelVisible(!panelVisible);
            setModalHighlight(undefined); // Reset to highlight current task.
        };

        // Opens the task tree modal dialog with the given task highlighted.
        const openModal = (taskId) => {
            setPanelVisible(true);
            setModalHighlight(taskId); // Highlight selected ancestor task.
        };

        return (
            <div ref={panelRef}>
                {task && task.path
                    ? (
                        <TaskPath
                            path={[...task.path, task]}
                            openModal={openModal}
                            setTask={(taskId) => {
                                openModal(taskId);
                                setTask(taskId);
                            }}
                        />
                    )
                    : (
                        <span className={inputStyle.text}>
                            {label || ui.translations['select.placeholder']}
                        </span>
                    )}
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
                <Collapse
                    isOpen={panelVisible}
                    className={taskStyle.taskCollapse}
                >
                    <TaskTreePanel
                        highlightTaskId={modalHighlight || (task ? task.id : undefined)}
                        onTaskSelect={setTask}
                        shortForm
                        showRootForAdmins={showRootForAdmins}
                        visible={panelVisible}
                    />
                </Collapse>
            </div>
        );
    }, [panelVisible, modalHighlight, task, panelRef, favorites, setData]);
}

DynamicTaskSelect.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string,
    onKost2Changed: PropTypes.func,
    showRootForAdmins: PropTypes.bool,
};

DynamicTaskSelect.defaultProps = {
    label: undefined,
    onKost2Changed: undefined,
    showRootForAdmins: false,
};

export default DynamicTaskSelect;

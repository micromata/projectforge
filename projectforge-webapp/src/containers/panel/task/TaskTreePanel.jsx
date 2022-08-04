import AwesomeDebouncePromise from 'awesome-debounce-promise';
import PropTypes from 'prop-types';
import React from 'react';
import { Card, CardBody } from 'reactstrap';
import LoadingContainer from '../../../components/design/loading-container';
import { debouncedWaitTime, getServiceURL, handleHTTPErrors } from '../../../utilities/rest';
import TaskTreeTable from './TaskTreeTable';
import TaskFilter from './TaskFilter';
import TaskTreeContext, { taskTreeContextDefaultValues } from './TaskTreeContext';

const loadTasksBounced = (
    {
        close,
        filter,
        highlightTaskId,
        initial,
        loading,
        open,
        setFilter,
        setLoading,
        setNodes,
        setColumnDefs,
        setTranslations,
        showRootForAdmins,
    },
) => {
    setLoading(loading || initial);

    fetch(
        getServiceURL(
            'task/tree',
            {
                table: true,
                initial: initial || '',
                open: open || '',
                highlightedTaskId: highlightTaskId || '',
                close: close || '',
                ...filter,
                showRootForAdmins,
            },
        ),
        {
            method: 'GET',
            credentials: 'include',
            headers: { Accept: 'application/json' },
        },
    )
        .then(handleHTTPErrors)
        .then((response) => response.json())
        .then((
            {
                nodes,
                columnDefs,
                translations: responseTranslations,
                initFilter,
            },
        ) => {
            if (responseTranslations) {
                setTranslations(responseTranslations);
            }

            setNodes(nodes);

            // TODO: SCROLL TO HIGHLIGHTED TASK

            if (initial) {
                if (initFilter) {
                    setFilter(initFilter);
                }
                if (columnDefs) {
                    setColumnDefs(columnDefs);
                }
            }
            setLoading(false);
        })
        // TODO: ERROR HANDLING
        .catch(() => setLoading(false));
};

function TaskTreePanel(
    {
        highlightTaskId,
        onTaskSelect: selectTask,
        shortForm,
        showRootForAdmins,
        visible,
        consumptionBarClickable,
    },
) {
    const [loading, setLoading] = React.useState(false);
    const [filter, setFilter] = React.useState({
        searchString: '',
        opened: true,
        notOpened: true,
        closed: false,
        deleted: false,
    });
    const [translations, setTranslations] = React.useState({});
    const [nodes, setNodes] = React.useState([]);
    const [columnDefs, setColumnDefs] = React.useState([]);
    const [loadTasksDebounced] = React.useState(
        () => AwesomeDebouncePromise(loadTasksBounced, debouncedWaitTime),
    );

    const loadTasks = (
        {
            initial,
            open,
            close,
            filter: newFilter,
        } = {},
    ) => {
        let loadFunction = loadTasksDebounced;

        if (initial || open || close) {
            loadFunction = loadTasksBounced;
        }

        loadFunction({
            close,
            filter: newFilter || filter,
            highlightTaskId,
            initial,
            loading,
            open,
            setFilter,
            setLoading,
            setNodes,
            setColumnDefs,
            setTranslations,
            showRootForAdmins,
        });
    };

    const toggleTask = (taskId, from) => {
        const options = {};

        switch (from) {
            case 'CLOSED':
                options.open = taskId;
                break;
            case 'OPENED':
                options.close = taskId;
                break;
            default:
        }

        loadTasks(options);
    };

    const handleCheckBoxChange = ({ target }) => {
        const newFilter = {
            ...filter,
            [target.id]: target.checked,
        };
        setFilter(newFilter);
        loadTasks({ filter: newFilter });
    };

    const handleSearchChange = ({ target }) => {
        const newFilter = {
            ...filter,
            searchString: target.value,
        };
        setFilter(newFilter);
        loadTasks({ filter: newFilter });
    };

    // Reload Tasks when highlight task id changes
    React.useEffect(() => {
        if (visible && !loading) {
            loadTasks({ initial: true });
        }
    }, [highlightTaskId]);

    // Reload Tasks when it is not currently loading, the panel is visible and the translations
    // are missing.
    React.useEffect(() => {
        if (!loading && visible && Object.isEmpty(translations)) {
            loadTasks({ initial: true });
        }
    }, [visible, loading, translations]);

    return (
        <LoadingContainer loading={loading}>
            <Card>
                <CardBody>
                    <TaskTreeContext.Provider
                        /* eslint-disable-next-line react/jsx-no-constructed-context-values */
                        value={{
                            ...taskTreeContextDefaultValues,
                            highlightTaskId,
                            selectTask,
                            shortForm,
                            toggleTask,
                            translations,
                        }}
                    >
                        <TaskFilter
                            filter={filter}
                            onSubmit={() => loadTasks()}
                            onCheckBoxChange={handleCheckBoxChange}
                            onChange={handleSearchChange}
                        />
                        <TaskTreeTable
                            nodes={nodes}
                            columnDefs={columnDefs}
                            consumptionBarClickable={consumptionBarClickable}
                            selectTask={selectTask}
                        />
                    </TaskTreeContext.Provider>
                </CardBody>
            </Card>
        </LoadingContainer>
    );
}

TaskTreePanel.propTypes = {
    visible: PropTypes.bool.isRequired,
    highlightTaskId: PropTypes.number,
    onTaskSelect: PropTypes.func,
    shortForm: PropTypes.bool,
    consumptionBarClickable: PropTypes.bool,
    showRootForAdmins: PropTypes.bool,
};

TaskTreePanel.defaultProps = {
    highlightTaskId: undefined,
    onTaskSelect: undefined,
    shortForm: false,
    showRootForAdmins: false,
    consumptionBarClickable: true,
};

export default TaskTreePanel;

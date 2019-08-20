import PropTypes from 'prop-types';
import React from 'react';
import LoadingContainer from '../../../components/design/loading-container';
import { getServiceURL, handleHTTPErrors } from '../../../utilities/rest';
import TaskTreeTable from './table/TaskTreeTable';
import TaskFilter from './TaskFilter';
import TaskTreeContext, { taskTreeContextDefaultValues } from './TaskTreeContext';

function NewTaskTreePanel(
    {
        highlightTaskId,
        onTaskSelect: selectTask,
        shortForm,
        showRootForAdmins,
        visible,
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
    const [columnsVisibility, setColumnsVisibility] = React.useState({});
    const scrollToRef = React.useRef(undefined);

    const loadTasks = (initial, open, close) => {
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
            .then(response => response.json())
            .then((
                {
                    root,
                    translations: responseTranslations,
                    initFilter,
                    columnsVisibility: responseColumnsVisibility,
                },
            ) => {
                if (responseTranslations) {
                    setTranslations(responseTranslations);
                }

                // TODO: RENAME CHILDS TO CHILDREN
                setNodes(root.childs);
                setColumnsVisibility(responseColumnsVisibility);

                if (initial && scrollToRef.current) {
                    // Scroll only once on initial call to highlighted row:
                    window.scrollTo(0, scrollToRef.current.offsetTop);
                }

                if (initial && initFilter) {
                    setFilter(initFilter);
                }

                setLoading(false);
            })
            // TODO: ERROR HANDLING
            .catch(() => setLoading(false));
    };

    const toggleTask = (taskId, from) => loadTasks(
        false,
        from === 'CLOSED' ? taskId : undefined,
        from === 'OPENED' ? taskId : undefined,
    );

    const handleCheckBoxChange = ({ target }) => setFilter({
        ...filter,
        [target.id]: target.checked,
    });

    const handleSearchChange = ({ target }) => setFilter({
        ...filter,
        searchString: target.value,
    });

    // Reload Tasks when highlight task id changes
    React.useEffect(() => {
        if (visible && !loading) {
            loadTasks(true);
        }
    }, [highlightTaskId]);

    // Reload Tasks when it is not currently loading, the panel is visible and the translations
    // are missing.
    React.useEffect(() => {
        if (!loading && visible && Object.isEmpty(translations)) {
            loadTasks(true);
        }
    }, [visible, loading, translations]);

    return (
        <LoadingContainer loading={loading}>
            <TaskTreeContext.Provider
                value={{
                    ...taskTreeContextDefaultValues,
                    columnsVisibility,
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
                />
            </TaskTreeContext.Provider>
        </LoadingContainer>
    );
}

NewTaskTreePanel.propTypes = {
    visible: PropTypes.bool.isRequired,
    highlightTaskId: PropTypes.number,
    onTaskSelect: PropTypes.func,
    shortForm: PropTypes.bool,
    showRootForAdmins: PropTypes.bool,
};

NewTaskTreePanel.defaultProps = {
    highlightTaskId: undefined,
    onTaskSelect: () => undefined,
    shortForm: false,
    showRootForAdmins: false,
};

export default NewTaskTreePanel;

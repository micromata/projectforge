import PropTypes from 'prop-types';
import React from 'react';
import { Alert } from 'reactstrap';
import DynamicAgGrid from '../../../components/base/dynamicLayout/components/table/DynamicAgGrid';
import TaskTreeContext from './TaskTreeContext';

function TaskTreeTable({
    columnDefs,
    nodes,
    selectTask,
    sortModel,
    filterModel,
    onColumnStatesChangedUrl,
    resetGridStateUrl,
    // visible,
}) {
    const {
        highlightTaskId,
    } = React.useContext(TaskTreeContext);

    const { toggleTask } = React.useContext(TaskTreeContext);

    const onGridApiReady = React.useCallback(() => {
        // Grid API ready callback - can be used for future enhancements
    }, []);

    const onCellClicked = (event) => {
        const { data, colDef } = event;
        const { field } = colDef;
        const { treeStatus, id } = data;
        if (field !== 'title' || treeStatus === 'LEAF') {
            selectTask(id, data);
        } else {
            toggleTask(id, treeStatus);
        }
    };

    const {
        translations,
    } = React.useContext(TaskTreeContext);

    return (
        <div>
            <div className="table-responsive">
                <DynamicAgGrid
                    onGridApiReady={onGridApiReady}
                    columnDefs={columnDefs}
                    entries={nodes}
                    height={400}
                    onCellClicked={onCellClicked}
                    highlightId={highlightTaskId}
                    sortModel={sortModel}
                    filterModel={filterModel}
                    onColumnStatesChangedUrl={onColumnStatesChangedUrl}
                    resetGridStateUrl={resetGridStateUrl}
                    // visible={visible}
                />
            </div>
            <Alert color="light">
                {translations['task.selectPanel.info']}
            </Alert>
        </div>
    );
}

TaskTreeTable.propTypes = {
    columnDefs: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.string,
        title: PropTypes.string,
        titleIcon: PropTypes.arrayOf(PropTypes.string),
    })).isRequired,
    nodes: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
    selectTask: PropTypes.func.isRequired,
    sortModel: PropTypes.arrayOf(PropTypes.shape({})),
    filterModel: PropTypes.shape({}),
    onColumnStatesChangedUrl: PropTypes.string,
    resetGridStateUrl: PropTypes.string,
    // visible: PropTypes.bool,
};

export default TaskTreeTable;

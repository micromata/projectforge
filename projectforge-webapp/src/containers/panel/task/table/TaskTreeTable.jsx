import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { Alert } from 'reactstrap';
import DynamicAgGrid from '../../../../components/base/dynamicLayout/components/table/DynamicAgGrid';
import TaskTreeContext from '../TaskTreeContext';

function TaskTreeTable({
    columnDefs, nodes, selectTask,
}) {
    const {
        highlightTaskId,
    } = React.useContext(TaskTreeContext);

    const [gridApi, setGridApi] = useState();
    const [columnApi, setColumnApi] = useState();

    const { toggleTask } = React.useContext(TaskTreeContext);

    const onGridApiReady = React.useCallback((api, colApi) => {
        setGridApi(api);
        setColumnApi(colApi);
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
        <div className="table-responsive">
            <DynamicAgGrid
                onGridApiReady={onGridApiReady}
                columnDefs={columnDefs}
                entries={nodes}
                height={400}
                onCellClicked={onCellClicked}
                highlightId={highlightTaskId}
            />
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
};

TaskTreeTable.defaultProps = {
};

export default TaskTreeTable;

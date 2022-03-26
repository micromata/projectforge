import PropTypes from 'prop-types';
import React from 'react';
import { AgGridReact } from 'ag-grid-react';
import { DynamicLayoutContext } from '../../context';

import 'ag-grid-community/dist/styles/ag-grid.css';
import 'ag-grid-community/dist/styles/ag-theme-alpine.css';

function DynamicListPageAgGrid({
    columnDefs,
    id,
    rowSelection,
    rowMultiSelectWithClick,
}) {
    const { data, ui } = React.useContext(DynamicLayoutContext);

    const gridStyle = React.useMemo(() => ({ width: '100%' }), []);
    const entries = Object.getByString(data, id) || '';

    const onGridReady = React.useCallback((params) => {
        const gridApi = params.api;
        gridApi.setDomLayout('autoHeight');
    }, []);

    // getSelectedNodes

    return React.useMemo(() => (
        <div
            className="ag-theme-alpine"
            style={gridStyle}
        >
            <AgGridReact
                rowData={entries}
                columnDefs={columnDefs}
                rowSelection={rowSelection}
                rowMultiSelectWithClick={rowMultiSelectWithClick}
                onGridReady={onGridReady}
            />
            <code>
                TODO
                <ul>
                    <li>Doc: You may use the shift-Key for multiselection on mouse clicks.</li>
                    <li>
                        Doc: You may use cursor up- and down keys to navigate and user space
                        to (de)select rows.
                    </li>
                </ul>
            </code>
        </div>
    ), [entries, ui]);
}

DynamicListPageAgGrid.propTypes = {
    columnDefs: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.string.isRequired,
        title: PropTypes.string,
        titleIcon: PropTypes.arrayOf(PropTypes.string),
    })).isRequired,
    id: PropTypes.string,
    rowSelection: PropTypes.string,
    rowMultiSelectWithClick: PropTypes.bool,
};

DynamicListPageAgGrid.defaultProps = {
    id: undefined,
};

export default DynamicListPageAgGrid;

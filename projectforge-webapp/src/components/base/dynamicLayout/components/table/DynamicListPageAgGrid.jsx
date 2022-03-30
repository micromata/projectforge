import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { AgGridReact } from 'ag-grid-react';
import { DynamicLayoutContext } from '../../context';
import { Button } from '../../../../design';
import { fetchJsonPost } from '../../../../../utilities/rest';
import 'ag-grid-community/dist/styles/ag-grid.css';
import 'ag-grid-community/dist/styles/ag-theme-alpine.css';
import history from '../../../../../utilities/history';

function DynamicListPageAgGrid({
    columnDefs,
    id,
    rowSelection,
    rowMultiSelectWithClick,
    multiSelectButtonTitle,
    urlAfterMultiSelect,
}) {
    const { data, ui } = React.useContext(DynamicLayoutContext);
    const [gridApi, setGridApi] = useState();

    const gridStyle = React.useMemo(() => ({ width: '100%' }), []);
    const entries = Object.getByString(data, id) || '';
    const { selectedEntityIds } = data;

    const onGridReady = React.useCallback((params) => {
        setGridApi(params.api);
        params.api.setDomLayout('autoHeight'); // Needed to get maximum height.
    }, [selectedEntityIds, setGridApi]);

    React.useEffect(() => {
        if (gridApi && selectedEntityIds) {
            console.log({ selectedEntityIds });
            gridApi.forEachNode((node) => {
                const row = node.data;
                // Recover previous selected nodes from server (if any):
                node.setSelected(selectedEntityIds.includes(row.id));
            });
        }
    }, [gridApi, selectedEntityIds]);

    const handleClick = React.useCallback((event) => {
        event.preventDefault();
        event.stopPropagation();
        if (!gridApi) return;
        const selectedIds = gridApi.getSelectedRows().map((item) => item.id);
        // console.log(event.target.id, selectedIds);
        fetchJsonPost(urlAfterMultiSelect,
            { selectedIds },
            (json) => {
                const { url } = json;
                history.push(url);
            });
    }, [gridApi]);

    // getSelectedNodes
    return React.useMemo(() => (
        <div
            className="ag-theme-alpine"
            style={gridStyle}
        >
            {multiSelectButtonTitle && (
                // Show this button only for multi selection with e. g. mass update:
                <Button
                    id="next"
                    onClick={handleClick}
                    color="success"
                    outline
                >
                    {multiSelectButtonTitle}
                </Button>
            )}
            <AgGridReact
                rowData={entries}
                columnDefs={columnDefs}
                rowSelection={rowSelection}
                rowMultiSelectWithClick={rowMultiSelectWithClick}
                onGridReady={onGridReady}
            />
        </div>
    ),
    [
        entries,
        ui,
        handleClick,
        gridStyle,
        multiSelectButtonTitle,
        columnDefs,
        rowSelection,
        rowMultiSelectWithClick,
        onGridReady,
    ]);
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
    multiSelectButtonTitle: PropTypes.string,
    urlAfterMultiSelect: PropTypes.string,
};

DynamicListPageAgGrid.defaultProps = {
    id: undefined,
};

export default DynamicListPageAgGrid;

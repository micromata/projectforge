import PropTypes from 'prop-types';
import React from 'react';
import { AgGridReact } from 'ag-grid-react';
import { DynamicLayoutContext } from '../../context';
import { Button } from '../../../../design';
import { fetchJsonPost, handleHTTPErrors } from '../../../../../utilities/rest';
import 'ag-grid-community/dist/styles/ag-grid.css';
import 'ag-grid-community/dist/styles/ag-theme-alpine.css';
import history from '../../../../../utilities/history';

function DynamicListPageAgGrid({
    columnDefs,
    id,
    rowSelection,
    rowMultiSelectWithClick,
    multiSelectButtonTitle,
    urlForMultiSelect,
}) {
    const { data, ui } = React.useContext(DynamicLayoutContext);

    const gridStyle = React.useMemo(() => ({ width: '100%' }), []);
    const entries = Object.getByString(data, id) || '';

    let gridApi;

    const onGridReady = React.useCallback((params) => {
        gridApi = params.api;
        gridApi.setDomLayout('autoHeight'); // Needed to get maximum height.
    }, []);

    const handleClick = React.useCallback((event) => {
        event.preventDefault();
        event.stopPropagation();
        const selectedIds = gridApi.getSelectedRows().map((item) => item.id);
        // console.log(event.target.id, selectedIds);
        fetchJsonPost(urlForMultiSelect, { selectedIds })
            .then(handleHTTPErrors)
            .then((response) => response.json())
            .then(({ targetType, url: redirectUrl }) => {
                switch (targetType) {
                    case 'REDIRECT':
                        history.push(redirectUrl);
                        break;
                    default:
                        // TODO: do nothing
                }
            });
    }, []);

    // getSelectedNodes
    return React.useMemo(() => (
        <div
            className="ag-theme-alpine"
            style={gridStyle}
        >
            <Button
                id="next"
                onClick={handleClick}
                color="success"
                outline
            >
                {multiSelectButtonTitle}
            </Button>
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
    multiSelectButtonTitle: PropTypes.string,
    urlForMultiSelect: PropTypes.string,
};

DynamicListPageAgGrid.defaultProps = {
    id: undefined,
};

export default DynamicListPageAgGrid;

import PropTypes from 'prop-types';
import React, { useRef } from 'react';
import { Button } from '../../../../design';
import { fetchJsonPost } from '../../../../../utilities/rest';
import 'ag-grid-community/dist/styles/ag-grid.css';
import 'ag-grid-community/dist/styles/ag-theme-alpine.css';
import history from '../../../../../utilities/history';
import DynamicAgGrid from './DynamicAgGrid';

function DynamicListPageAgGrid({
    columnDefs,
    id,
    rowSelection,
    rowMultiSelectWithClick,
    multiSelectButtonTitle,
    urlAfterMultiSelect,
}) {
    const agGridRef = useRef();

    const handleClick = React.useCallback((event) => {
        event.preventDefault();
        event.stopPropagation();
        const selectedIds = agGridRef.current.getSelectedIds();
        // console.log(event.target.id, selectedIds);
        fetchJsonPost(urlAfterMultiSelect,
            { selectedIds },
            (json) => {
                const { url } = json;
                history.push(url);
            });
    }, []);

    // getSelectedNodes
    return React.useMemo(() => (
        <div>
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
            <DynamicAgGrid
                ref={agGridRef}
                columnDefs={columnDefs}
                id={id}
                rowSelection={rowSelection}
                rowMultiSelectWithClick={rowMultiSelectWithClick}
            />
        </div>
    ),
    [
        handleClick,
        multiSelectButtonTitle,
        columnDefs,
        rowSelection,
        rowMultiSelectWithClick,
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

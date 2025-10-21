import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { fetchJsonPost, getServiceURL, handleHTTPErrors } from '../../../../../utilities/rest';
import history from '../../../../../utilities/history';
import DynamicAgGrid from './DynamicAgGrid';
import { DynamicLayoutContext } from '../../context';
import DynamicButton from '../DynamicButton';

function DynamicListPageAgGrid({
    columnDefs,
    selectionColumnDef,
    id,
    sortModel,
    rowSelection,
    rowMultiSelectWithClick,
    rowClickRedirectUrl,
    rowClickOpenModal,
    onColumnStatesChangedUrl,
    resetGridStateUrl,
    multiSelectButtonTitle,
    multiSelectButtonConfirmMessage,
    urlAfterMultiSelect,
    handleCancelUrl,
    pagination,
    paginationPageSize,
    paginationPageSizeSelector,
    getRowClass,
}) {
    const [gridApi, setGridApi] = useState();
    const [columnApi, setColumnApi] = useState();

    const onGridApiReady = React.useCallback((api, colApi) => {
        setGridApi(api);
        setColumnApi(colApi);
    }, []);

    React.useEffect(() => {
        if (columnApi) {
            columnApi.resetColumnState();
            columnApi.applyColumnState({
                state: sortModel,
                defaultState: { sort: null },
            });
        }
    }, [columnApi, sortModel]);

    const { ui, callAction } = React.useContext(DynamicLayoutContext);

    const handleCancel = React.useCallback(() => {
        fetch(getServiceURL(handleCancelUrl), {
            method: 'GET',
            credentials: 'include',
        })
            .then(handleHTTPErrors)
            .then((response) => response.text())
            .then((url) => {
                history.push(url);
            });
    }, [gridApi, handleCancelUrl]);

    const handleClick = React.useCallback(() => {
        const selectedIds = gridApi.getSelectedRows().map((item) => item.id);
        fetchJsonPost(
            urlAfterMultiSelect,
            { selectedIds },
            (json) => {
                callAction({ responseAction: json });
            },
        );
    }, [gridApi, urlAfterMultiSelect]);

    // getSelectedNodes
    return React.useMemo(
        () => (
            <div>
                {multiSelectButtonTitle && (
                    // Show these buttons only for multi selection with e. g. mass update:
                    <>
                        <DynamicButton
                            id="cancel"
                            title={ui.translations.cancel || 'cancel'}
                            handleButtonClick={handleCancel}
                            color="danger"
                            outline
                        />
                        <DynamicButton
                            id="next"
                            title={multiSelectButtonTitle || 'next'}
                            handleButtonClick={handleClick}
                            color="success"
                            outline
                            confirmMessage={multiSelectButtonConfirmMessage}
                        />
                    </>
                )}
                <DynamicAgGrid
                    onGridApiReady={onGridApiReady}
                    columnDefs={columnDefs}
                    selectionColumnDef={selectionColumnDef}
                    id={id}
                    sortModel={sortModel}
                    rowSelection={rowSelection}
                    rowClickRedirectUrl={rowClickRedirectUrl}
                    rowClickOpenModal={rowClickOpenModal}
                    onColumnStatesChangedUrl={onColumnStatesChangedUrl}
                    resetGridStateUrl={resetGridStateUrl}
                    pagination={pagination}
                    paginationPageSize={paginationPageSize}
                    paginationPageSizeSelector={paginationPageSizeSelector}
                    getRowClass={getRowClass}
                />
            </div>
        ),
        [
            handleClick,
            multiSelectButtonTitle,
            multiSelectButtonConfirmMessage,
            columnDefs,
            selectionColumnDef,
            sortModel,
            rowSelection,
            rowMultiSelectWithClick,
            paginationPageSize,
            ui,
        ],
    );
}

DynamicListPageAgGrid.propTypes = {
    columnDefs: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.string,
        title: PropTypes.string,
        titleIcon: PropTypes.arrayOf(PropTypes.string),
    })).isRequired,
    selectionColumnDef: PropTypes.shape({
        pinned: PropTypes.string,
        resizable: PropTypes.bool,
        sortable: PropTypes.bool,
        filter: PropTypes.bool,
    }),
    id: PropTypes.string,
    sortModel: PropTypes.arrayOf(PropTypes.shape({
        colId: PropTypes.string.isRequired,
        sort: PropTypes.string,
        sortIndex: PropTypes.number,
    })),
    rowSelection: PropTypes.shape({
        mode: PropTypes.string,
        enableClickSelection: PropTypes.bool,
        enableSelectionWithoutKeys: PropTypes.bool,
    }),
    rowClickRedirectUrl: PropTypes.string,
    rowClickOpenModal: PropTypes.bool,
    onColumnStatesChangedUrl: PropTypes.string,
    resetGridStateUrl: PropTypes.string,
    multiSelectButtonTitle: PropTypes.string,
    multiSelectButtonConfirmMessage: PropTypes.string,
    urlAfterMultiSelect: PropTypes.string,
    handleCancelUrl: PropTypes.string,
    pagination: PropTypes.bool,
    paginationPageSize: PropTypes.number,
    paginationPageSizeSelector: PropTypes.arrayOf(PropTypes.number),
    getRowClass: PropTypes.string,
    rowMultiSelectWithClick: PropTypes.bool,
};

export default DynamicListPageAgGrid;

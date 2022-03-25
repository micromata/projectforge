/* eslint-disable no-unused-vars */
import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { AgGridReact } from 'ag-grid-react';
import { DynamicLayoutContext } from '../../context';

import 'ag-grid-community/dist/styles/ag-grid.css';
import 'ag-grid-community/dist/styles/ag-theme-alpine.css';

function DynamicListPageAgGrid({ columnDefs, id }) {
    const { data, ui } = React.useContext(DynamicLayoutContext);

    const entries = Object.getByString(data, id) || '';

    return React.useMemo(() => (
        <div
            className="ag-theme-alpine"
            style={{
                height: 400,
                width: '100%',
            }}
        >
            <AgGridReact rowData={entries} columnDefs={columnDefs} />
            {data.size !== undefined && (
                <p>
                    {`${ui.translations['table.showing']} ${data.size}/${data.totalSize}`}
                </p>
            )}
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
};

DynamicListPageAgGrid.defaultProps = {
    id: undefined,
};

export default DynamicListPageAgGrid;

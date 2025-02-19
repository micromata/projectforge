import PropTypes from 'prop-types';
import React from 'react';
import { Table } from '../../../../design';
import { DynamicLayoutContext } from '../../context';
import DynamicTableHead from './DynamicListPageTableHead';
import DynamicTableRow from './DynamicListPageTableRow';

function DynamicListPageTable({ columns, id }) {
    const { data, ui } = React.useContext(DynamicLayoutContext);

    const entries = Object.getByString(data, id) || '';

    return React.useMemo(() => (
        <div
            style={{
                marginLeft: '1em',
                marginRight: '1em',
            }}
        >
            <Table striped hover responsive>
                <thead>
                    <tr>
                        {columns.map((column) => (
                            <DynamicTableHead
                                {...column}
                                key={`table-head-column-${column.id}`}
                            />
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {entries.map((row) => (
                        <DynamicTableRow
                            key={`table-body-row-${row.id}`}
                            columns={columns}
                            row={row}
                            highlightRow={data.highlightRowId === row.id}
                        />
                    ))}
                </tbody>
            </Table>
            {data.size !== undefined && (
                <p>
                    {`${ui.translations['table.showing']} ${data.size}/${data.totalSize}`}
                </p>
            )}
        </div>
    ), [entries, ui]);
}

DynamicListPageTable.propTypes = {
    columns: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.string.isRequired,
        title: PropTypes.string,
        titleIcon: PropTypes.arrayOf(PropTypes.string),
    })).isRequired,
    id: PropTypes.string,
};

export default DynamicListPageTable;

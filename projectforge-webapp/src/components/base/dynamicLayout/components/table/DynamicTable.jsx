import PropTypes from 'prop-types';
import React from 'react';
import { Card, CardBody, Table } from '../../../../design';
import { DynamicLayoutContext } from '../../context';
import DynamicTableHead from './DynamicTableHead';
import DynamicTableRow from './DynamicTableRow';

function DynamicTable({ columns, id }) {
    const { data, ui } = React.useContext(DynamicLayoutContext);

    const entries = Object.getByString(data, id) || '';

    return React.useMemo(() => (
        <Card>
            <CardBody>
                <Table striped hover responsive>
                    <thead>
                        <tr>
                            {columns.map(column => (
                                <DynamicTableHead
                                    key={`table-head-column-${column.id}`}
                                    {...column}
                                />
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {entries.map(row => (
                            <DynamicTableRow
                                key={`table-body-row-${row.id}`}
                                columns={columns}
                                row={row}
                            />
                        ))}
                    </tbody>
                </Table>
                {data.size !== undefined
                    ? (
                        <span>
                            {`${ui.translations['table.showing']} ${data.size}/${data.totalSize}`}
                        </span>
                    )
                    : undefined}
            </CardBody>
        </Card>
    ), [entries, ui]);
}

DynamicTable.propTypes = {
    columns: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.string.isRequired,
        title: PropTypes.string.isRequired,
    })).isRequired,
    id: PropTypes.string,
};

DynamicTable.defaultProps = {
    id: undefined,
};

export default DynamicTable;

import PropTypes from 'prop-types';
import React from 'react';
import { Card, CardBody, Table } from '../../../../design';
import TableRow from './Row';

function LayoutTable({ columns, data, id }) {
    let rows = data[id];

    if (rows === undefined) {
        rows = [];
    }

    return (
        <Card>
            <CardBody>
                <Table striped hover responsive>
                    <thead>
                        <tr>
                            {columns.map(column => (
                                <th key={`table-head-column-${column.id}`}>
                                    {column.title}
                                </th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {rows.map(row => (
                            <TableRow
                                key={`table-body-row-${row.id}`}
                                columns={columns}
                                data={row}
                            />
                        ))}
                    </tbody>
                </Table>
            </CardBody>
        </Card>
    );
}

LayoutTable.propTypes = {
    columns: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.string,
        title: PropTypes.string,
    })).isRequired,
    data: PropTypes.shape({}),
    id: PropTypes.string,
};

LayoutTable.defaultProps = {
    data: [],
    id: undefined,
};

export default LayoutTable;

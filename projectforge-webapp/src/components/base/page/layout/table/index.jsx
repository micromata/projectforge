import PropTypes from 'prop-types';
import React from 'react';
import { Card, CardBody, Table } from '../../../../design';
import AnimatedChevron from '../../../../design/input/chevron/Animated';
import TableRow from './Row';

function LayoutTable(
    {
        columns,
        data,
        id,
        sorting,
    },
) {
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
                            {columns.map((column) => {
                                let sortingDirection = 'neutral';

                                if (sorting.column === column.id) {
                                    sortingDirection = sorting.direction === 'ASC' ? 'down' : 'up';
                                }

                                return (
                                    <th key={`table-head-column-${column.id}`}>
                                        <AnimatedChevron direction={sortingDirection} />
                                        {column.title}
                                    </th>
                                );
                            })}
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
                {data.size
                    ? <span>{`[Showing] ${data.size}/${data.totalSize}`}</span>
                    : undefined}
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
    sorting: PropTypes.shape({
        column: PropTypes.string,
        direction: PropTypes.oneOf(['ASC', 'DESC']),
    }),
};

LayoutTable.defaultProps = {
    data: [],
    id: undefined,
    sorting: undefined,
};

export default LayoutTable;

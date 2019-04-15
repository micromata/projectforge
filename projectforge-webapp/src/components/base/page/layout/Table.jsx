import PropTypes from 'prop-types';
import React from 'react';
import format from '../../../../utilities/format';
import history from '../../../../utilities/history';
import { Card, CardBody, Table } from '../../../design';

class LayoutTable extends React.Component {
    handleRowClick(event) {
        history.push(`edit/${event.currentTarget.dataset.id}`);
    }

    render() {
        const { columns, data, id } = this.props;
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
                                <tr
                                    key={`table-body-row-${row.id}`}
                                    onClick={this.handleRowClick}
                                    data-id={row.id}
                                >
                                    {columns.map((column) => {
                                        let value;

                                        if (row[column.id] === undefined) {
                                            value = '';
                                        } else if (column.formatter) {
                                            value = format(column.formatter, row[column.id]);
                                        } else {
                                            value = row[column.id];
                                        }

                                        return (
                                            <td key={`table-body-row-${row.id}-column-${column.id}`}>
                                                {value}
                                            </td>
                                        );
                                    })}
                                </tr>
                            ))}
                        </tbody>
                    </Table>
                </CardBody>
            </Card>
        );
    }
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

import PropTypes from 'prop-types';
import React from 'react';
import { Card, CardBody, Table } from '../../../../design';
import AnimatedChevron from '../../../../design/input/chevron/Animated';
import { DynamicLayoutContext } from '../../context';
import DynamicTableRow from './DynamicTableRow';

function DynamicTable({ columns, id }) {
    const { data, ui } = React.useContext(DynamicLayoutContext);

    return React.useMemo(() => {
        return (
            <Card>
                <CardBody>
                    <Table striped hover responsive>
                        <thead>
                            <tr>
                                {columns.map(({ id: columnId, title }) => (
                                    <th key={`table-head-column-${columnId}`}>
                                        {/* TODO Handle Sorting */}
                                        <AnimatedChevron direction="neutral" />
                                        {title}
                                    </th>
                                ))}
                            </tr>
                        </thead>
                        <tbody>
                            {(data[id] || []).map(row => (
                                <DynamicTableRow
                                    key={`table-body-row-${row.id}`}
                                    columns={columns}
                                    row={row}
                                />
                            ))}
                        </tbody>
                    </Table>
                    {data.size
                        ? (
                            <span>
                                {`${ui.translations['table.showing']} ${data.size}/${data.totalSize}`}
                            </span>
                        )
                        : undefined}
                </CardBody>
            </Card>
        );
    }, [data[id]]);
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

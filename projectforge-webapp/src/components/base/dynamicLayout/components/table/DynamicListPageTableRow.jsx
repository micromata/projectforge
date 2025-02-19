import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { useSelector } from 'react-redux';
import { useNavigate } from 'react-router';
import { tableColumnsPropType } from '../../../../../utilities/propTypes';
import Formatter from '../../../Formatter';
import DynamicCustomized from '../customized';
import style from './DynamicTable.module.scss';

function DynamicListPageTableRow(
    {
        columns,
        row,
        highlightRow = false,
    },
) {
    const list = useSelector((state) => state.list);
    const navigate = useNavigate();

    const handleRowClick = () => navigate(`/${list.categories[list.currentCategory].standardEditPage.replace(':id', row.id)}`);

    return (
        <tr
            onClick={handleRowClick}
            className={classNames(
                style.clickable,
                { [style.highlighted]: highlightRow === true },
                { [style.deleted]: row.deleted === true },
            )}
        >
            {columns.map((
                {
                    id,
                    dataType,
                    ...column
                },
            ) => (
                <td key={`table-body-row-${row.id}-column-${id}`}>
                    {dataType === 'CUSTOMIZED'
                        ? <DynamicCustomized id={id} data={row} />
                        : (
                            <Formatter
                                data={row}
                                id={id}
                                dataType={dataType}
                                {...column}
                                key={column.key}
                            />
                        )}
                </td>
            ))}
        </tr>
    );
}

DynamicListPageTableRow.propTypes = {
    columns: tableColumnsPropType.isRequired,
    row: PropTypes.shape({
        id: PropTypes.number.isRequired,
        deleted: PropTypes.bool,
    }).isRequired,
    highlightRow: PropTypes.bool,
};

export default DynamicListPageTableRow;

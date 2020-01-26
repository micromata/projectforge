import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { openEditPage } from '../../../../../actions';
import { tableColumnsPropType } from '../../../../../utilities/propTypes';
import Formatter from '../../../Formatter';
import DynamicCustomized from '../customized';
import style from './DynamicTable.module.scss';

function DynamicTableRow(
    {
        columns,
        row,
        highlightRow,
        handleRowClick,
    },
) {
    return (
        <tr
            onClick={handleRowClick}
            className={classNames(
                style.clickable,
                { [style.highlighted]: highlightRow === row.id },
                { [style.deleted]: row.deleted === true },
            )}
        >
            {columns.map((
                {
                    id,
                    formatter,
                    dataType,
                },
            ) => (
                <td key={`table-body-row-${row.id}-column-${id}`}>
                    {dataType === 'CUSTOMIZED'
                        ? <DynamicCustomized id={id} data={row} />
                        : (
                            <Formatter
                                formatter={formatter}
                                data={row}
                                id={id}
                                dataType={dataType}
                            />
                        )}
                </td>
            ))}
        </tr>
    );
}

DynamicTableRow.propTypes = {
    columns: tableColumnsPropType.isRequired,
    handleRowClick: PropTypes.func.isRequired,
    row: PropTypes.shape({
        id: PropTypes.number.isRequired,
    }).isRequired,
    highlightRow: PropTypes.number,
};

DynamicTableRow.defaultProps = {
    highlightRow: -1,
};

const mapStateToProps = ({ list }) => ({
    highlightRow: list.categories[list.currentCategory].highlightRow,
});

const actions = (dispatch, { row }) => ({
    handleRowClick: () => dispatch(openEditPage(row.id)),
});

export default connect(mapStateToProps, actions)(DynamicTableRow);

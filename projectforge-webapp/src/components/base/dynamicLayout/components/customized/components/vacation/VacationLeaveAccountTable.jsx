import PropTypes from 'prop-types';
import React from 'react';
import history from '../../../../../../../utilities/history';
import prefix from '../../../../../../../utilities/prefix';
import { Table } from '../../../../../../design';
import { DynamicLayoutContext } from '../../../../context';
import style from './Vacation.module.scss';

function VacationLeaveAccountTable({ entries }) {
    const { ui } = React.useContext(DynamicLayoutContext);

    return (
        <>
            {entries && entries.length > 0 && (
                <>
                    <h4>{ui.translations['vacation.leaveAccountEntry.title.heading']}</h4>
                    <Table striped hover>
                        <thead>
                            <tr>
                                <th>{ui.translations.date}</th>
                                <th className={style.number}>
                                    {ui.translations['vacation.leaveAccountEntry.amount']}
                                </th>
                                <th className={style.fullWidth}>{ui.translations.description}</th>
                            </tr>
                        </thead>
                        <tbody>
                            {entries.map((entry) => (
                                <tr
                                    key={entry.id}
                                    onClick={() => history.push(`${prefix}leaveAccountEntry/edit/${entry.id}?returnToCaller=account`)}
                                >
                                    <td>{entry.dateFormatted}</td>
                                    <td className={style.number}>{entry.amount}</td>
                                    <td>{entry.description}</td>
                                </tr>
                            ))}
                        </tbody>
                    </Table>
                </>
            )}
        </>
    );
}

VacationLeaveAccountTable.propTypes = {
    entries: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.number.isRequired,
        dateFormatted: PropTypes.string,
        amount: PropTypes.number,
        description: PropTypes.string,
    })),
};

VacationLeaveAccountTable.defaultProps = {
    entries: undefined,
};

export default VacationLeaveAccountTable;

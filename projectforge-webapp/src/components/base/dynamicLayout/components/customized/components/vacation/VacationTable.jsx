import React from 'react';
import { Table } from '../../../../../../design';
import { DynamicLayoutContext } from '../../../../context';
import VacationEntries from './VacationEntries';
import styles from './Vacation.module.scss';
import VacationLeaveAccountTable from './VacationLeaveAccountTable';

function VacationTable() {
    const { data, ui } = React.useContext(DynamicLayoutContext);

    if (!data.vacations) {
        return null;
    }

    const {
        vacationsCurrentYear,
        vacationsPreviousYear,
        vacationsNextYear,
        leaveAccountEntries,
    } = data.vacations;

    return (
        <>
            <h4>{ui.translations['vacation.title.list']}</h4>
            <Table striped hover>
                <thead>
                    <tr>
                        <th>{ui.translations['vacation.startdate']}</th>
                        <th>{ui.translations['vacation.enddate']}</th>
                        <th>{ui.translations['vacation.status']}</th>
                        <th className={styles.number}>{ui.translations['vacation.Days']}</th>
                        <th>{ui.translations['vacation.special']}</th>
                        <th>{ui.translations['vacation.replacement']}</th>
                        <th>{ui.translations['vacation.replacement.others']}</th>
                        <th>{ui.translations['vacation.manager']}</th>
                        <th>{ui.translations['vacation.vacationmode']}</th>
                        <th>{ui.translations.comment}</th>
                    </tr>
                </thead>
                <tbody>
                    <VacationEntries entries={vacationsNextYear} />
                    <VacationEntries entries={vacationsCurrentYear} />
                    <VacationEntries entries={vacationsPreviousYear} />
                </tbody>
            </Table>
            <VacationLeaveAccountTable entries={leaveAccountEntries} />
        </>
    );
}

VacationTable.propTypes = {};

export default VacationTable;

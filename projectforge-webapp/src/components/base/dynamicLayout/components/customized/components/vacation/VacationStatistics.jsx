import React from 'react';
import 'react-rrule-generator/build/styles.css';
import { Col, Row, Table } from 'reactstrap';
import { DynamicLayoutContext } from '../../../../context';
import style from './Vacation.module.scss';
import { VacationStatisticsContext } from './VacationStatisticsContext';
import VacationStatisticsEntry from './VacationStatisticsEntry';

function VacationStatistics() {
    const { data, ui } = React.useContext(DynamicLayoutContext);
    const { statistics } = data;

    return React.useMemo(() => {
        if (!statistics) {
            return <span>No statistics found!</span>;
        }

        const { statisticsCurrentYear: current, statisticsPreviousYear: prev } = statistics;

        return (
            <Row>
                <Col sm={12}>
                    <Table striped className={style.statistics}>
                        <VacationStatisticsContext.Provider
                            /* eslint-disable-next-line react/jsx-no-constructed-context-values */
                            value={{
                                current,
                                prev,
                                translations: ui.translations,
                            }}
                        >
                            <thead>
                                <VacationStatisticsEntry
                                    field="year"
                                    isHead
                                    title="vacation.leaveaccount.title"
                                />
                            </thead>
                            <tbody>
                                <VacationStatisticsEntry
                                    title="vacation.annualleave"
                                    field="vacationDaysInYearFromContract"
                                />
                                <VacationStatisticsEntry
                                    title="vacation.previousyearleave"
                                    field="remainingLeaveFromPreviousYear"
                                />
                                <VacationStatisticsEntry
                                    title="vacation.subtotal"
                                    field="totalLeaveIncludingCarry"
                                    highlighted
                                />
                                <VacationStatisticsEntry
                                    title="vacation.previousyearleaveunused"
                                    field="remainingLeaveFromPreviousYearUnused"
                                />
                                <VacationStatisticsEntry
                                    title="menu.vacation.leaveAccountEntry"
                                    field="leaveAccountEntriesSum"
                                />
                                <VacationStatisticsEntry
                                    title="vacation.vacationApproved"
                                    field="vacationDaysApproved"
                                />
                                <VacationStatisticsEntry
                                    title="vacation.vacationInProgress"
                                    field="vacationDaysInProgress"
                                />
                                <VacationStatisticsEntry
                                    title="vacation.availablevacation"
                                    field="vacationDaysLeftInYear"
                                    highlighted
                                />
                                <VacationStatisticsEntry
                                    title="vacation.specialApproved"
                                    field="specialVacationDaysApproved"
                                />
                                <VacationStatisticsEntry
                                    title="vacation.specialInProgress"
                                    field="specialVacationDaysInProgress"
                                />
                            </tbody>
                        </VacationStatisticsContext.Provider>
                    </Table>
                </Col>
            </Row>
        );
    }, [statistics, ui.translations]);
}

VacationStatistics.propTypes = {};

export default VacationStatistics;

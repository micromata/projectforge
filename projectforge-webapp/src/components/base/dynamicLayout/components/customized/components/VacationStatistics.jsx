/* eslint-disable react/jsx-indent,indent,max-len */
import React from 'react';
import 'react-rrule-generator/build/styles.css';
import { Col, Row, Table } from 'reactstrap';
import { DynamicLayoutContext } from '../../../context';
import style from './Vacation.module.scss';

function VacationStatistics({ values }) {
    const { ui } = React.useContext(DynamicLayoutContext);

    const { statisticsCurrentYear, statisticsPreviousYear } = values;
    const current = statisticsCurrentYear;
    const prev = statisticsPreviousYear;

    return React.useMemo(
        () => (
            <React.Fragment>
                <Row>
                    <Col sm={12}>
                        <Table striped className={style.statistics}>
                            <thead>
                            <tr className={style.borderBottom}>
                                <th>{ui.translations['vacation.leaveaccount.title']}</th>
                                <th className={style.number}>{current.year}</th>
                                <th className={style.number}>{prev.year}</th>
                            </tr>
                            </thead>
                            <tbody>
                            <tr>
                                <td>{ui.translations['vacation.annualleave']}</td>
                                <td className={style.number}>{current.vacationDaysInYearFromContract}</td>
                                <td className={style.number}>{prev.vacationDaysInYearFromContract}</td>
                            </tr>
                            <tr>
                                <td>{ui.translations['vacation.previousyearleave']}</td>
                                <td className={style.number}>{current.remainingLeaveFromPreviousYear}</td>
                                <td className={style.number}>{prev.remainingLeaveFromPreviousYear}</td>
                            </tr>
                            <tr className={style.borderTop}>
                                <th scope="row">{ui.translations['vacation.subtotal']}</th>
                                <td className={style.mumberBold}>{current.totalLeaveIncludingCarry}</td>
                                <td className={style.mumberBold}>{prev.totalLeaveIncludingCarry}</td>
                            </tr>
                            <tr>
                                <td>{ui.translations['vacation.previousyearleaveunused']}</td>
                                <td className={style.number}>{current.remainingLeaveFromPreviousYearUnused}</td>
                                <td className={style.number}>{prev.remainingLeaveFromPreviousYearUnused}</td>
                            </tr>
                            <tr>
                                <td>{ui.translations['menu.vacation.leaveAccountEntry']}</td>
                                <td className={style.number}>{current.leaveAccountEntriesSum}</td>
                                <td className={style.number}>{prev.leaveAccountEntriesSum}</td>
                            </tr>
                            <tr>
                                <td>{ui.translations['vacation.vacationApproved']}</td>
                                <td className={style.number}>{current.vacationDaysApproved}</td>
                                <td className={style.number}>{prev.vacationDaysApproved}</td>
                            </tr>
                            {current.hasVacationDaysInProgress || prev.hasVacationDaysInProgress
                                ? (
                                    <tr>
                                        <td>{ui.translations['vacation.vacationInProgress']}</td>
                                        <td className={style.number}>{current.vacationDaysInProgress}</td>
                                        <td className={style.number}>{prev.vacationDaysInProgress}</td>
                                    </tr>
                                ) : undefined}
                            <tr className={style.borderTop}>
                                <th scope="row">{ui.translations['vacation.availablevacation']}</th>
                                <td className={style.mumberBold}>{current.vacationDaysLeftInYear}</td>
                                <td className={style.mumberBold}>{prev.vacationDaysLeftInYear}</td>
                            </tr>
                            {current.hasSpecialVacationDaysApproved || prev.hasSpecialVacationDaysApproved
                                ? (
                                    <tr>
                                        <td>{ui.translations['vacation.specialApproved']}</td>
                                        <td className={style.number}>{current.specialVacationDaysApproved}</td>
                                        <td className={style.number}>{prev.specialVacationDaysApproved}</td>
                                    </tr>
                                ) : undefined}
                            {current.hasSpecialVacationDaysInProgress || prev.hasSpecialVacationDaysInProgress
                                ? (
                                    <tr>
                                        <td>{ui.translations['vacation.specialInProgress']}</td>
                                        <td className={style.number}>{current.specialVacationDaysInProgress}</td>
                                        <td className={style.number}>{prev.specialVacationDaysInProgress}</td>
                                    </tr>
                                ) : undefined}
                            </tbody>
                        </Table>
                    </Col>
                </Row>
            </React.Fragment>
        ),
    );
}

VacationStatistics.propTypes = {};

VacationStatistics.defaultProps = {};

export default VacationStatistics;

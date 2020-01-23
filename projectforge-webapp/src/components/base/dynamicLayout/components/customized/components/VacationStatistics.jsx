/* eslint-disable react/jsx-indent,indent,max-len */
import React from 'react';
import 'react-rrule-generator/build/styles.css';
import { Col, Row, Table } from 'reactstrap';
import { DynamicLayoutContext } from '../../../context';
import style from './VacationStatistics.module.scss';

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
                        <Table striped>
                            <thead>
                            <tr className={style.borderBottom}>
                                <th>{ui.translations['vacation.leaveaccount.title']}</th>
                                <th>{current.year}</th>
                                <th>{prev.year}</th>
                            </tr>
                            </thead>
                            <tbody>
                            <tr>
                                <td>{ui.translations['vacation.annualleave']}</td>
                                <td className={style.number}>{current.vacationDaysInYearFromContract}</td>
                                <td className={style.number}>{prev.vacationDaysInYearFromContract}</td>
                            </tr>
                            <tr className={style.borderBottom}>
                                <td>{ui.translations['vacation.previousyearleave']}</td>
                                <td className={style.number}>{current.remainingLeaveFromPreviousYear}</td>
                                <td className={style.number}>{prev.remainingLeaveFromPreviousYear}</td>
                            </tr>
                            <tr>
                                <th scope="row">{ui.translations['vacation.subtotal']}</th>
                                <td className={style.mumberBold}>{current.totalLeaveIncludingCarry}</td>
                                <td className={style.mumberBold}>{prev.totalLeaveIncludingCarry}</td>
                            </tr>
                            <tr>
                                <td>{ui.translations['vacation.previousyearleaveunused']}</td>
                                <td className={style.number}>{current.remainingLeaveFromPreviousYearUnusedString}</td>
                                <td className={style.number}>{prev.remainingLeaveFromPreviousYearUnusedString}</td>
                            </tr>
                            <tr>
                                <td>{ui.translations['menu.vacation.leaveAccountEntry']}</td>
                                <td className={style.number}>{current.leaveAccountEntriesSum}</td>
                                <td className={style.number}>{prev.leaveAccountEntriesSum}</td>
                            </tr>
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

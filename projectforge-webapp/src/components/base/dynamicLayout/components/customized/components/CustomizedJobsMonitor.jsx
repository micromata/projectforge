import PropTypes from 'prop-types';
import React, { useContext, useEffect, useMemo, useRef } from 'react';
import { DynamicLayoutContext } from '../../../context';
import { fetchJsonGet } from '../../../../../../utilities/rest';
import DynamicProgress from '../../DynamicProgress';

function CustomizedJobsMonitor(props) {
    const { values } = props;
    const {
        variables, ui, callAction,
    } = useContext(DynamicLayoutContext);

    const {
        jobId, all, fetchUpdateInterval, cancelConfirmMessage, caller,
    } = values;

    const interval = useRef();
    const fetchUpdateIntervalRef = useRef(fetchUpdateInterval);
    const jobIdRef = useRef(jobId);
    const allRef = useRef(all);
    const cancelConfirmMessageRef = useRef(cancelConfirmMessage);
    const callerRef = useRef(caller);

    const jobs = Object.getByString(variables, 'jobs');

    useEffect(() => {
        jobIdRef.current = jobId;
        allRef.current = all;
        fetchUpdateIntervalRef.current = fetchUpdateInterval;
        cancelConfirmMessageRef.current = cancelConfirmMessage;
        callerRef.current = caller;
    }, [jobId, all, fetchUpdateInterval, cancelConfirmMessage, caller]);

    const fetchJobsList = () => {
        fetchJsonGet(
            'jobsMonitor/jobs',
            { jobId: jobIdRef.current, all: allRef.current, caller: callerRef.current },
            (json) => callAction({ responseAction: json }),
        );
    };

    const onCancelClick = (cancelJobId) => {
        fetchJsonGet(
            'jobsMonitor/cancel',
            { jobId: cancelJobId },
            (json) => callAction({ responseAction: json }),
        );
    };

    useEffect(() => {
        interval.current = setInterval(
            () => fetchJobsList(),
            fetchUpdateIntervalRef.current || 2000,
        );
        return () => {
            clearInterval(interval.current);
        };
    }, []);

    useEffect(() => {
        fetchUpdateIntervalRef.current = fetchUpdateInterval;
    }, [fetchUpdateInterval]);
    let noJobsText;
    if (!jobs || jobs.length === 0) {
        noJobsText = ui.translations['jobs.monitor.noJobsAvailable'] || 'No current jobs.';
    }
    return useMemo(
        () => (
            <div>
                {jobs && jobs.map((job) => (
                    <DynamicProgress
                        value={job.progressPercentage}
                        color={job.progressBarColor}
                        animated={job.animated}
                        title={job.progressTitle}
                        onCancelClick={onCancelClick}
                        cancelConfirmMessage={cancelConfirmMessageRef.current}
                        cancelId={job.cancelId}
                        info={job.info}
                        infoColor={job.infoColor}
                    />
                ))}
                {noJobsText}
            </div>
        ),
        [variables],
    );
}

CustomizedJobsMonitor.propTypes = {
    jobId: PropTypes.string,
    all: PropTypes.bool,
    fetchUpdateInterval: PropTypes.number,
};

export default CustomizedJobsMonitor;

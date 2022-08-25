import PropTypes from 'prop-types';
import React, { useContext, useEffect, useRef } from 'react';
import { Progress } from 'reactstrap';
import { colorPropType } from '../../../../utilities/propTypes';
import { DynamicLayoutContext } from '../context';
import { fetchJsonGet } from '../../../../utilities/rest';
import history from '../../../../utilities/history';

function DynamicProgress(props) {
    const {
        title,
        color,
        id,
        value,
        fetchUpdateUrl,
        fetchUpdateInterval,
    } = props;
    const { data, variables, callAction } = useContext(DynamicLayoutContext);
    const interval = useRef();
    const fetchUpdateUrlRef = useRef(fetchUpdateUrl);
    const fetchUpdateIntervalRef = useRef(fetchUpdateInterval);

    const info = Object.getByString(data, id) || Object.getByString(variables, id)
        || { value, title };

    const fetchUpdate = () => {
        console.log(fetchUpdateUrlRef.current);
        fetchJsonGet(
            fetchUpdateUrlRef.current,
            undefined,
            (json) => {
                console.log(json);
                return callAction({ responseAction: json });
            },
        );
    };

    const {
        value: useValue, title: useTitle, color: useColor, animated: useAnimated,
    } = info;

    useEffect(() => {
        if (fetchUpdateUrl) {
            interval.current = setInterval(
                () => fetchUpdate(),
                fetchUpdateIntervalRef.current || 2000,
            );
        }
        return () => {
            if (fetchUpdateUrl) {
                clearInterval(interval.current);
            }
        };
    }, []);

    return (
        <Progress
            className="job-progress"
            value={useValue}
            color={useColor}
            animated={useAnimated}
            bar
        >
            {useTitle}
        </Progress>
    );
}

DynamicProgress.propTypes = {
    title: PropTypes.string,
    color: colorPropType,
    value: PropTypes.number,
    id: PropTypes.string,
    fetchUpdateUrl: PropTypes.string,
    fetchUpdateInterval: PropTypes.number,
};

DynamicProgress.defaultProps = {
    title: undefined,
    id: undefined,
    color: undefined,
    value: 0,
    fetchUpdateUrl: undefined,
    fetchUpdateInterval: undefined,
};

export default DynamicProgress;

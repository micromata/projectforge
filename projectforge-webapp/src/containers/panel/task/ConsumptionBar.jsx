import React from 'react';
import PropTypes from 'prop-types';
import { UncontrolledTooltip } from 'reactstrap';
import style from './ConsumptionBar.module.scss';

function CustomizedBar({ progress }) {
    if (!progress) {
        return <React.Fragment />;
    }
    const { title, status, width, id } = progress;
    return (
        // ToDo: onClick
        <a href="http://www.micromata.de" id={`cb-${id}`}>
            <div className={`${style.progress} ${style[status]}`}>
                <div style={{ width }}>
                    {' '}
                </div>
            </div>
            <UncontrolledTooltip placement="right" target={`cb-${id}`}>
                {title}
            </UncontrolledTooltip>
        </a>
    );
}

CustomizedBar.propTypes = {
    progress: PropTypes.shape({
        title: PropTypes.string,
        status: PropTypes.string,
        width: PropTypes.string,
        id: PropTypes.number,
    }),
};

CustomizedBar.defaultProps = {
    progress: undefined,
};

export default CustomizedBar;

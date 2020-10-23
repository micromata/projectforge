import { faStar as faStarRegular } from '@fortawesome/free-regular-svg-icons';
import { faStar as faStarSolid, faTimes } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React, { useState } from 'react';
import InputContainer from '../InputContainer';
import styles from './RatingStars.module.scss';

function RatingStars(
    {
        onChange,
        value,
        values,
        ...props
    },
) {
    const [hoverIdx, setHoverIdx] = useState(-1);

    const handleClick = idx => () => onChange(idx);
    const handleMouseEnter = idx => () => setHoverIdx(idx);
    const handleMouseLeave = () => setHoverIdx(-1);

    return (
        <InputContainer
            {...props}
            additionalLabel={values[hoverIdx] || values[value] || '-'}
            isActive
            withMargin
        >
            {values.map((starLabel, idx) => {
                let icon;
                let color = 'inherit';

                if (idx === 0) {
                    icon = faTimes;

                    if (hoverIdx === 0) {
                        color = '#009BA3';
                    } else if (value === 0) {
                        color = '#dc3545';
                    }
                } else {
                    if (idx <= value) {
                        icon = faStarSolid;
                        color = '#ffc107';
                    } else {
                        icon = faStarRegular;
                    }

                    if (idx <= hoverIdx) {
                        color = '#009BA3';
                    }
                }

                return (
                    <button
                        key={starLabel}
                        className={styles.option}
                        type="button"
                        onClick={handleClick(idx)}
                        onMouseEnter={handleMouseEnter(idx)}
                        onMouseLeave={handleMouseLeave}
                    >
                        <FontAwesomeIcon
                            icon={icon}
                            color={color}
                        />
                    </button>
                );
            })}
        </InputContainer>
    );
}

RatingStars.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    values: PropTypes.arrayOf(PropTypes.string).isRequired,
    value: PropTypes.number,
};

RatingStars.defaultProps = {
    value: -1,
};

export default RatingStars;

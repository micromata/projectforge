import PropTypes from 'prop-types';
import React from 'react';
import RatingStars from '../../../../design/input/ratingStars/RatingStars';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from './DynamicValidationManager';

function DynamicRating({ id, label, values }) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    const value = Object.getByString(data, id);

    return React.useMemo(() => {
        const handleChange = rating => setData({ [id]: rating });

        return (
            <DynamicValidationManager id={id}>
                <RatingStars
                    id={`${ui.uid}-${id}`}
                    label={label}
                    onChange={handleChange}
                    values={values}
                    value={value}
                />
            </DynamicValidationManager>
        );
    }, [value, setData, id, label, ui.uid, values]);
}

DynamicRating.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
};

DynamicRating.defaultProps = {};

export default DynamicRating;

import PropTypes from 'prop-types';
import React from 'react';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from './DynamicValidationManager';

function DynamicRating({
    id, label, values, ...props
}) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    const value = Object.getByString(data, id) || false;

    return React.useMemo(() => {
        //const handleValueChange = ({ value }) => setData({ [id]: target.checked });

        const ratingCompleted = ({ rating} ) => console.log("Rating is: " + rating);

        return (
            <DynamicValidationManager id={id}>
                Where the magic happens...
            </DynamicValidationManager>
        );
    }, [value, setData, id, label, props]);
}

DynamicRating.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
};

DynamicRating.defaultProps = {};

export default DynamicRating;

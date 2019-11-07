import PropTypes from 'prop-types';
import React from 'react';
import DayPickerInput from 'react-day-picker/DayPickerInput';
import AdditionalLabel from './AdditionalLabel';

// Automatically validate the input fields nested inside. (Only first level)
function ValidationManager({ children, customValidation }) {
    let extraLabel = <React.Fragment />;

    return (
        <React.Fragment>
            {React.Children.map(children, (child) => {
                if (!child) {
                    return child;
                }
                const { props: childProps, type } = child;
                const {
                    checked,
                    required,
                    maxLength,
                    value,
                } = childProps;

                let valid = true;
                let { additionalLabel } = childProps;

                // Validate required and maxLength
                if (
                    (required && !(value || checked))
                    || (maxLength && value && value.length > maxLength)
                ) {
                    valid = false;
                }

                // Check for a custom validation.
                if (customValidation) {
                    valid = false;

                    if (additionalLabel) {
                        // Add the validation message to the additional label if existing
                        additionalLabel += ` - ${customValidation.message}`;
                    } else {
                        // or else set the additional label to the validation message.
                        additionalLabel = customValidation.message;
                    }

                    if (type === DayPickerInput) {
                        extraLabel = <AdditionalLabel title={additionalLabel} />;
                    }
                }

                return {
                    ...child,
                    // Manupulating the props of the child.
                    props: {
                        ...child.props,
                        additionalLabel,
                        color: valid ? childProps.color : 'danger',
                    },
                };
            })}
            {extraLabel}
        </React.Fragment>
    );
}

ValidationManager.propTypes = {
    children: PropTypes.node.isRequired,
    customValidation: PropTypes.shape({
        message: PropTypes.string,
    }),
};

ValidationManager.defaultProps = {
    customValidation: undefined,
};

export default ValidationManager;

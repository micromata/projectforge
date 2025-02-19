import PropTypes from 'prop-types';
import React from 'react';

// Automatically validate the input fields nested inside. (Only first level)
function ValidationManager({ children, customValidation }) {
    const extraLabel = null;

    return (
        <>
            {React.Children.map(children, (child) => {
                if (!child) {
                    return child;
                }
                const { props: childProps } = child;
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
                }

                return {
                    ...child,
                    // Manipulating the props of the child.
                    props: {
                        ...child.props,
                        additionalLabel,
                        color: valid ? childProps.color : 'danger',
                    },
                };
            })}
            {extraLabel}
        </>
    );
}

ValidationManager.propTypes = {
    children: PropTypes.node.isRequired,
    customValidation: PropTypes.shape({
        message: PropTypes.string,
    }),
};

export default ValidationManager;

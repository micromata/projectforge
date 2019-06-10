const getBackgroundColor = (data) => {
    return data.style ? data.style.bgColor || '#eee' : '#eee';
};

const getForegroundColor = (data) => {
    return data.style ? data.style.fgColor || '#222' : '#222';
};

export const customStyles = {
    control: styles => ({
        ...styles,
    }),
    container: base => ({
        ...base,
        zIndex: '999',
    }),
    // Option in drop-down menu:
    option: (styles, {
        data,
        isFocused,
    }) => { // The options displayed in the opened menu:
        let opacity = null;
        if (isFocused) {
            opacity = '0.5';
        }
        return {
            ...styles,
            backgroundColor: getBackgroundColor(data),
            color: getForegroundColor(data),
            opacity,
        };
    },
    // Selected value in input field:
    multiValue: (styles, { data }) => {
        return {
            ...styles,
            backgroundColor: getBackgroundColor(data),
            opacity: (!data.visible) ? '0.5' : undefined,
        };
    },
    // Selected value in input field (label):
    multiValueLabel: (styles, { data }) => ({
        ...styles,
        color: getForegroundColor(data),
        textDecoration: (!data.visible) ? 'line-through' : undefined,
        fontStyle: (!data.visible) ? 'italic' : undefined,
    }),
    /* multiValueRemove: (styles, { data }) => ({
        ...styles,
        color: getForegroundColor(data), // Doesn't work :-(
        ':hover': {
            opacity: '0.5',
        },
    }), */
};

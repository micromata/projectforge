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
    option: (styles, {
        data,
        isFocused,
    }) => { // The options displayed in the opened menu:
        let opacity = null;
        if (isFocused) {
            opacity = '0.5';
        }
        const backgroundColor = getBackgroundColor(data);
        return {
            ...styles,
            backgroundColor,
            opacity,
            color: getForegroundColor(data),
        };
    },
    multiValue: (styles, { data }) => {
        return {
            ...styles,
            backgroundColor: getBackgroundColor(data),
        };
    },
    multiValueLabel: (styles, { data }) => ({
        ...styles,
        color: getForegroundColor(data),
    }),
    multiValueRemove: (styles, { data }) => ({
        ...styles,
        color: getForegroundColor(data),
        ':hover': {
            opacity: '0.5',
        },
    }),
};

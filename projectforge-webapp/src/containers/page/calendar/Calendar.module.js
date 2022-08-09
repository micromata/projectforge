const customStyles = {
    control: (styles) => ({
        ...styles,
    }),
    container: (base) => ({
        ...base,
        zIndex: '999',
    }),
    // Option in drop-down menu:
    option: (styles, {
        data,
        isFocused,
        // The options displayed in the opened menu:
    }) => ({
        ...styles,
        backgroundColor: isFocused ? '#ddd' : undefined,
        color: isFocused ? '#000' : undefined,
        display: 'flex',
        ':before': {
            backgroundColor: data?.style?.bgColor || '#eee',
            borderRadius: 10,
            borderStyle: 'solid',
            borderWidth: '1px',
            content: '" "',
            display: 'block',
            marginRight: 8,
            height: 15,
            width: 15,
        },
    }),
    // Selected value in input field:
    multiValue: (styles, { data }) => ({
        ...styles,
        opacity: (!data.visible) ? '0.5' : undefined,
    }),
    // Selected value in input field (label):
    multiValueLabel: (styles, { data }) => ({
        ...styles,
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

export default customStyles;

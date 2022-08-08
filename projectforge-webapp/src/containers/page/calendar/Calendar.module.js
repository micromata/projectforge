const getBackgroundColor = ({ style }) => ((style && style.bgColor) ? style.bgColor : '#eee');
const getFocusedBackgroundColor = ({ style }) => ((style && style.bgColor) ? style.bgColor : '#ddd');

const getForegroundColor = ({ style }) => ((style && style.fgColor) ? style.fgColor : '#222');

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
        backgroundColor: isFocused ? getFocusedBackgroundColor(data) : getBackgroundColor(data),
        color: getForegroundColor(data),
    }),
    // Selected value in input field:
    multiValue: (styles, { data }) => ({
        ...styles,
        backgroundColor: getBackgroundColor(data),
        opacity: (!data.visible) ? '0.5' : undefined,
    }),
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

export default customStyles;

import { black } from '../../utilities/colorAcrobatics';

const getBackgroundColor = (data) => {
    return data.style ? data.style.bgColor || '#eee' : '#eee';
};

export const customStyles = {
    control: styles => ({
        ...styles,
        backgroundColor: 'white',
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
            color: black(backgroundColor) ? 'black' : 'white',
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
        color: black(getBackgroundColor(data)) ? 'black' : 'white',
    }),
    multiValueRemove: (styles, { data }) => ({
        ...styles,
        color: black(getBackgroundColor(data)) ? 'black' : 'white',
        ':hover': {
            opacity: '0.5',
        },
    }),
};

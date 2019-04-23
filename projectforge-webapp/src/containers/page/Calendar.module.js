import { black } from '../../utilities/colorAcrobatics';

const getBackgroundColor = (data) => {
    return data.style ? data.style.bgColor || '#fff' : '#fff';
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
            color: black(backgroundColor) ? 'white' : 'black',
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
        color: black(getBackgroundColor(data)) ? 'white' : 'black',
    }),
    multiValueRemove: (styles, { data }) => ({
        ...styles,
        color: black(getBackgroundColor(data)) ? 'white' : 'black',
        ':hover': {
            opacity: '0.5',
        },
    }),
};

// https://stackoverflow.com/a/6491621
Object.getByString = (object, multiKey) => {
    if (!multiKey) {
        return undefined;
    }

    let obj = object;

    multiKey
    // convert indexes to properties
        .replace(/\[(\w+)\]/g, '.$1')
        // remove leading dots
        .replace(/^\./, '')
        // split at dots
        .split('.')
        .forEach((key) => {
            if (obj) obj = obj[key];
        });

    return obj;
};

Object.isEmpty = object => Object.keys(object).length === 0;

Object.isObject = object => typeof object === 'object';

// Combines two objects one level down.
Object.combine = (o1, o2) => ({
    ...o1,
    ...o2,
    ...Object.keys(o1)
        .map(key => ({
            key,
            v1: o1[key],
            v2: o2[key],
        }))
        .filter(({ v1, v2 }) => typeof v1 === 'object' && typeof v2 === 'object')
        .reduce((previousValue, currentValue) => ({
            ...previousValue,
            [currentValue.key]: {
                ...currentValue.v1,
                ...currentValue.v2,
            },
        }), {}),
});

Array.findByField = (array, field, value) => array.reduce((accumulator, currentValue) => {
    if (currentValue[field] === value) {
        return currentValue;
    }

    return accumulator;
}, undefined);

// Replace all selector characters to prevent that they appear in an id.
String.idify = string => string.replace(/[.#*, >+~[\]=|^$:()]/g, '-');

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

Array.findByField = (array, field, value) => array.reduce((accumulator, currentValue) => {
    if (currentValue[field] === value) {
        return currentValue;
    }

    return accumulator;
}, undefined);

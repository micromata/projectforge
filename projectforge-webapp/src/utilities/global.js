import { forIn, get, mergeWith, set } from 'lodash/object';
import { isArray } from 'lodash/lang';

// https://stackoverflow.com/a/6491621
Object.getByString = (object, multiKey) => {
    if (!multiKey) {
        return undefined;
    }
    if (!(typeof multiKey === 'string' || multiKey instanceof String)) {
        /* eslint-disable-next-line no-console */
        console.log("Warning: multiKey isn't of type String.", multiKey, typeof multiKey);
        return undefined;
    }
    return get(object, multiKey);
};

Object.isEmpty = (object) => Object.keys(object).length === 0;

const customizer = (objValue, srcValue) => {
    if (isArray(objValue)) {
        // Don't merge arrays: replace existing array by src array.
        return srcValue;
    }
    return undefined;
};

// Combines two objects. Paths like user.rights[2].value are also supported in o2 fields.
Object.combine = (o1, o2) => {
    const newValues = {};
    const specialProperties = {};
    forIn(o2, (value, key) => {
        if (key.includes('.') || key.match(/\[(\d)\]/) || value === undefined) {
            // keys like user.name or user.rights[2].value or undefined values
            // need special treatment after merge.
            specialProperties[key] = value;
        } else {
            newValues[key] = value;
        }
    });
    const newState = mergeWith(o1, newValues, customizer);
    forIn(specialProperties, (value, key) => {
        // Sets value of new state by deep property path (like user.name or user.rights[2].value)
        // undefined values will also be set.
        set(newState, key, value);
    });
    return newState;
};

Array.findByField = (array, field, value) => array.reduce((accumulator, currentValue) => {
    if (currentValue[field] === value) {
        return currentValue;
    }

    return accumulator;
}, undefined);

// Replace all selector characters to prevent that they appear in an id.
String.idify = (string) => string.replace(/[.#*, >+~/[\]=|^$:()]/g, '-');

String.truncate = (str, length) => (str?.length > length ? str.substring(0, length) : str);

Object.getResponseHeaderFilename = (contentDisposition) => {
    // attachment; filename*=UTF-8''document.pdf; filename=document.pdf
    const matches = /filename[^;=\n]*=(UTF-8(['"]*))?([^;=\n]*)*/.exec(contentDisposition);
    return matches && matches.length >= 3 && matches[3] ? decodeURI(matches[3].replace(/['"]/g, '')) : 'download';
};

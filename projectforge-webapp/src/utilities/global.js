// https://stackoverflow.com/a/6491621
Object.getByString = (object, multiKey) => {
    let obj = object;

    multiKey
    // convert indexes to properties
        .replace(/\[(\w+)\]/g, '.$1')
        // remove leading dots
        .replace(/^\./, '')
        // split at dots
        .split('.')
        .forEach((key) => {
            if (obj)
              obj = obj[key];
        });

    return obj;
};

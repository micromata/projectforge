export default () => Math.random()
    .toString(36)
    .replace(/[^a-z0-9]+/g, '')
    .substr(1, 10);

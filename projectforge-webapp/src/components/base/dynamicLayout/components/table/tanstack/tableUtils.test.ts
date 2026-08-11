import { describe, it, expect } from 'vitest';
import { toSortPrimitive, createValueComparator, buildColumnDefs } from './tableUtils';

const collator = new Intl.Collator('de-DE', { numeric: true });
const compare = createValueComparator(collator);

describe('toSortPrimitive', () => {
    it('returns null for null, undefined and empty string', () => {
        expect(toSortPrimitive(null)).toBeNull();
        expect(toSortPrimitive(undefined)).toBeNull();
        expect(toSortPrimitive('')).toBeNull();
        expect(toSortPrimitive('   ')).toBeNull();
    });

    it('passes numbers through', () => {
        expect(toSortPrimitive(42)).toBe(42);
        expect(toSortPrimitive(0)).toBe(0);
        expect(toSortPrimitive(NaN)).toBeNull();
    });

    it('parses ISO dates to millis', () => {
        const ts = toSortPrimitive('2026-01-15T09:00:00.000Z') as number;
        expect(typeof ts).toBe('number');
        expect(ts).toBeGreaterThan(0);
        // January < December
        const jan = toSortPrimitive('2026-01-15T09:00:00.000Z') as number;
        const dec = toSortPrimitive('2026-12-01T09:00:00.000Z') as number;
        expect(jan).toBeLessThan(dec);
    });

    it('parses plain ISO dates', () => {
        const d = toSortPrimitive('2026-08-11') as number;
        expect(typeof d).toBe('number');
    });

    it('treats German locale-formatted strings as plain strings (not dates)', () => {
        // "11.08.2026 09:00-11:30" does not match ISO_DATE_RE
        const v = toSortPrimitive('11.08.2026 09:00-11:30');
        expect(typeof v).toBe('string');
        // Lexicographic order differs from chronological for dd.MM.yyyy format
        const aug = toSortPrimitive('11.08.2026 09:00-11:30') as string;
        const dec = toSortPrimitive('01.12.2026 08:00-09:00') as string;
        expect(aug > dec).toBe(true); // '11.' > '01.' lexicographically — the bug
    });

    it('extracts displayName from objects', () => {
        expect(toSortPrimitive({ displayName: 'Alice' })).toBe('Alice');
        expect(toSortPrimitive({ displayName: '' })).toBeNull();
        expect(toSortPrimitive({ other: 'x' })).toBeNull(); // no displayName → treated as blank
    });

    it('joins displayName from arrays', () => {
        const v = toSortPrimitive([{ displayName: 'A' }, { displayName: 'B' }]);
        expect(v).toBe('A, B');
        expect(toSortPrimitive([])).toBeNull();
    });
});

describe('createValueComparator', () => {
    it('sorts ISO timestamps chronologically', () => {
        const jan = '2026-01-15T09:00:00.000Z';
        const dec = '2026-12-01T09:00:00.000Z';
        expect(compare(jan, dec)).toBeLessThan(0);
        expect(compare(dec, jan)).toBeGreaterThan(0);
        expect(compare(jan, jan)).toBe(0);
    });

    it('places blanks last in ascending order', () => {
        expect(compare(null, 'a')).toBeGreaterThan(0);
        expect(compare('a', null)).toBeLessThan(0);
        expect(compare(null, null)).toBe(0);
        expect(compare('', 'a')).toBeGreaterThan(0);
    });

    it('sorts numbers correctly', () => {
        expect(compare(1, 2)).toBeLessThan(0);
        expect(compare(10, 2)).toBeGreaterThan(0);
    });

    it('sorts strings with numeric collation (A2 < A10)', () => {
        expect(compare('A2', 'A10')).toBeLessThan(0);
    });

    it('sorts German umlauts correctly (Ä near A, before B)', () => {
        expect(compare('Ä', 'B')).toBeLessThan(0);
        expect(compare('Ö', 'P')).toBeLessThan(0);
    });
});

describe('buildColumnDefs with sortField', () => {
    const fakeCompare = createValueComparator(new Intl.Collator('de-DE', { numeric: true }));

    const colWithSortField = {
        field: 'timePeriod',
        headerName: 'Zeitraum',
        sortField: 'timesheet.startTime',
    };

    const col = buildColumnDefs([colWithSortField], fakeCompare)[0] as any;

    it('accessorFn returns the display value (timePeriod)', () => {
        const row = {
            timePeriod: '11.08.2026 09:00-11:30',
            timesheet: { startTime: '2026-08-11T09:00:00.000Z' },
        };
        expect(col.accessorFn(row)).toBe('11.08.2026 09:00-11:30');
    });

    it('sortingFn orders rows chronologically via timesheet.startTime', () => {
        const rowAug = {
            original: { timePeriod: '11.08.2026 09:00-11:30', timesheet: { startTime: '2026-08-11T09:00:00.000Z' } },
            getValue: () => '11.08.2026 09:00-11:30',
        };
        const rowDec = {
            original: { timePeriod: '01.12.2026 08:00-09:00', timesheet: { startTime: '2026-12-01T08:00:00.000Z' } },
            getValue: () => '01.12.2026 08:00-09:00',
        };
        // Chronologically Aug < Dec → sortingFn should return < 0
        expect(col.sortingFn(rowAug, rowDec, 'timePeriod')).toBeLessThan(0);
        expect(col.sortingFn(rowDec, rowAug, 'timePeriod')).toBeGreaterThan(0);
    });

    it('sortField is stored in meta', () => {
        expect((col.meta as any).sortField).toBe('timesheet.startTime');
    });
});

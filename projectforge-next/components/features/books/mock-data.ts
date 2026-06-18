import type { BookDetail, HistoryEntry, UserRef } from "./types";

const USERS: UserRef[] = [
  { id: 11, displayName: "Fink, Laura" },
  { id: 12, displayName: "Weber, Max" },
  { id: 13, displayName: "Frank, Anna" },
  { id: 14, displayName: "Müller, Hans" },
  { id: 15, displayName: "Richter, Klaus" },
  { id: 16, displayName: "Braun, Eva" },
  { id: 17, displayName: "Schmidt, Karl" },
  { id: 18, displayName: "Becker, Maria" },
  { id: 19, displayName: "Hoffmann, Jens" },
  { id: 20, displayName: "Wagner, Petra" },
];

interface Seed {
  id: number;
  title: string;
  authors: string;
  signature: string;
  yearOfPublishing: string;
  publisher: string;
  editor: string | null;
  isbn: string;
  keywords: string;
  abstractText: string;
  created: string;
  lendOutByIdx: number | null;
  lendOutDate: string | null;
}

const SEEDS: Seed[] = [
  {
    id: 1,
    title: "IR and Raman Spectroscopy",
    authors: "Larkin, Peter J.",
    signature: "NAT-5",
    yearOfPublishing: "2011",
    publisher: "Wiley-Interscience",
    editor: "2. Auflage",
    isbn: "978-0-470-01872-3",
    keywords: "chemistry, spectroscopy, IR, Raman",
    abstractText:
      "Umfassendes Referenzwerk zur Infrarot- und Raman-Spektroskopie mit praktischen Anwendungsbeispielen für Chemiker und Materialwissenschaftler.",
    created: "27.02.2012",
    lendOutByIdx: 0,
    lendOutDate: "2025-04-12",
  },
  {
    id: 2,
    title: "Grundlagen der Biologie",
    authors: "Müller, Hans",
    signature: "BIO-12",
    yearOfPublishing: "2019",
    publisher: "Springer",
    editor: "1. Auflage",
    isbn: "978-3-642-12345-6",
    keywords: "biology",
    abstractText: "Standardwerk der Biologie für Studierende.",
    created: "15.03.2015",
    lendOutByIdx: null,
    lendOutDate: null,
  },
  {
    id: 3,
    title: "Relativitätstheorie — Eine populäre Darstellung",
    authors: "Einstein, Albert",
    signature: "MAT-3",
    yearOfPublishing: "2008",
    publisher: "Vieweg",
    editor: "Sonderausgabe",
    isbn: "978-3-528-08412-3",
    keywords: "physics, relativity",
    abstractText: "Klassiker der Physik in allgemein verständlicher Form.",
    created: "08.11.2010",
    lendOutByIdx: 1,
    lendOutDate: "2024-09-12",
  },
  {
    id: 4,
    title: "Linux Kernel Programming",
    authors: "Torvalds, Linus",
    signature: "IT-88",
    yearOfPublishing: "2021",
    publisher: "Packt",
    editor: "1. Auflage",
    isbn: "978-1-78953-242-8",
    keywords: "software, linux",
    abstractText: "Einführung in die Kernel-Entwicklung.",
    created: "22.07.2020",
    lendOutByIdx: null,
    lendOutDate: null,
  },
  {
    id: 5,
    title: "Radioaktivität und ihre Messung",
    authors: "Curie, Marie",
    signature: "CHE-6",
    yearOfPublishing: "2015",
    publisher: "Hirzel",
    editor: null,
    isbn: "978-3-7776-2244-1",
    keywords: "chemistry, nuclear",
    abstractText: "Historisches Werk zur Radioaktivitätsforschung.",
    created: "03.01.2018",
    lendOutByIdx: 2,
    lendOutDate: "2025-02-01",
  },
  {
    id: 6,
    title: "QED: Die seltsame Theorie des Lichts",
    authors: "Feynman, Richard",
    signature: "PHY-9",
    yearOfPublishing: "2022",
    publisher: "Piper",
    editor: "Neuauflage",
    isbn: "978-3-492-31234-5",
    keywords: "physics, quantum",
    abstractText: "Allgemein verständliche Einführung in die Quantenelektrodynamik.",
    created: "11.09.2022",
    lendOutByIdx: null,
    lendOutDate: null,
  },
  {
    id: 7,
    title: "On the Origin of Species",
    authors: "Darwin, Charles",
    signature: "GEO-2",
    yearOfPublishing: "2014",
    publisher: "Reclam",
    editor: null,
    isbn: "978-3-15-019912-3",
    keywords: "biology, evolution",
    abstractText: "Grundlagenwerk der Evolutionstheorie.",
    created: "05.05.2016",
    lendOutByIdx: 3,
    lendOutDate: "2025-01-15",
  },
  {
    id: 8,
    title: "A Brief History of Time",
    authors: "Hawking, Stephen",
    signature: "MAT-7",
    yearOfPublishing: "2018",
    publisher: "Bantam",
    editor: "Updated",
    isbn: "978-0-553-38016-3",
    keywords: "physics, cosmology",
    abstractText: "Populärwissenschaftliches Werk über Kosmologie.",
    created: "19.02.2019",
    lendOutByIdx: null,
    lendOutDate: null,
  },
  {
    id: 9,
    title: "The Art of Computer Programming, Vol. 1",
    authors: "Knuth, Donald",
    signature: "IT-92",
    yearOfPublishing: "2020",
    publisher: "Addison-Wesley",
    editor: "3rd Edition",
    isbn: "978-0-201-89683-1",
    keywords: "software, algorithms",
    abstractText: "Klassiker der Informatik.",
    created: "30.06.2021",
    lendOutByIdx: 4,
    lendOutDate: "2024-12-10",
  },
  {
    id: 10,
    title: "Die Doppelhelix",
    authors: "Watson, James",
    signature: "BIO-8",
    yearOfPublishing: "2016",
    publisher: "Rowohlt",
    editor: null,
    isbn: "978-3-499-12345-8",
    keywords: "biology, DNA",
    abstractText: "Persönlicher Bericht zur Entdeckung der DNA-Struktur.",
    created: "14.11.2017",
    lendOutByIdx: null,
    lendOutDate: null,
  },
  {
    id: 11,
    title: "General Chemistry",
    authors: "Pauling, Linus",
    signature: "CHE-11",
    yearOfPublishing: "2019",
    publisher: "Dover",
    editor: "Reprint",
    isbn: "978-0-486-65622-9",
    keywords: "chemistry, general",
    abstractText: "Allgemeines Lehrbuch der Chemie.",
    created: "28.03.2020",
    lendOutByIdx: 5,
    lendOutDate: "2024-11-20",
  },
  {
    id: 12,
    title: "Computing Machinery and Intelligence",
    authors: "Turing, Alan",
    signature: "IT-105",
    yearOfPublishing: "2023",
    publisher: "MIT Press",
    editor: null,
    isbn: "978-0-262-04567-8",
    keywords: "AI, computing",
    abstractText: "Sammelband zur frühen KI-Forschung.",
    created: "07.12.2023",
    lendOutByIdx: null,
    lendOutDate: null,
  },
];

function seedToBook(seed: Seed): BookDetail {
  const lendOutBy = seed.lendOutByIdx == null ? null : USERS[seed.lendOutByIdx];
  return {
    id: seed.id,
    title: seed.title,
    authors: seed.authors,
    signature: seed.signature,
    yearOfPublishing: seed.yearOfPublishing,
    publisher: seed.publisher,
    editor: seed.editor,
    isbn: seed.isbn,
    keywords: seed.keywords,
    abstractText: seed.abstractText,
    comment: null,
    status: "PRESENT",
    type: "BOOK",
    lendOutBy,
    lendOutDate: seed.lendOutDate,
    lendOutComment: null,
    created: seed.created,
  };
}

function pad2(n: number): string {
  return n.toString().padStart(2, "0");
}

function generated(): BookDetail[] {
  const out: BookDetail[] = [];
  for (let i = SEEDS.length + 1; i <= 234; i++) {
    const seed = SEEDS[i % SEEDS.length];
    const day = ((i * 7) % 28) + 1;
    const month = ((i * 3) % 12) + 1;
    const year = 2008 + ((i * 5) % 16);
    const lent = i % 3 !== 0;
    const borrower = lent ? USERS[i % USERS.length] : null;
    out.push({
      id: i,
      title: `${seed.title} (Band ${1 + (i % 9)})`,
      authors: seed.authors,
      signature: `${seed.signature.split("-")[0]}-${100 + i}`,
      yearOfPublishing: String(2005 + ((i * 11) % 19)),
      publisher: seed.publisher,
      editor: seed.editor,
      isbn: seed.isbn,
      keywords: seed.keywords,
      abstractText: seed.abstractText,
      comment: null,
      status: "PRESENT",
      type: "BOOK",
      lendOutBy: borrower,
      lendOutDate: lent ? `2025-${pad2(month)}-${pad2(day)}` : null,
      lendOutComment: null,
      created: `${pad2(day)}.${pad2(month)}.${year}`,
    });
  }
  return out;
}

// Mutable in-memory store. Mock route handlers mutate this on PUT.
export const BOOKS: BookDetail[] = [...SEEDS.map(seedToBook), ...generated()];

export function findBook(id: number): BookDetail | undefined {
  return BOOKS.find((b) => b.id === id);
}

export function updateBook(id: number, patch: Partial<BookDetail>): BookDetail | undefined {
  const idx = BOOKS.findIndex((b) => b.id === id);
  if (idx < 0) return undefined;
  BOOKS[idx] = { ...BOOKS[idx], ...patch, id: BOOKS[idx].id };
  return BOOKS[idx];
}

// Mock change-log mirroring DisplayHistoryEntry. Keyed by book id.
export const BOOK_HISTORY: Record<number, HistoryEntry[]> = {
  1: [
    {
      id: 5001,
      modifiedAt: "2025-04-12T09:14:00Z",
      timeAgo: "vor 1 Monat",
      modifiedByUserId: 11,
      modifiedByUser: "Fink, Laura",
      operationType: "Update",
      operation: "Ausgeliehen",
      userComment: null,
      attributes: [
        {
          propertyName: "lendOutBy",
          displayPropertyName: "Ausgeliehen von",
          operationType: "Update",
          oldValue: null,
          newValue: "Fink, Laura",
        },
        {
          propertyName: "lendOutDate",
          displayPropertyName: "Ausgeliehen seit",
          operationType: "Update",
          oldValue: null,
          newValue: "2025-04-12",
        },
      ],
    },
    {
      id: 5002,
      modifiedAt: "2024-03-03T14:32:00Z",
      timeAgo: "vor 2 Jahren",
      modifiedByUserId: 12,
      modifiedByUser: "Weber, Max",
      operationType: "Update",
      operation: "Zurückgegeben",
      userComment: null,
      attributes: [
        {
          propertyName: "lendOutBy",
          displayPropertyName: "Ausgeliehen von",
          operationType: "Update",
          oldValue: "Weber, Max",
          newValue: null,
        },
      ],
    },
    {
      id: 5003,
      modifiedAt: "2024-01-18T10:05:00Z",
      timeAgo: "vor 2 Jahren",
      modifiedByUserId: 99,
      modifiedByUser: "Fin Reinhard",
      operationType: "Update",
      operation: "Datensatz bearbeitet",
      userComment: null,
      attributes: [
        {
          propertyName: "keywords",
          displayPropertyName: "Schlüsselworte",
          operationType: "Update",
          oldValue: "chemistry, spectroscopy",
          newValue: "chemistry, spectroscopy, IR, Raman",
        },
        {
          propertyName: "editor",
          displayPropertyName: "Auflage",
          operationType: "Update",
          oldValue: "1. Auflage",
          newValue: "2. Auflage",
        },
        {
          propertyName: "abstractText",
          displayPropertyName: "Beschreibung",
          operationType: "Update",
          oldValue: null,
          newValue:
            "Umfassendes Referenzwerk zur Infrarot- und Raman-Spektroskopie …",
        },
      ],
    },
    {
      id: 5004,
      modifiedAt: "2023-09-12T11:00:00Z",
      timeAgo: "vor 3 Jahren",
      modifiedByUserId: 12,
      modifiedByUser: "Weber, Max",
      operationType: "Update",
      operation: "Ausgeliehen",
      userComment: null,
      attributes: [],
    },
    {
      id: 5005,
      modifiedAt: "2012-02-27T08:00:00Z",
      timeAgo: "vor 14 Jahren",
      modifiedByUserId: null,
      modifiedByUser: "System",
      operationType: "Insert",
      operation: "Datensatz angelegt",
      userComment: null,
      attributes: [],
    },
  ],
};

export function getBookHistory(id: number): HistoryEntry[] {
  return BOOK_HISTORY[id] ?? [];
}

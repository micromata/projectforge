import type { Book } from "./types";

const SEED: Book[] = [
  {
    id: 1,
    angelegt: "27.02.2012",
    jahr: "2011",
    sig: "NAT-5",
    autor: "Larkin, Peter J.",
    titel: "IR and Raman Spectroscopy",
    key: "chemistry, spectroscopy",
    ausgelBy: "Fink, Laura",
    avail: false,
  },
  {
    id: 2,
    angelegt: "15.03.2015",
    jahr: "2019",
    sig: "BIO-12",
    autor: "Müller, Hans",
    titel: "Grundlagen der Biologie",
    key: "biology",
    ausgelBy: "—",
    avail: true,
  },
  {
    id: 3,
    angelegt: "08.11.2010",
    jahr: "2008",
    sig: "MAT-3",
    autor: "Einstein, Albert",
    titel: "Relativitätstheorie — Eine populäre Darstellung",
    key: "physics, relativity",
    ausgelBy: "Weber, Max",
    avail: false,
  },
  {
    id: 4,
    angelegt: "22.07.2020",
    jahr: "2021",
    sig: "IT-88",
    autor: "Torvalds, Linus",
    titel: "Linux Kernel Programming",
    key: "software, linux",
    ausgelBy: "—",
    avail: true,
  },
  {
    id: 5,
    angelegt: "03.01.2018",
    jahr: "2015",
    sig: "CHE-6",
    autor: "Curie, Marie",
    titel: "Radioaktivität und ihre Messung",
    key: "chemistry, nuclear",
    ausgelBy: "Frank, Anna",
    avail: false,
  },
  {
    id: 6,
    angelegt: "11.09.2022",
    jahr: "2022",
    sig: "PHY-9",
    autor: "Feynman, Richard",
    titel: "QED: Die seltsame Theorie des Lichts",
    key: "physics, quantum",
    ausgelBy: "—",
    avail: true,
  },
  {
    id: 7,
    angelegt: "05.05.2016",
    jahr: "2014",
    sig: "GEO-2",
    autor: "Darwin, Charles",
    titel: "On the Origin of Species",
    key: "biology, evolution",
    ausgelBy: "Müller, Hans",
    avail: false,
  },
  {
    id: 8,
    angelegt: "19.02.2019",
    jahr: "2018",
    sig: "MAT-7",
    autor: "Hawking, Stephen",
    titel: "A Brief History of Time",
    key: "physics, cosmology",
    ausgelBy: "—",
    avail: true,
  },
  {
    id: 9,
    angelegt: "30.06.2021",
    jahr: "2020",
    sig: "IT-92",
    autor: "Knuth, Donald",
    titel: "The Art of Computer Programming, Vol. 1",
    key: "software, algorithms",
    ausgelBy: "Richter, Klaus",
    avail: false,
  },
  {
    id: 10,
    angelegt: "14.11.2017",
    jahr: "2016",
    sig: "BIO-8",
    autor: "Watson, James",
    titel: "Die Doppelhelix",
    key: "biology, DNA",
    ausgelBy: "—",
    avail: true,
  },
  {
    id: 11,
    angelegt: "28.03.2020",
    jahr: "2019",
    sig: "CHE-11",
    autor: "Pauling, Linus",
    titel: "General Chemistry",
    key: "chemistry, general",
    ausgelBy: "Braun, Eva",
    avail: false,
  },
  {
    id: 12,
    angelegt: "07.12.2023",
    jahr: "2023",
    sig: "IT-105",
    autor: "Turing, Alan",
    titel: "Computing Machinery and Intelligence",
    key: "AI, computing",
    ausgelBy: "—",
    avail: true,
  },
];

const BORROWERS = [
  "Fink, Laura",
  "Weber, Max",
  "Frank, Anna",
  "Müller, Hans",
  "Richter, Klaus",
  "Braun, Eva",
  "Schmidt, Karl",
  "Becker, Maria",
  "Hoffmann, Jens",
  "Wagner, Petra",
];

function pad2(n: number) {
  return n.toString().padStart(2, "0");
}

function generated(): Book[] {
  const out: Book[] = [];
  for (let i = SEED.length + 1; i <= 234; i++) {
    const seed = SEED[i % SEED.length];
    const day = ((i * 7) % 28) + 1;
    const month = ((i * 3) % 12) + 1;
    const year = 2008 + ((i * 5) % 16);
    const avail = i % 3 === 0;
    out.push({
      id: i,
      angelegt: `${pad2(day)}.${pad2(month)}.${year}`,
      jahr: String(2005 + ((i * 11) % 19)),
      sig: `${seed.sig.split("-")[0]}-${100 + i}`,
      autor: seed.autor,
      titel: `${seed.titel} (Band ${1 + (i % 9)})`,
      key: seed.key,
      ausgelBy: avail ? "—" : BORROWERS[i % BORROWERS.length],
      avail,
    });
  }
  return out;
}

export const BOOKS: Book[] = [...SEED, ...generated()];

import { NextResponse } from "next/server";
import { BOOKS } from "@/components/features/books/mock-data";
import type { Book } from "@/components/features/books/types";
import type { MagicFilter, ResultSet } from "@/lib/rs/types";

// Mock of POST /rs/book/list. Mirrors the Spring Boot AbstractPagesRest contract:
// accepts a MagicFilter, returns a ResultSet<Book>. Replace by a rewrite to the
// Spring backend once auth is wired.

export async function POST(request: Request): Promise<Response> {
  const filter = (await request.json()) as MagicFilter;

  const search = filter.searchString?.trim().toLowerCase();
  let rows: Book[] = BOOKS;

  if (search) {
    rows = rows.filter(
      (b) =>
        b.titel.toLowerCase().includes(search) ||
        b.autor.toLowerCase().includes(search) ||
        b.sig.toLowerCase().includes(search) ||
        b.key.toLowerCase().includes(search)
    );
  }

  const sort = filter.sortProperties?.[0];
  if (sort?.property) {
    const prop = sort.property as keyof Book;
    const dir = sort.sortOrder === "DESCENDING" ? -1 : 1;
    rows = [...rows].sort((a, b) => {
      const av = a[prop];
      const bv = b[prop];
      if (av === bv) return 0;
      if (av == null) return -dir;
      if (bv == null) return dir;
      return av < bv ? -dir : dir;
    });
  }

  const totalSize = rows.length;
  const pageSize =
    filter.paginationPageSize ??
    (filter.entries?.find((e) => e.field === "paginationPageSize")?.value?.value
      ? Number(
          filter.entries.find((e) => e.field === "paginationPageSize")?.value
            ?.value
        )
      : 50);
  const pageIndex = Number(filter.extended?.page ?? 0);
  const start = pageIndex * pageSize;
  const paged = rows.slice(start, start + pageSize);

  const result: ResultSet<Book> = {
    resultSet: paged,
    totalSize,
    paginationPageSize: pageSize,
  };
  return NextResponse.json(result);
}

import { NextResponse } from "next/server";
import {
  findBook,
  updateBook,
} from "@/components/features/books/mock-data";
import type { BookDetail } from "@/components/features/books/types";

// Mock of GET / PUT /rs/book/{id}. Mirrors the Spring Boot
// AbstractPagesRest.getItem / saveOrUpdate contract. Replace by a rewrite to
// the Spring backend once auth is wired.

interface Params {
  params: Promise<{ id: string }>;
}

function parseId(raw: string): number | null {
  const id = Number(raw);
  return Number.isFinite(id) && id > 0 ? id : null;
}

export async function GET(_req: Request, ctx: Params): Promise<Response> {
  const { id: raw } = await ctx.params;
  const id = parseId(raw);
  if (id == null) return NextResponse.json({ error: "Invalid id" }, { status: 400 });
  const book = findBook(id);
  if (!book) return NextResponse.json({ error: "Not found" }, { status: 404 });
  return NextResponse.json(book);
}

export async function PUT(req: Request, ctx: Params): Promise<Response> {
  const { id: raw } = await ctx.params;
  const id = parseId(raw);
  if (id == null) return NextResponse.json({ error: "Invalid id" }, { status: 400 });
  const body = (await req.json()) as Partial<BookDetail>;
  const saved = updateBook(id, body);
  if (!saved) return NextResponse.json({ error: "Not found" }, { status: 404 });
  return NextResponse.json(saved);
}

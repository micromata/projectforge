import { NextResponse } from "next/server";
import { getBookHistory } from "@/components/features/books/mock-data";

// Mock of GET /rs/book/history/{id}. Mirrors the Spring Boot
// AbstractPagesRest history endpoint that returns
// List<DisplayHistoryEntry>.

interface Params {
  params: Promise<{ id: string }>;
}

export async function GET(_req: Request, ctx: Params): Promise<Response> {
  const { id: raw } = await ctx.params;
  const id = Number(raw);
  if (!Number.isFinite(id) || id <= 0) {
    return NextResponse.json({ error: "Invalid id" }, { status: 400 });
  }
  return NextResponse.json(getBookHistory(id));
}

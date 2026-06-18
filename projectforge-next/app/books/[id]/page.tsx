import { notFound } from "next/navigation";
import { SidebarInset, SidebarProvider } from "@/components/ui/sidebar";
import { AppSidebar } from "@/components/shared/app-sidebar";
import { BrandStripe } from "@/components/shared/brand-stripe";
import { BookEditForm } from "@/components/features/books/edit/book-edit-form";

interface Props {
  params: Promise<{ id: string }>;
}

export default async function BookEditPage({ params }: Props) {
  const { id: raw } = await params;
  const id = Number(raw);
  if (!Number.isFinite(id) || id <= 0) notFound();

  return (
    <div className="flex h-screen flex-col overflow-hidden">
      <BrandStripe />
      <SidebarProvider className="!min-h-0 flex flex-1 overflow-hidden">
        <AppSidebar />
        <SidebarInset className="flex flex-1 flex-col overflow-hidden">
          <BookEditForm bookId={id} />
        </SidebarInset>
      </SidebarProvider>
    </div>
  );
}

export function BrandStripe() {
  return (
    <div
      className="h-[3.5px] w-full shrink-0"
      style={{
        background:
          "linear-gradient(90deg, var(--brand-teal) 0%, var(--brand-teal) 25%, var(--brand-pink) 25%, var(--brand-pink) 50%, var(--brand-yellow) 50%, var(--brand-yellow) 75%, var(--brand-green) 75%, var(--brand-green) 100%)",
      }}
    />
  );
}

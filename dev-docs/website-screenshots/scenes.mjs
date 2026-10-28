// The scenes of the website screenshots: name (= file name), path below /next, wait (ms), act (interaction), dark,
// height (of the viewport, default 860).
// Viewport coordinates are CSS pixels of the 1440 x 860 viewport.

// Workaround: the day view button shows its raw i18n key "calendar.day" (bug, to be fixed in the frontend).
const fixDayButton = (p) =>
  p.evaluate(() => { for (const b of document.querySelectorAll("button")) if (b.textContent.trim() === "calendar.day") b.textContent = "Day"; });

const selectTeamCalendar = async (p) => {
  if (await p.getByText("Delivery team").count()) return; // Remembered from an earlier run.
  await p.mouse.click(400, 130); // Calendar picker in the header.
  await p.waitForTimeout(500);
  await p.keyboard.type("Deliv");
  await p.waitForTimeout(1500);
  await p.getByText("Delivery team").last().click({ timeout: 5000 });
  await p.waitForTimeout(800);
  await p.keyboard.press("Escape");
};

const removeEmployeeFilter = async (p) => {
  const chip = p.getByText("Employee: Laura Becker");
  if (!(await chip.count())) return;
  await chip.click();
  await p.waitForTimeout(800);
  await p.getByRole("button", { name: "Delete" }).click();
  await p.waitForTimeout(2500);
};

// Closes the notification banners (e.g. the direct message of chat-seed.mjs), for this session only.
const closeBanners = async (p) => {
  const banner = p.getByTestId("notification-banner");
  while (await banner.count()) {
    await banner.first().locator("button").last().click();
    await p.waitForTimeout(300);
  }
};

export default [
  { name: "2026-Menu", path: "/timesheet", act: async (p) => { await p.getByText("Menu", { exact: true }).first().click(); } },
  { name: "2026-Calendar", path: "/calendar", act: async (p) => {
      await selectTeamCalendar(p);
      await p.getByRole("button", { name: "Workdays" }).nth(1).click(); // Week, workdays only.
      await p.waitForTimeout(2000);
      await fixDayButton(p);
  } },
  // The team with its vacations and availability entries (seeded selection, VacationExportTestDataContributor).
  { name: "2026-TeamVacationCalendar", path: "/vacationExport", wait: 3000 },
  // Alex's vacation is a conflict: the substitute Max isn't available on three of its days.
  { name: "2026-Vacations", path: "/vacation", wait: 2500 },
  { name: "2026-Availability", path: "/availability", wait: 2500 },
  { name: "2026-Timesheets", path: "/timesheet", act: removeEmployeeFilter },
  { name: "2026-TaskTree", path: "/taskTree", wait: 2500, act: async (p) => {
      // The tree remembers expanded nodes; expand Bluefin > Fleet Tracking Platform if not open yet.
      for (const n of ["Bluefin Logistics GmbH", "Fleet Tracking Platform"]) {
        if (!(await p.getByText("Specification", { exact: true }).count())) {
          await p.getByText(n, { exact: true }).first().click();
          await p.waitForTimeout(900);
        }
      }
      await p.mouse.click(1300, 760);
  } },
  { name: "2026-OrderBook", path: "/order", wait: 3000 },
  { name: "2026-Invoice", path: "/invoice", wait: 3000 },
  { name: "2026-Statistics", path: "/finance/statistics", wait: 5000 },
  { name: "2026-Liquidity", path: "/liquidity?tab=forecast", wait: 2500, act: async (p) => {
      const start = p.locator("input").first();
      await start.fill("420000");
      await start.press("Enter");
      await p.mouse.click(1300, 180);
      await p.keyboard.press("Escape");
      await p.waitForTimeout(3000);
  } },
  { name: "2026-DataTransfer", path: "/datatransfer", wait: 2500, act: async (p) => {
      await p.getByText("Alpine Insurance – Claims Portal").first().click();
      await p.waitForTimeout(3000);
  } },
  // Sample problems of dashboard-problems.kts: tiles with figures and trend bars.
  { name: "2026-SystemDashboard", path: "/systemDashboard", wait: 3500, dark: true, height: 640, act: closeBanners },
  { name: "2026-Dark", path: "/finance/statistics", wait: 5000, dark: true },
];

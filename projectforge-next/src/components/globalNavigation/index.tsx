import Link from 'next/link'
import Image from 'next/image'

export const GlobalNavigation = () => {
  return (
    <div className="absolute top-0 left-0 right-[10%] bottom-24 overflow-auto md:relative">
      <div className="flex flex-col p-2 gap-4 md:p-4">
        <div className="flex flex-col gap-2">
          <span className="font-bold">Allgemein</span>
          <Link href="/calendar">Kalender</Link>
          <Link href="/projects">Kalenderliste</Link>
          <Link href="/tasks">Urlaubseinträge</Link>
          <Link href="/timesheets">Bücher</Link>
          <Link href="/timesheets">Adressbücher</Link>
          <Link href="/timesheets">Adressen</Link>
          <Link href="/timesheets">Direktwahl</Link>
          <Link href="/timesheets">SMS senden</Link>
          <Link href="/timesheets">Suchen</Link>
        </div>
        <div className="flex flex-col gap-2">
          <span className="font-bold">Administration</span>
          <Link href="/calendar">Mein Zugang</Link>
          <Link href="/projects">2. Faktor einrichten</Link>
          <Link href="/tasks">Meine Einstellungen</Link>
        </div>
        <div className="flex flex-row justify-between items-center gap-2 text-xs text-zinc-600 pt-8">
          <Image src="/projectforge.png" alt="Company Logo" width={65} height={43} />
          <div className="flex flex-col items-end">
            <a href="https://www.projectforge.org">www.projectforge.org</a>
            <a
              href="https://www.projectforge.org/changelog-posts"
              className="flex flex-col items-end"
            >
              <span>master@a72cc00 - 8.1-SNAPSHOT</span>
              <p>&copy; 2001-2025 Micromata GmbH</p>
            </a>
          </div>
        </div>
      </div>
    </div>
  )
}

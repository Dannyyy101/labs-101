import ErrorState from "@/components/ErrorState"
import { getSettings } from "./action"
import SettingsForm from "./SettingsForm"

export default async function Settings() {
    const settings = await getSettings()

    return <div className="flex-1 w-full bg-muted/50 px-3.5 py-5 md:px-6 md:py-7">
        <div className="mx-auto flex max-w-2xl flex-col gap-5">
            <h1 className="text-[34px] font-bold leading-tight tracking-tight">Einstellungen</h1>
            {settings.ok
                ? <SettingsForm settings={settings.data} />
                // no form here, it would show the default instead of the real goal
                : <ErrorState title="Einstellungen nicht verfügbar" message={settings.error} />}
        </div>
    </div>
}

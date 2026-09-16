'use client'

import { useCallback, useState } from 'react'
import {
    BarcodeScanner,
    BarcodeScannerProvider,
    useTorch,
    type DetectedBarcode
} from 'react-barcode-scanner'
import 'react-barcode-scanner/polyfill'

const FORMATS = ['ean_13', 'ean_8', 'upc_a', 'upc_e']

type ScannerPanelProps = {
    onDetected?: (barcode: string) => void
}

export default function ScannerPanel(props: ScannerPanelProps) {
    return (
        <BarcodeScannerProvider>
            <ScannerSurface {...props} />
        </BarcodeScannerProvider>
    )
}

function ScannerSurface({ onDetected }: ScannerPanelProps) {
    const [code, setCode] = useState<string>()
    const [cameraError, setCameraError] = useState<Error>()

    const handleCapture = useCallback(
        (barcodes: DetectedBarcode[]) => {
            const value = barcodes[0]?.rawValue
            if (!value) return
            setCode(value)
            navigator.vibrate?.(24)
        },
        []
    )

    return (
        <div
            className={[
                "rounded-2xl",
                'relative w-full select-none overflow-hidden bg-neutral-950 text-neutral-50',
                'aspect-[3/4] sm:aspect-[4/3]',
                '[&_video]:absolute [&_video]:inset-0 [&_video]:size-full [&_video]:object-cover'
            ].join(' ')}
        >
            {cameraError ? (
                <CameraError error={cameraError} />
            ) : (
                <>
                    <BarcodeScanner
                        options={{ formats: FORMATS, delay: 400 }}
                        paused={!!code}
                        onCapture={handleCapture}
                        onCameraError={setCameraError}
                        playsInline
                        muted
                    />

                    {/* Zielfenster: breit und flach wie ein EAN-13 */}
                    <div className="pointer-events-none absolute inset-0 grid place-items-center">
                        <div
                            className={[
                                'relative aspect-[5/2] w-[80%] max-w-sm rounded-xl transition-colors duration-200',
                                'shadow-[0_0_0_9999px_rgba(0,0,0,0.55)]',
                                code ? 'ring-2 ring-lime-300' : 'ring-1 ring-white/25'
                            ].join(' ')}
                        >
                            <Bracket className="left-0 top-0 rounded-tl-xl border-l-2 border-t-2" active={!!code} />
                            <Bracket className="right-0 top-0 rounded-tr-xl border-r-2 border-t-2" active={!!code} />
                            <Bracket className="bottom-0 left-0 rounded-bl-xl border-b-2 border-l-2" active={!!code} />
                            <Bracket className="bottom-0 right-0 rounded-br-xl border-b-2 border-r-2" active={!!code} />
                        </div>
                    </div>
                </>
            )}

            {/* Kopfzeile liegt über dem Bild, statt Höhe zu verbrauchen */}
            <div className="pointer-events-none absolute inset-x-0 top-0 bg-gradient-to-b from-black/70 to-transparent px-4 pb-8 pt-[max(0.875rem,env(safe-area-inset-top))]">
                <p className="text-sm font-medium">Produkt scannen</p>
            </div>

            {/* Fußzeile: Hinweis, Licht, Ergebnis */}
            <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/85 via-black/60 to-transparent px-4 pb-[max(1rem,env(safe-area-inset-bottom))] pt-10">
                {code ? (
                    <div className="flex flex-col gap-3">
                        <p className="text-center font-mono text-lg tabular-nums tracking-[0.3em] text-lime-300">
                            {code}
                        </p>
                        <div className="flex gap-2">
                            <button
                                type="button"
                                onClick={() => setCode(undefined)}
                                className="h-11 flex-1 rounded-full border border-white/20 text-sm font-medium text-neutral-100 transition-colors hover:bg-white/10 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-lime-300"
                            >
                                Nochmal
                            </button>
                            <button
                                type="button"
                                onClick={() => onDetected?.(code)}
                                className="h-11 flex-[2] rounded-full bg-lime-300 text-sm font-medium text-neutral-950 transition-colors hover:bg-lime-200 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-lime-300"
                            >
                                Hinzufügen
                            </button>
                        </div>
                    </div>
                ) : (
                    <div className="flex items-center justify-between gap-3">
                        <p className="text-sm text-neutral-300">Code quer ins Fenster halten</p>
                        <TorchButton />
                    </div>
                )}
            </div>
        </div>
    )
}

function Bracket({ className, active }: { className: string; active: boolean }) {
    return (
        <span
            aria-hidden
            className={`absolute size-6 transition-colors duration-200 ${active ? 'border-lime-300' : 'border-white'
                } ${className}`}
        />
    )
}

function TorchButton() {
    const { isTorchSupported, isTorchOn, setIsTorchOn } = useTorch()
    if (!isTorchSupported) return null

    return (
        <button
            type="button"
            onClick={() => setIsTorchOn(!isTorchOn)}
            aria-pressed={isTorchOn}
            className={`grid size-11 shrink-0 place-items-center rounded-full transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-lime-300 ${isTorchOn ? 'bg-lime-300 text-neutral-950' : 'bg-white/15 text-neutral-100 hover:bg-white/25'
                }`}
        >
            <span aria-hidden>☀</span>
            <span className="sr-only">Licht {isTorchOn ? 'aus' : 'an'}schalten</span>
        </button>
    )
}

function CameraError({ error }: { error: Error }) {
    const denied = error.name === 'NotAllowedError'

    return (
        <div className="absolute inset-0 grid place-items-center px-8 text-center">
            <div className="max-w-xs">
                <p className="text-sm font-medium">Kamera nicht verfügbar</p>
                <p className="mt-2 text-sm leading-relaxed text-neutral-400">
                    {denied
                        ? 'Kamerazugriff ist blockiert. In den Website-Einstellungen freigeben und neu laden.'
                        : 'Die Kamera lässt sich nicht starten. Prüfe, ob eine andere App sie benutzt und ob die Seite über HTTPS läuft.'}
                </p>
            </div>
        </div>
    )
}
'use client'

import Image from "next/image"
import { ReactNode, useState } from "react"

/**
 * Profile picture from zitadel, `fallback` (the initial) while there is none: users without a picture
 * get a 404 there. unoptimized: the host of zitadel differs between local and prod.
 */
export function AvatarImage({ src, alt, size, className, fallback }: { src: string | null, alt: string, size: number, className?: string, fallback: ReactNode }) {
    const [failed, setFailed] = useState<string | null>(null)
    if (!src || failed === src) return fallback
    return <Image className={className} src={src} alt={alt} width={size} height={size} unoptimized onError={() => setFailed(src)} />
}

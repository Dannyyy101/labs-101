'use client'

import { ThemeProvider as NextThemesProvider } from 'next-themes'

if (process.env.NODE_ENV !== 'production' && typeof window !== 'undefined') {
    const originalError = console.error
    console.error = (...args: unknown[]) => {
        if (typeof args[0] === 'string' && args[0].includes('Encountered a script tag')) return
        originalError(...args)
    }
}

export function ThemeProvider({ children, ...props }: React.ComponentProps<typeof NextThemesProvider>) {
    return <NextThemesProvider {...props}>{children}</NextThemesProvider>
}
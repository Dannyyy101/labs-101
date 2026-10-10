"use client"

import * as React from "react"
import { format } from "date-fns"
import { de } from "date-fns/locale"

import { cn } from "@/lib/utils"
import { Calendar } from "@/components/ui/calendar"
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover"

/** Gray pill with the date like in Apple Calendar, opens a month calendar. */
export function DatePicker({ date, setDate, className }: { date: Date, setDate: (date: Date) => void, className?: string }) {
  const [open, setOpen] = React.useState(false)

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger
        className={cn("rounded-md bg-muted px-2.5 py-1 text-[15px] tabular-nums hover:bg-muted/70 data-popup-open:text-[#0a84ff]", className)}
      >
        {format(date, "d. MMM yyyy", { locale: de })}
      </PopoverTrigger>
      <PopoverContent className="w-auto p-0" align="end">
        <Calendar mode="single" selected={date} locale={de} required
          onSelect={(next) => {
            setDate(next)
            setOpen(false)
          }} />
      </PopoverContent>
    </Popover>
  )
}

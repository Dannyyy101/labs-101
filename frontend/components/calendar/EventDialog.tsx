'use client'
import { addTimeToDate, toTimeInputValue } from "@/utils/date";
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "../ui/accordion";
import { Button } from "../ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "../ui/card";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import { Separator } from "../ui/separator";
import { DatePicker } from "./datePicker";
import { Training } from "./calendar";
import { FormEvent, SubmitEventHandler, useEffect, useState } from "react";
import { useEventStore } from "@/lib/zustand/eventStore";
import { WorkoutTemplate } from "@/utils/types/workoutTypes";
import { createCalendarEvent, deleteCalendarEvent, getAllWorkoutTemplates } from "./action";
import { Empty, EmptyHeader, EmptyMedia, EmptyTitle, EmptyDescription, EmptyContent } from "../ui/empty";
import { Spinner } from "../ui/spinner";
import { CalendarEvent } from "@/utils/types/calendarTypes";

export function EventDialog({ calendarEvent, closeDialog, startDate }: { calendarEvent: CalendarEvent | null, closeDialog: () => void, startDate: Date | null }) {
    const setEvents = useEventStore((state) => state.setEvents)
    const events = useEventStore((state) => state.events)
    const endDate = new Date(startDate || new Date())
    endDate.setHours(endDate.getHours() + 1)


    const [event, setEvent] = useState<CalendarEvent>(calendarEvent || { id: "", title: "", startDate: startDate || new Date(), endDate });
    const [workoutTemplates, setWorkoutTemplates] = useState<Map<string, WorkoutTemplate[]> | null>()
    const [workoutTemplatesLoading, setWorkoutTemplatesLoading] = useState<boolean>(true)

    useEffect(() => {
        const fetch = async () => {
            setWorkoutTemplates((await getAllWorkoutTemplates()).value)
            setWorkoutTemplatesLoading(false)
        }
        fetch()
    }, [])

    const addTrainingToEvent = (training: WorkoutTemplate) => {

        setEvent({ ...event, title: training.name, training })
    }

    const saveNewEvent = async (formData: FormData) => {
        addTimeToDate(formData.get("time-picker-start")?.toString() || "", event.startDate)
        addTimeToDate(formData.get("time-picker-end")?.toString() || "", event.endDate)
        setEvents([...events, event])
        console.log(event)
        if (!event.id) {
            await createCalendarEvent(event)
        }
        setEvent({ id: "", title: "", startDate: new Date(), endDate: new Date() })
        closeDialog();
    }

    const deleteEvent = async () => {
        if (event.id) {
            await deleteCalendarEvent(event.id)
            setEvents([...events.filter((e) => e.id !== event.id)])
            setEvent({ id: "", title: "", startDate: new Date(), endDate: new Date() })
            closeDialog();
        }
    }

    return <>
        <CardHeader>
            <CardTitle>Create new Event</CardTitle>
            <CardDescription>
            </CardDescription>
        </CardHeader>
        <CardContent>
            <form action={saveNewEvent}>
                <div className="grid gap-2 mb-4">
                    <Label htmlFor="title">Title</Label>
                    <Input name="title" className="w-1/2 h-8" value={event.title} />
                </div>

                <div className="flex gap-2">
                    <div className="grid gap-2">
                        <Label htmlFor="email">Start Date</Label>
                        <div className='flex'>
                            <DatePicker date={event.startDate} setDate={(date) => setEvent((prev) => ({ ...prev, startDate: date }))} />
                            <Input
                                type='time'
                                id='time-picker-start'
                                name="time-picker-start"
                                step='1'
                                defaultValue={toTimeInputValue(event.startDate)}
                                className='bg-background appearance-none [&::-webkit-calendar-picker-indicator]:hidden [&::-webkit-calendar-picker-indicator]:appearance-none'
                            />
                        </div>
                    </div>
                    <div className="grid gap-2">
                        <Label htmlFor="password">End Date</Label>
                        <div className='flex'>
                            <DatePicker date={event.endDate} setDate={(date) => setEvent((prev) => ({ ...prev, endDate: date }))} />
                            <Input
                                type='time'
                                id='time-picker-end'
                                name='time-picker-end'
                                step='1'
                                defaultValue={toTimeInputValue(event.endDate)}
                                className='bg-background appearance-none [&::-webkit-calendar-picker-indicator]:hidden [&::-webkit-calendar-picker-indicator]:appearance-none'
                            />
                        </div>
                    </div>
                </div>
                <Separator className={"my-2"} />

                <div>
                    <h2 className='font-heading text-base font-medium'>Exercises</h2>
                    {workoutTemplates ?
                        <Accordion defaultValue={[...workoutTemplates?.keys() ?? []]} className="mt-1">
                            {[...workoutTemplates].map(([key, templates]) => (
                                <AccordionItem key={key} value={key}>
                                    <AccordionTrigger>{key}</AccordionTrigger>
                                    <AccordionContent className="flex gap-4">
                                        {templates.map((training) => (
                                            <Card
                                                key={training.id}
                                                style={{ borderWidth: event.training?.id === training.id ? "1px" : "0" }}
                                                className="w-md border-black"
                                                onClick={() => addTrainingToEvent(training)}
                                            >
                                                <CardHeader>
                                                    <CardTitle>{training.name}</CardTitle>
                                                    <CardDescription>training</CardDescription>
                                                </CardHeader>
                                            </Card>
                                        ))}
                                    </AccordionContent>
                                </AccordionItem>
                            ))}
                        </Accordion>
                        : <EmptyOrLoadingScreen loading={workoutTemplatesLoading} />
                    }
                </div>
                <div className="flex gap-2 mt-4">
                    {event.id && <Button variant="destructive" className="min-w-1/2" onClick={deleteEvent}>
                        Delete event
                    </Button>}
                    <Button style={{ width: `${event.id ? "50%" : "100%"}` }} type="submit">
                        {event.id ? "Update event" : "Create new event"}
                    </Button>
                </div>
            </form>
        </CardContent>
        <CardFooter className="flex-col gap-2">

        </CardFooter></>
}

const EmptyOrLoadingScreen = ({ loading }: { loading: boolean }) => {
    if (loading) {
        return <Spinner className="size-6" />
    }
    return <Empty>
        <EmptyHeader>
            <EmptyTitle>No workout templates</EmptyTitle>
            <EmptyDescription>You haven't created any workout templates yes. Get started by creating your first template</EmptyDescription>
        </EmptyHeader>
        <EmptyContent>
            <Button>Create template</Button>
        </EmptyContent>
    </Empty>
}
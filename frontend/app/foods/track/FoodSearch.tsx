'use client'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { InputGroup, InputGroupInput, InputGroupAddon } from "@/components/ui/input-group";
import { Spinner } from "@/components/ui/spinner";
import { AlertCircle, Barcode, Search } from "lucide-react";
import InfiniteScroll from "react-infinite-scroll-component";
import { Food, Meal, SearchFood } from "@/utils/types/food";
import { ReactNode, useState } from "react";
import { findAndSafeFoodIfNotExistByBarcode, findFoodByNameAndUserId } from "./action";
import { Button } from "@/components/ui/button";
import FoodTrackView from "./FoodTrackView";
import ScannerPanel from "@/components/ScannerPanel";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { SelectedFoodAction, SelectedFoodState } from "@/lib/zustand/selectedFood";

const FoodSearch: React.FC<{ meal: Meal, selectedFoodStore: SelectedFoodState, children?: ReactNode, className?: string }> = ({ meal, selectedFoodStore, children, className }) => {

    const [food, setFood] = useState<SearchFood[]>([])
    const [hasMore, setHasMore] = useState(true);
    const [searchInput, setSearchInput] = useState<string>("")
    const [open, setOpen] = useState<boolean>(false)
    const [showBarcodeScanner, setShowBarcodeScanner] = useState<boolean>(false)
    const [page, setPage] = useState(0)

    const findByName = async (name: string) => {
        setSearchInput(name)
        const result = await findFoodByNameAndUserId(name)
        setFood(result.content)
        setPage(0)
        setHasMore(!result.last)
    }

    const fetchMore = async () => {
        const next = await findFoodByNameAndUserId(searchInput, { page: page + 1 })
        setFood((prev) => [...prev, ...next.content])
        setPage(next.number)
        setHasMore(!next.last)
    }
    const [error, setError] = useState<string | null>(null)
    const [loading, setLoading] = useState(false)

    const lookupFood = async (code: string) => {
        setError(null)
        setLoading(true)
        const result = await findAndSafeFoodIfNotExistByBarcode(code)
        setLoading(false)

        if (result.ok) {
            selectedFoodStore.selectFood(meal as any, result.data.id, SelectedFoodAction.CREATING)
            setShowBarcodeScanner(false)
        } else {
            setError(result.error)
        }
    }

    const toggleBarcodeScanner = () => {
        setShowBarcodeScanner((prev) => !prev)
        setError(null)
    }
    const [showDialog, setShowDialog] = useState<"SEARCH" | "TEXT">("SEARCH")

    return <Dialog open={selectedFoodStore.showSearch} onOpenChange={selectedFoodStore.setShowSearch}>
        <DialogTrigger className={className}>{children}</DialogTrigger>
        <DialogContent className="w-4xl flex flex-col ">
            <section>
                <div className="flex gap-x-1">
                    <button style={{ backgroundColor: showDialog === "TEXT" ? "black" : "var(--accent)", color: showDialog === "TEXT" ? "white" : "black" }} onClick={() => setShowDialog("TEXT")} className="rounded-xl bg-accent w-16 py-1">Text</button>
                    <button style={{ backgroundColor: showDialog === "SEARCH" ? "black" : "var(--accent)", color: showDialog === "SEARCH" ? "white" : "black" }} onClick={() => setShowDialog("SEARCH")} className="rounded-xl bg-accent w-16 py-1">Suchen</button>
                </div>
                <div className="bg-accent flex rounded-md h-8 p-0.5 mt-2">
                    <button className="w-full rounded-md font-semibold text-sm" style={{ backgroundColor: selectedFoodStore.meal === Meal.BREAKFAST ? "white" : "transparent" }} onClick={() => selectedFoodStore.setMeal(Meal.BREAKFAST)}>Frühstück</button>
                    <button className="w-full rounded-md font-semibold" style={{ backgroundColor: selectedFoodStore.meal === Meal.LUNCH ? "white" : "transparent" }} onClick={() => selectedFoodStore.setMeal(Meal.LUNCH)}>Mittagessen</button>
                    <button className="w-full rounded-md font-semibold" style={{ backgroundColor: selectedFoodStore.meal === Meal.DINNER ? "white" : "transparent" }} onClick={() => selectedFoodStore.setMeal(Meal.DINNER)}>Abendessen</button>
                    <button className="w-full rounded-md font-semibold" style={{ backgroundColor: selectedFoodStore.meal === Meal.SNACK ? "white" : "transparent" }} onClick={() => selectedFoodStore.setMeal(Meal.SNACK)}>Snack</button>
                </div>
                {showDialog === "SEARCH" ?
                    <>
                        <InputGroup className="w-full mt-2 h-10 mb-2 rounded-md">
                            <InputGroupInput value={searchInput} placeholder="Suchen..." onChange={(e) => findByName(e.target.value)} />
                            <InputGroupAddon>
                                <Search />
                            </InputGroupAddon>
                            <button className="mr-2" onClick={toggleBarcodeScanner}><Barcode /></button>
                        </InputGroup>
                        {error && (
                            <Alert variant="destructive" className="mt-2">
                                <AlertCircle className="size-4" />
                                <AlertTitle>Fehler</AlertTitle>
                                <AlertDescription>{error}</AlertDescription>
                            </Alert>
                        )}
                        {loading && <div className="w-full flex justify-center mt-2"><Spinner className="size-4" /></div>}
                        {showBarcodeScanner ? <ScannerPanel
                            onDetected={(ean) => {
                                setShowBarcodeScanner(false)
                                lookupFood(ean)
                            }}
                        /> :
                            <InfiniteScroll
                                className="rounded-2xl w-full max-h-96 overflow-y-auto mt-2 flex flex-col"
                                dataLength={food.length}
                                next={fetchMore}
                                hasMore={hasMore}
                                loader={<div className="w-full flex justify-center"><Spinner className="size-4" /></div>}
                                endMessage={<p style={{ textAlign: 'center' }}>All items loaded.</p>}
                            >
                                {food.map((f) => (
                                    <Button
                                        className="hover:bg-accent  max-w-96 text-left flex justify-start whitespace-normal h-auto py-2"
                                        variant="ghost"
                                        onClick={() => selectedFoodStore.selectFood(meal as any, f.id, SelectedFoodAction.CREATING)}
                                        key={f.id}
                                    >
                                        {f.name}
                                    </Button>
                                ))}
                            </InfiniteScroll>
                        }
                    </>
                    :
                    <div></div>
                }

            </section>

        </DialogContent>
    </Dialog>
}


export default FoodSearch;
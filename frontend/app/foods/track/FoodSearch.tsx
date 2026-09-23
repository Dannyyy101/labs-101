'use client'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { InputGroup, InputGroupInput, InputGroupAddon } from "@/components/ui/input-group";
import { Spinner } from "@/components/ui/spinner";
import { AlertCircle, Barcode, Search } from "lucide-react";
import InfiniteScroll from "react-infinite-scroll-component";
import { Food, Meal, SearchFood } from "@/utils/types/food";
import { useState } from "react";
import { findAndSafeFoodIfNotExistByBarcode, findFoodByNameAndUserId } from "./action";
import { Button } from "@/components/ui/button";
import FoodTrackView from "./FoodTrackView";
import ScannerPanel from "@/components/ScannerPanel";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { SelectedFoodAction, SelectedFoodState } from "@/lib/zustand/selectedFood";

export default function FoodSearch({ meal, selectedFoodStore }: {
    meal: string, selectedFoodStore: SelectedFoodState
}) {

    const [food, setFood] = useState<SearchFood[]>([])
    const [hasMore, setHasMore] = useState(true);
    const [searchInput, setSearchInput] = useState<string>("")
    const [open, setOpen] = useState<boolean>(false)
    const [showBarcodeScanner, setShowBarcodeScanner] = useState<boolean>(false)

    const findByName = async (name: string) => {
        setSearchInput(name)
        const foundFood = await findFoodByNameAndUserId(name)
        setFood(foundFood.content)
        setHasMore(foundFood.totalElements % foundFood.size !== 0)
    }

    const fetchMore = async () => {
        const next = await findFoodByNameAndUserId(searchInput, { page: food.length / 20 });
        if (next.totalElements % next.size !== 0) {
            setHasMore(false)
        }
        setFood((prev) => [...prev, ...next.content]);
    };
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

    return <Dialog open={selectedFoodStore.showSearch} onOpenChange={selectedFoodStore.setShowSearch}>
        <DialogTrigger className="">Add Food</DialogTrigger>
        <DialogContent className="w-4xl flex flex-col ">
            <section>
                <>
                    <InputGroup className="max-w-xs mt-8 mb-2 md:mt-0">
                        <InputGroupInput value={searchInput} placeholder="Search..." onChange={(e) => findByName(e.target.value)} />
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
                            className="bg-accent rounded-2xl w-full max-h-96 overflow-y-auto p-2 mt-2 flex flex-col"
                            dataLength={food.length}
                            next={fetchMore}
                            hasMore={hasMore}
                            loader={<div className="w-full flex justify-center"><Spinner className="size-4" /></div>}
                            endMessage={<p style={{ textAlign: 'center' }}>All items loaded.</p>}
                        >
                            {food.map((f) => (
                                <Button
                                    className="max-w-96 text-left flex justify-start whitespace-normal h-auto py-2"
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

            </section>

        </DialogContent>
    </Dialog>
}

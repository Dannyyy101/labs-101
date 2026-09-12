'use client'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { InputGroup, InputGroupInput, InputGroupAddon } from "@/components/ui/input-group";
import { Spinner } from "@/components/ui/spinner";
import { Search } from "lucide-react";
import InfiniteScroll from "react-infinite-scroll-component";
import { Food } from "@/utils/types/food";
import { useState } from "react";
import { findFoodByNameAndUserId } from "./action";
import { Button } from "@/components/ui/button";
import FoodTrackView from "./FoodTrackView";

export default function FoodSearch({ meal }: { meal: string }) {

    const [food, setFood] = useState<Food[]>([])
    const [hasMore, setHasMore] = useState(true);
    const [searchInput, setSearchInput] = useState<string>("")
    const [focusedFood, setFocusedFood] = useState<Food | null>(null)
    const [open, setOpen] = useState<boolean>(false)

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
    return <Dialog open={open} onOpenChange={setOpen}>
        <DialogTrigger className="">Add Food</DialogTrigger>
        <DialogContent className="w-4xl flex flex-col">
            <section>
                {!focusedFood ?
                    <>
                        <InputGroup className="max-w-xs ">
                            <InputGroupInput value={searchInput} placeholder="Search..." onChange={(e) => findByName(e.target.value)} />
                            <InputGroupAddon>
                                <Search />
                            </InputGroupAddon>

                        </InputGroup>
                        <InfiniteScroll
                            className="bg-accent rounded-2xl w-full max-h-96 overflow-y-auto p-2 mt-2 flex flex-col"
                            dataLength={food.length}
                            next={fetchMore}
                            hasMore={hasMore}
                            loader={<div className="w-full flex justify-center"><Spinner className="size-4" /></div>}
                            endMessage={<p style={{ textAlign: 'center' }}>All items loaded.</p>}
                        >
                            {food.map((f) => <Button className={"w-full text-left"} variant={"ghost"} onClick={() => setFocusedFood(f)} key={f.id}>{f.name}</Button>)}
                        </InfiniteScroll>
                    </>
                    : <FoodTrackView props={{ meal: meal, food: focusedFood, back: () => setFocusedFood(null), closeView: () => setOpen(false) }} />}
            </section>
        </DialogContent>
    </Dialog>
}
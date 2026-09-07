'use client'
import { Search } from "lucide-react"

import {
    InputGroup,
    InputGroupAddon,
    InputGroupInput,
} from "@/components/ui/input-group"
import { useState } from "react"
import { Food } from "@/utils/types/food"
import { findFoodByNameAndUserId } from "./action"

import InfiniteScroll from 'react-infinite-scroll-component';


export default function TrackFood() {

    const [food, setFood] = useState<Food[]>([])
    const [hasMore, setHasMore] = useState(true);
    const [searchInput, setSearchInput] = useState<string>("")


    const findByName = async (name: string) => {
        console.log("Name")
        setSearchInput(name)
        const foundFood = await findFoodByNameAndUserId(name)
        setFood(foundFood.content)
    }

    const fetchMore = async () => {
        const next = await findFoodByNameAndUserId(searchInput, { page: food.length / 20 });
        if (next.totalElements % next.size !== 0) {
            setHasMore(false)
        }
        setFood((prev) => [...prev, ...next.content]);
    };

    return (
        <div className="w-full h-[90vh] relative">
            <InputGroup className="max-w-xs absolute top-1/4 left-1/2 transform -translate-x-1/2 -translate-y-1/2">
                <InputGroupInput value={searchInput} placeholder="Search..." onChange={(e) => findByName(e.target.value)} />
                <InputGroupAddon>
                    <Search />
                </InputGroupAddon>

                <InfiniteScroll
                    className="bg-accent rounded-2xl absolute top-8 left-0 w-full max-h-96 overflow-y-auto p-2"
                    dataLength={food.length}
                    next={fetchMore}
                    hasMore={hasMore}
                    loader={<p>Loading...</p>}
                    endMessage={<p style={{ textAlign: 'center' }}>All items loaded.</p>}
                >
                    {food.map((f) => <button className="text-left" key={f.id}>{f.name}</button>)}
                </InfiniteScroll>
            </InputGroup>
        </div>
    )


}
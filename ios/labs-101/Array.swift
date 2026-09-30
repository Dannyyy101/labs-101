//
//  Array.swift
//  labs-101
//
//  Created by Daniel Stöcklein on 29.09.26.
//

extension Array {
    func chunked(into size: Int) -> [[Element]] {
        return stride(from: 0, to: count, by: size).map {
            Array(self[$0 ..< Swift.min($0 + size, count)])
        }
    }
}

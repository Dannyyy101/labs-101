//
//  HealthkitManager.swift
//  labs-101
//
//  Created by Daniel Stöcklein on 22.08.26.
//
import HealthKit
import UIKit

struct Workout: Encodable {
    let uuid: UUID
    let totalDistance: Double
}

class HealthKitManager {
    let url = URL(string: "http://192.168.178.54:8080/api/workouts/sync")!

    let healthStore = HKHealthStore();
    
    let requestTypes: Set = [
        HKQuantityType.workoutType(),
        HKQuantityType(.activeEnergyBurned),
        HKQuantityType(.distanceCycling),
        HKQuantityType(.distanceWalkingRunning),
        HKQuantityType(.distanceWheelchair),
        HKQuantityType(.heartRate),
        HKQuantityType(.dietaryEnergyConsumed),
        HKQuantityType(.stepCount)
    ]

    
    func requestExerciseData () {
        let sampleType = HKObjectType.workoutType()
        let query = HKSampleQuery(sampleType: sampleType, predicate: nil, limit: Int(HKObjectQueryNoLimit), sortDescriptors: nil) {
            query, results, error in
            
            
            
            guard let samples = results as? [HKWorkout] else {
                print("Error: \(String(describing: error))")
                return
            }
            
            var workouts: [Workout] = []
            
            var last = samples.last
            
            HKQuery.predicateForObject(with: <#T##UUID#>)
            
            for sample in samples {
                workouts.append(Workout(uuid: sample.uuid,
                                        totalDistance: sample.statistics(for: HKQuantityType(.distanceWalkingRunning))?.sumQuantity()?.doubleValue(for: HKUnit.meter()) ?? 0))
            }
            
       
            var request = URLRequest(url: self.url)
            request.httpMethod = "POST"
            
            let data = try! JSONEncoder().encode(workouts)
            request.httpBody = data
            
            request.setValue(
                "application/json",
                forHTTPHeaderField: "Content-Type"
            )
            
            let task = URLSession.shared.dataTask(with: request) { data, response, error in
                let statusCode = (response as! HTTPURLResponse).statusCode

                if statusCode == 200 {
                    print("SUCCESS")
                } else {
                    print("FAILURE")
                }
            }

            //task.resume()

            DispatchQueue.main.async {

            }
        }
        
        healthStore.execute(query)
    }
    
}

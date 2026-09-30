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

protocol HealthData : Encodable {
    var uuid: UUID {get}
    var type: String {get}
}

struct AnyHealthData: Encodable {
    let base: any HealthData
    
    func encode(to encoder: Encoder) throws {
        try base.encode(to: encoder)
    }
}

struct Steps : HealthData  {
    let uuid: UUID
    var type: String
    let value: Double
    let createdAt: Date
    let updatedAt: Date
}


struct SyncData: Encodable {
    var items: [AnyHealthData]
    var deleted: [AnyHealthData]
}

class HealthKitManager {
    let url = URL(string: "http://192.168.178.60:8080/api/users/0Hatub6x8Vxyp3L8YNG5QHG7s579dTai/sync/apple-health")!

    let healthStore = HKHealthStore();
    
    var myAnchor: HKQueryAnchor?;
    
    var syncData: SyncData = SyncData(items: [], deleted: [])
    
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

    
    func requestStepsData () {
        guard let stepCountType = HKObjectType.quantityType(forIdentifier: .stepCount) else {
            fatalError("*** Unable to get the step count type ***")
        }
       
        let query = HKAnchoredObjectQuery(type: stepCountType,
                                          predicate: nil,
                                          anchor: myAnchor,
                                          limit: HKObjectQueryNoLimit)
        { (query, samplesOrNil, deletedObjectsOrNil, newAnchor, errorOrNil) in
            
            guard let samples = samplesOrNil, let deletedObjects = deletedObjectsOrNil else {
                return
            }
            
            self.myAnchor = newAnchor
            
            guard let stepSamples = samples as? [HKQuantitySample] else { return }
            
            for sample in stepSamples {
                let steps = sample.quantity.doubleValue(for: .count())
                let stepsData : Steps = Steps(uuid: sample.uuid,type: "STEPS", value: steps, createdAt: sample.startDate, updatedAt: sample.endDate)
                self.syncData.items.append(AnyHealthData(base: stepsData))
                print("Amount: \(steps), CreatedAt: \(stepsData.createdAt), UpdatedAt: \(stepsData.updatedAt)")
            }
                        
            guard let deletedStepSamples = deletedObjects as? [HKQuantitySample] else { return }

            
            for deletedStepCountSamples in deletedStepSamples {
                let steps = deletedStepCountSamples.quantity.doubleValue(for: .count())
                let stepsData : Steps = Steps(uuid: deletedStepCountSamples.uuid,type: "STEPS", value: steps, createdAt: deletedStepCountSamples.startDate, updatedAt: deletedStepCountSamples.endDate)
                self.syncData.deleted.append(AnyHealthData(base: stepsData))
                print("Amount: \(steps), CreatedAt: \(stepsData.createdAt), UpdatedAt: \(stepsData.updatedAt)")
            }
            
            print(self.syncData.items.count + self.syncData.deleted.count)
            
            
            var request = URLRequest(url: self.url)
            request.httpMethod = "POST"
            
            let encoder = JSONEncoder()
            encoder.dateEncodingStrategy = .secondsSince1970
            
            for chunk in self.syncData.items.chunked(into: 1000){
                
                let data = try! encoder.encode(SyncData(items: chunk, deleted: []))
                print(data)
                request.httpBody = data
                
                request.setValue(
                    "application/json",
                    forHTTPHeaderField: "Content-Type"
                )
                
                
                
                let task = URLSession.shared.dataTask(with: request) { data, response, error in
                    let statusCode = (response as! HTTPURLResponse).statusCode
                    
                    if statusCode == 204 {
                        print("SUCCESS")
                    } else {
                        print("FAILURE")
                    }
                }
                
                task.resume()
            }
            DispatchQueue.main.async {

            }
    
        }
        
        healthStore.execute(query)
    }
    
}

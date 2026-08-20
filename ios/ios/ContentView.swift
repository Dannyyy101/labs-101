import SwiftUI
import HealthKitUI

let allTypes: Set = [
    HKQuantityType.workoutType(),
    HKQuantityType(.activeEnergyBurned),
    HKQuantityType(.distanceCycling),
    HKQuantityType(.distanceWalkingRunning),
    HKQuantityType(.distanceWheelchair),
    HKQuantityType(.heartRate)
]

struct MyView: View {
    @State var accessRequested = false
    @State var trigger = false
    
    var healthStore = HKHealthStore()


    var body: some View {
        Button("Access health data") {
            // OK to read or write HealthKit data here.
        }
        .disabled(!accessRequested)
        
        // If HealthKit data is available, request authorization
        // when this view appears.
        .onAppear() {
            
            // Check that Health data is available on the device.
            if HKHealthStore.isHealthDataAvailable() {
                // Modifying the trigger initiates the health data
                // access request.
                trigger.toggle()
            }
        }
        
        // Requests access to share and read HealthKit data types
        // when the trigger changes.
        .healthDataAccessRequest(store: healthStore,
                                 shareTypes: allTypes,
                                 readTypes: allTypes,
                                 trigger: trigger) { result in
            switch result {
                
            case .success(_):
                accessRequested = true
            case .failure(let error):
                // Handle the error here.
                print("Fehler")
            }
        }
    }
}

#Preview {
    MyView()
}

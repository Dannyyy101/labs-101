import SwiftUI
import HealthKitUI



struct ContentView: View {
    @State var accessRequested = false
    @State var trigger = false
    
    let manager = HealthKitManager()
    
    let healthStore = HKHealthStore()


    var body: some View {
        Button("Fetch Workouts") {
            manager.requestExerciseData()
        }
        Button("Access health data") {
            // OK to read or write HealthKit data here.
        }
        .disabled(!accessRequested)
        
        // If HealthKit data is available, request authorization
        // when this view appears.
        .onAppear() {
            
            // Check that Health data is available on the device.
            if HKHealthStore.isHealthDataAvailable() {
                trigger.toggle()
            }
        }
        
        // Requests access to share and read HealthKit data types
        // when the trigger changes.
        .healthDataAccessRequest(store: manager.healthStore,
                                 shareTypes: manager.requestTypes,
                                 readTypes: manager.requestTypes,
                                 trigger: trigger) { result in
            switch result {
                
            case .success(_):
                accessRequested = true
            case .failure(let error):
                // Handle the error here.
                fatalError("*** An error occurred while requesting authentication: \(error) ***")
            }
        }
    }
}
#Preview {
    ContentView()
}

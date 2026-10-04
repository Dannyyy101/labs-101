import HealthKit

/// Every HealthKit type the app reads and syncs.
///
/// The identifiers and their units come from `HKTypeIdentifiers.h` of the SDK.
/// They are created from their raw value, so identifiers of newer iOS versions
/// are simply skipped on older devices instead of failing to compile.
nonisolated enum HealthTypeCatalog {
    static let quantityTypes: [HKQuantityType] = quantityUnits.keys.sorted().compactMap {
        HKObjectType.quantityType(forIdentifier: HKQuantityTypeIdentifier(rawValue: "HKQuantityTypeIdentifier" + $0))
    }

    static let categoryTypes: [HKCategoryType] = categoryIdentifiers.compactMap {
        HKObjectType.categoryType(forIdentifier: HKCategoryTypeIdentifier(rawValue: "HKCategoryTypeIdentifier" + $0))
    }

    /// Blood pressure and food group samples of other types, reading them needs
    /// the permission for those types, not for the correlation type itself.
    static let correlationTypes: [HKCorrelationType] = [.init(.bloodPressure), .init(.food)]

    static let otherSampleTypes: [HKSampleType] = [
        .workoutType(),
        .electrocardiogramType(),
        .audiogramSampleType(),
        .stateOfMindType(),
        HKScoredAssessmentType(.GAD7),
        HKScoredAssessmentType(.PHQ9),
    ]

    /// Synced with anchored queries, workouts first because they are the most interesting ones.
    static let syncedTypes: [HKSampleType] = otherSampleTypes + quantityTypes + categoryTypes + correlationTypes

    static let characteristicTypes: [HKCharacteristicType] = [
        .init(.dateOfBirth), .init(.biologicalSex), .init(.bloodType),
        .init(.fitzpatrickSkinType), .init(.wheelchairUse), .init(.activityMoveMode),
    ]

    static let readTypes: Set<HKObjectType> = Set(quantityTypes + categoryTypes + otherSampleTypes + characteristicTypes)
        // the locations of workouts are read through their routes
        .union([HKSeriesType.workoutRoute()])

    /// Types samples from the website may be written as (see HealthWriter).
    /// HealthKit refuses to request write access for computed types like
    /// exercise time, so this is a curated list instead of all types.
    static let shareTypes: Set<HKSampleType> = Set([
        .bodyMass, .height, .bodyFatPercentage, .leanBodyMass, .waistCircumference, .bodyMassIndex,
        .heartRate, .bloodPressureSystolic, .bloodPressureDiastolic, .bloodGlucose, .oxygenSaturation,
        .bodyTemperature, .respiratoryRate, .stepCount, .distanceWalkingRunning, .distanceCycling,
        .activeEnergyBurned, .dietaryWater, .dietaryCaffeine, .dietaryEnergyConsumed, .dietaryProtein,
        .dietaryCarbohydrates, .dietaryFatTotal, .dietaryFatSaturated, .dietarySugar, .dietaryFiber,
        .dietarySodium, .dietaryCholesterol,
    ].map { HKQuantityType($0) } + [
        HKCategoryType(.mindfulSession), HKCategoryType(.sleepAnalysis), .workoutType(),
    ])

    /// The canonical unit of the type, every sample of a type is stored in the same unit.
    static func unit(for type: HKQuantityType) -> HKUnit {
        units[type.identifier] ?? .count()
    }

    private static let units: [String: HKUnit] = Dictionary(uniqueKeysWithValues: quantityUnits.map { name, unit in
        let hkUnit: HKUnit = switch unit {
        case "": .count()
        case "appleEffortScore": .appleEffortScore()
        default: HKUnit(from: unit)
        }
        return ("HKQuantityTypeIdentifier" + name, hkUnit)
    })

    /// Name without the HKQuantityTypeIdentifier/HKCategoryTypeIdentifier prefix → unit
    /// (checked against `is(compatibleWith:)` for every type)
    private static let quantityUnits: [String: String] = [
        "AppleSleepingWristTemperature": "degC",
        "BodyFatPercentage": "%",
        "BodyMass": "kg",
        "BodyMassIndex": "count",
        "ElectrodermalActivity": "S",
        "Height": "m",
        "LeanBodyMass": "kg",
        "WaistCircumference": "m",
        "ActiveEnergyBurned": "kcal",
        "AppleExerciseTime": "min",
        "AppleMoveTime": "min",
        "AppleStandTime": "min",
        "BasalEnergyBurned": "kcal",
        "CrossCountrySkiingSpeed": "m/s",
        "CyclingCadence": "count/min",
        "CyclingFunctionalThresholdPower": "W",
        "CyclingPower": "W",
        "CyclingSpeed": "m/s",
        "DistanceCrossCountrySkiing": "m",
        "DistanceCycling": "m",
        "DistanceDownhillSnowSports": "m",
        "DistancePaddleSports": "m",
        "DistanceRowing": "m",
        "DistanceSkatingSports": "m",
        "DistanceSwimming": "m",
        "DistanceWalkingRunning": "m",
        "DistanceWheelchair": "m",
        "EstimatedWorkoutEffortScore": "appleEffortScore",
        "FlightsClimbed": "count",
        "NikeFuel": "count",
        "PaddleSportsSpeed": "m/s",
        "PhysicalEffort": "kcal/(kg*hr)",
        "PushCount": "count",
        "RowingSpeed": "m/s",
        "RunningPower": "W",
        "RunningSpeed": "m/s",
        "StepCount": "count",
        "SwimmingStrokeCount": "count",
        "UnderwaterDepth": "m",
        "WorkoutEffortScore": "appleEffortScore",
        "EnvironmentalAudioExposure": "dBASPL",
        "EnvironmentalSoundReduction": "dBASPL",
        "HeadphoneAudioExposure": "dBASPL",
        "AtrialFibrillationBurden": "%",
        "HeartRate": "count/s",
        "HeartRateRecoveryOneMinute": "count/min",
        "HeartRateVariabilityRMSSD": "ms",
        "HeartRateVariabilitySDNN": "ms",
        "PeripheralPerfusionIndex": "%",
        "RestingHeartRate": "count/min",
        "VO2Max": "ml/(kg*min)",
        "WalkingHeartRateAverage": "count/min",
        "AppleWalkingSteadiness": "%",
        "RunningGroundContactTime": "ms",
        "RunningStrideLength": "m",
        "RunningVerticalOscillation": "cm",
        "SixMinuteWalkTestDistance": "m",
        "StairAscentSpeed": "m/s",
        "StairDescentSpeed": "m/s",
        "WalkingAsymmetryPercentage": "%",
        "WalkingDoubleSupportPercentage": "%",
        "WalkingSpeed": "m/s",
        "WalkingStepLength": "m",
        "DietaryBiotin": "g",
        "DietaryCaffeine": "g",
        "DietaryCalcium": "g",
        "DietaryCarbohydrates": "g",
        "DietaryChloride": "g",
        "DietaryCholesterol": "g",
        "DietaryChromium": "g",
        "DietaryCopper": "g",
        "DietaryEnergyConsumed": "kcal",
        "DietaryFatMonounsaturated": "g",
        "DietaryFatPolyunsaturated": "g",
        "DietaryFatSaturated": "g",
        "DietaryFatTotal": "g",
        "DietaryFiber": "g",
        "DietaryFolate": "g",
        "DietaryIodine": "g",
        "DietaryIron": "g",
        "DietaryMagnesium": "g",
        "DietaryManganese": "g",
        "DietaryMolybdenum": "g",
        "DietaryNiacin": "g",
        "DietaryPantothenicAcid": "g",
        "DietaryPhosphorus": "g",
        "DietaryPotassium": "g",
        "DietaryProtein": "g",
        "DietaryRiboflavin": "g",
        "DietarySelenium": "g",
        "DietarySodium": "g",
        "DietarySugar": "g",
        "DietaryThiamin": "g",
        "DietaryVitaminA": "g",
        "DietaryVitaminB12": "g",
        "DietaryVitaminB6": "g",
        "DietaryVitaminC": "g",
        "DietaryVitaminD": "g",
        "DietaryVitaminE": "g",
        "DietaryVitaminK": "g",
        "DietaryWater": "mL",
        "DietaryZinc": "g",
        "BloodAlcoholContent": "%",
        "BloodPressureDiastolic": "mmHg",
        "BloodPressureSystolic": "mmHg",
        "InsulinDelivery": "IU",
        "NumberOfAlcoholicBeverages": "count",
        "NumberOfTimesFallen": "count",
        "TimeInDaylight": "min",
        "UVExposure": "",
        "WaterTemperature": "degC",
        "BasalBodyTemperature": "degC",
        "AppleSleepingBreathingDisturbances": "count",
        "ForcedExpiratoryVolume1": "L",
        "ForcedVitalCapacity": "L",
        "InhalerUsage": "count",
        "OxygenSaturation": "%",
        "PeakExpiratoryFlowRate": "L/min",
        "RespiratoryRate": "count/s",
        "BloodGlucose": "mg/dL",
        "BodyTemperature": "degC",
    ]

    private static let categoryIdentifiers: [String] = [
        "AppleStandHour",
        "EnvironmentalAudioExposureEvent",
        "HeadphoneAudioExposureEvent",
        "HighHeartRateEvent",
        "HypertensionEvent",
        "IrregularHeartRhythmEvent",
        "LowCardioFitnessEvent",
        "LowHeartRateEvent",
        "MindfulSession",
        "AppleWalkingSteadinessEvent",
        "HandwashingEvent",
        "ToothbrushingEvent",
        "BleedingAfterMenopause",
        "BleedingAfterPregnancy",
        "BleedingDuringPregnancy",
        "CervicalMucusQuality",
        "Contraceptive",
        "InfrequentMenstrualCycles",
        "IntermenstrualBleeding",
        "IrregularMenstrualCycles",
        "Lactation",
        "MenopausalState",
        "MenstrualFlow",
        "OvulationTestResult",
        "PersistentIntermenstrualBleeding",
        "Pregnancy",
        "PregnancyTestResult",
        "ProgesteroneTestResult",
        "ProlongedMenstrualPeriods",
        "SexualActivity",
        "SleepApneaEvent",
        "SleepAnalysis",
        "AbdominalCramps",
        "Acne",
        "AppetiteChanges",
        "BladderIncontinence",
        "Bloating",
        "BreastPain",
        "ChestTightnessOrPain",
        "Chills",
        "Constipation",
        "Coughing",
        "Diarrhea",
        "Dizziness",
        "DrySkin",
        "Fainting",
        "Fatigue",
        "Fever",
        "GeneralizedBodyAche",
        "HairLoss",
        "Headache",
        "Heartburn",
        "HotFlashes",
        "LossOfSmell",
        "LossOfTaste",
        "LowerBackPain",
        "MemoryLapse",
        "MoodChanges",
        "Nausea",
        "NightSweats",
        "PelvicPain",
        "RapidPoundingOrFlutteringHeartbeat",
        "RunnyNose",
        "ShortnessOfBreath",
        "SinusCongestion",
        "SkippedHeartbeat",
        "SleepChanges",
        "SoreThroat",
        "VaginalDryness",
        "Vomiting",
        "Wheezing",
    ]
}

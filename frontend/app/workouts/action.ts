'use server'

import { workoutTypeToName } from "@/lib/workout"
import { Result, Workout, WorkoutHeaderData } from "@/utils/types"

export async function getWorkoutsHeaderData(): Promise<Result<WorkoutHeaderData>> {
    const restingHeartRateTimeline = [];
    const VO2MaxTimeline = [];
    const zone2Peace = [];

    const startDate = new Date(2026, 6, 1);   // 1. Januar 2026
    const endDate = new Date(2026, 7, 31);    // 31. August 2026

    let currentDate = new Date(startDate);

    while (currentDate <= endDate) {
        const rhrValue = Math.floor(Math.random() * (70 - 55 + 1)) + 55;

        const vo2Value = parseFloat((Math.random() * (50 - 40) + 40).toFixed(1));

        const zone2PeaceValue = Math.floor(Math.random() * (20 - 55 + 1)) + 55;

        restingHeartRateTimeline.push({
            value: rhrValue,
            date: new Date(currentDate)
        });

        VO2MaxTimeline.push({
            value: vo2Value,
            date: new Date(currentDate)
        });

        zone2Peace.push({
            value: zone2PeaceValue,
            date: new Date(currentDate)
        });

        currentDate.setDate(currentDate.getDate() + 1);
    }

    return {
        value: {
            totalWorkouts: 10, totalWorkoutTime: { value: 300, unit: "min" }, caloriesBurned: {
                "unit": "kcal",
                "value": 365.0
            },
            restingHeartRateTimeline,
            VO2MaxTimeline,
            zone2Peace
        },
        error: null
    }
}

export async function getAllWorkouts(): Promise<Result<Workout[]>> {
    const result = [{
        "uuid": "4E2F1A0B-5C8D-4E9F-8A7B-1C2D3E4F5A6B",
        "workoutActivityType": 37,
        "workoutActivityTypeName": "HKWorkoutActivityTypeRunning",
        "startDate": new Date(),
        "endDate": new Date(),
        "duration": 1800.0,
        "totalDistance": {
            "unit": "m",
            "value": 4800.0
        },
        "totalEnergyBurned": {
            "unit": "kcal",
            "value": 365.0
        },
        "HKQuantityTypeIdentifierHeartRate": {
            "sumQuantity": null,
            "averageQuantity": { "unit": "count/min", "value": 152.0 },
            "minimumQuantity": { "unit": "count/min", "value": 110.0 },
            "maximumQuantity": { "unit": "count/min", "value": 178.0 },
            "mostRecentQuantity": { "unit": "count/min", "value": 140.0 },
            "mostRecentQuantityDateInterval": {
                "start": new Date(),
                "end": new Date()
            }
        }
    },
    {
        // Cycling
        "uuid": "F23A9B4C-1D8E-45A9-B2F3-C7D8E9F0A1B2",
        "workoutActivityType": 13,
        "workoutActivityTypeName": "HKWorkoutActivityTypeCycling",
        "startDate": new Date(),
        "endDate": new Date(),
        "duration": 3600.0,
        "totalDistance": {
            "unit": "m",
            "value": 25000.0
        },
        "totalEnergyBurned": {
            "unit": "kcal",
            "value": 580.0
        },
        "HKQuantityTypeIdentifierHeartRate": {
            "sumQuantity": null,
            "averageQuantity": { "unit": "count/min", "value": 145.0 },
            "minimumQuantity": { "unit": "count/min", "value": 95.0 },
            "maximumQuantity": { "unit": "count/min", "value": 168.0 },
            "mostRecentQuantity": { "unit": "count/min", "value": 135.0 },
            "mostRecentQuantityDateInterval": {
                "start": new Date(),
                "end": new Date()
            }
        }
    },
    {
        // Swimming
        "uuid": "8A7B6C5D-4E3F-2A1B-9C8D-7E6F5A4B3C2D",
        "workoutActivityType": 46,
        "workoutActivityTypeName": "HKWorkoutActivityTypeSwimming",
        "startDate": new Date(),
        "endDate": new Date(),
        "duration": 1800.0,
        "totalDistance": {
            "unit": "m",
            "value": 1500.0
        },
        "totalEnergyBurned": {
            "unit": "kcal",
            "value": 410.0
        },
        "HKQuantityTypeIdentifierHeartRate": {
            "sumQuantity": null,
            "averageQuantity": { "unit": "count/min", "value": 138.0 },
            "minimumQuantity": { "unit": "count/min", "value": 105.0 },
            "maximumQuantity": { "unit": "count/min", "value": 160.0 },
            "mostRecentQuantity": { "unit": "count/min", "value": 128.0 },
            "mostRecentQuantityDateInterval": {
                "start": new Date(),
                "end": new Date()
            }
        }
    },
    {
        // Walking
        "uuid": "1A2B3C4D-5E6F-7A8B-9C0D-1E2F3A4B5C6D",
        "workoutActivityType": 52,
        "workoutActivityTypeName": "HKWorkoutActivityTypeWalking",
        "startDate": new Date(),
        "endDate": new Date(),
        "duration": 2700.0,
        "totalDistance": {
            "unit": "m",
            "value": 4200.0
        },
        "totalEnergyBurned": {
            "unit": "kcal",
            "value": 210.0
        },
        "HKQuantityTypeIdentifierHeartRate": {
            "sumQuantity": null,
            "averageQuantity": { "unit": "count/min", "value": 105.0 },
            "minimumQuantity": { "unit": "count/min", "value": 75.0 },
            "maximumQuantity": { "unit": "count/min", "value": 125.0 },
            "mostRecentQuantity": { "unit": "count/min", "value": 98.0 },
            "mostRecentQuantityDateInterval": {
                "start": new Date(),
                "end": new Date()
            }
        }
    },
    {
        // Traditional Strength Training
        "uuid": "D6C5B4A3-F2E1-D0C9-B8A7-F6E5D4C3B2A1",
        "workoutActivityType": 50,
        "workoutActivityTypeName": "HKWorkoutActivityTypeTraditionalStrengthTraining",
        "startDate": new Date(),
        "endDate": new Date(),
        "duration": 2400.0,
        "totalDistance": {
            "unit": "m",
            "value": 0.0 // Distance is typically 0 for strength training, but Apple often includes the key anyway.
        },
        "totalEnergyBurned": {
            "unit": "kcal",
            "value": 320.0
        },
        "HKQuantityTypeIdentifierHeartRate": {
            "sumQuantity": null,
            "averageQuantity": { "unit": "count/min", "value": 125.0 },
            "minimumQuantity": { "unit": "count/min", "value": 85.0 },
            "maximumQuantity": { "unit": "count/min", "value": 172.0 },
            "mostRecentQuantity": { "unit": "count/min", "value": 110.0 },
            "mostRecentQuantityDateInterval": {
                "start": new Date(),
                "end": new Date()
            }
        }
    }]

    result.forEach((item) => {
        item.workoutActivityTypeName = workoutTypeToName(item.workoutActivityType)
    })

    return {
        value: result, error: null
    }
}
export interface paths {
    "/api/calendar": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["calendarGetAllCalendarEvents"];
        put?: never;
        post: operations["calendarCreateCalendarEvent"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/calendar/external": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["externalCalendarGetCalendars"];
        put?: never;
        post: operations["externalCalendarAddCalendar"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/calendar/external/apple": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["externalCalendarConnectAppleAccount"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/calendar/external/apple/{accountId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        delete: operations["externalCalendarDisconnectAppleAccount"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/calendar/external/events": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["externalCalendarGetEvents"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/calendar/external/events/sync": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["externalCalendarSyncEvents"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/calendar/external/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        delete: operations["externalCalendarDeleteCalendar"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/calendar/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put: operations["calendarUpdateCalendarEvent"];
        post?: never;
        delete: operations["calendarDeleteCalendarEvent"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/foods": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["foodGetAll"];
        put?: never;
        post: operations["foodCreate"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/foods/bar-code/{code}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["foodGetByBarcode"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/foods/extract": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["foodExtract"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/foods/open-food": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["foodCreateOpenFood"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/foods/open-food/{openFoodId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["foodGetOpenFood"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/foods/open-food/{openFoodId}/import": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["foodImportOpenFood"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/foods/open-food/{openFoodId}/track": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["foodTrackOpenFood"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/foods/search/byNameAndUser": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["foodSearch"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/foods/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put: operations["foodUpdateFood"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/foods/{id}/portions": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["foodAddFoodPortions"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/foods/{id}/track": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["foodTrackFoodForUser"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["userSearchUsers"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["userGetProfile"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/food-images": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["foodImageGetAll"];
        put?: never;
        post: operations["foodImageUpload"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/food-images/usage": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["foodImageUsage"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/food-images/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["foodImageGet"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/food-images/{id}/accept": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["foodImageAccept"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/food-images/{id}/image": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["foodImageGetImage"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/food-images/{id}/reject": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["foodImageReject"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/foods/{foodId}/last": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["userGetLastTracked"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/friends": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["friendGetFriends"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/friends/requests": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["friendSendRequest"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/friends/requests/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put: operations["friendAcceptRequest"];
        post?: never;
        delete: operations["friendDeleteRequest"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/friends/{friendId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        delete: operations["friendRemoveFriend"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/friends/{friendId}/cheers": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["friendCheer"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/friends/{friendId}/profile": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["friendGetProfile"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/health/characteristics": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["healthGetCharacteristics"];
        put: operations["healthSaveCharacteristics"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/health/overview": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["healthOverviewGetOverview"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/health/overview/energy": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["healthOverviewGetEnergy"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/health/samples": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["healthGetSamples"];
        put?: never;
        post: operations["healthUploadSamples"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/health/samples/delete": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["healthDeleteSamples"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/health/sync/complete": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["healthCompleteFullSync"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/health/types": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["healthGetSummary"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/health/write-requests": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["healthGetWriteRequests"];
        put?: never;
        post: operations["healthCreateWriteRequest"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/health/write-requests/{id}/ack": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["healthAcknowledgeWriteRequest"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/notifications": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["notificationGetNotifications"];
        put?: never;
        post: operations["notificationCreateNotification"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/notifications/read": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put: operations["notificationMarkAllRead"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/notifications/unread-count": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["notificationGetUnreadCount"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/notifications/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        delete: operations["notificationDeleteNotification"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/notifications/{id}/read": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put: operations["notificationMarkRead"];
        post?: never;
        delete: operations["notificationMarkUnread"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/profile": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["userProfileGetProfile"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/runs": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["runGetRuns"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/runs/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["runGetRun"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/settings": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["userSettingsGetSettings"];
        put: operations["userSettingsUpdateSettings"];
        post: operations["userSettingsCreateSettings"];
        delete: operations["userSettingsDeleteSettings"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/tracked-foods": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["userGetTrackedFoodForUser"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/tracked-foods/{trackedFoodId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["userGetTrackedFoodById"];
        put: operations["userUpdateTrackedFoodById"];
        post?: never;
        delete: operations["userDeleteTrackedFoodById"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/workout-sessions": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["workoutSessionGetSessions"];
        put?: never;
        post: operations["workoutSessionStart"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/workout-sessions/active": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["workoutSessionGetActive"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/workout-sessions/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["workoutSessionGetSession"];
        put: operations["workoutSessionUpdate"];
        post?: never;
        delete: operations["workoutSessionDelete"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me/workout-sessions/{id}/finish": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["workoutSessionFinish"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/workouts": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["workoutGetAllWorkouts"];
        put?: never;
        post: operations["workoutCreateWorkout"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/workouts/exercises": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["workoutGetAllExercises"];
        put?: never;
        post: operations["workoutCreateExercise"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/workouts/exercises/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put: operations["workoutUpdateExerciseById"];
        post?: never;
        delete: operations["workoutDeleteExercise"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/workouts/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["workoutGetWorkoutById"];
        put: operations["workoutUpdateWorkout"];
        post?: never;
        delete: operations["workoutDeleteWorkout"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/workouts/{id}/shares": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put: operations["workoutShareWorkout"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/workouts/{id}/shares/me": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        delete: operations["workoutLeaveWorkout"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
}
export type webhooks = Record<string, never>;
export interface components {
    schemas: {
        AcceptFoodImageDto: {
            items: components["schemas"]["Item"][];
            meal: string;
        };
        AchievementDto: {
            /** Format: date */
            achievedAt: string | null;
            id: string;
            /** Format: double */
            progress: number | null;
            /** Format: double */
            target: number;
        };
        AckHealthWriteRequestDto: {
            error?: string | null;
            /** Format: uuid */
            sampleUuid?: string | null;
        };
        ActivityDayDto: {
            /** Format: date */
            date: string;
            loggedIn: boolean;
            /** Format: int32 */
            meals: number;
            /** Format: double */
            runDistance: number;
            /** Format: int32 */
            runs: number;
            /** Format: double */
            workoutMinutes: number;
            /** Format: int32 */
            workouts: number;
        };
        ActivityDto: {
            /** Format: double */
            activeEnergy: number;
            /** Format: double */
            basalEnergy: number;
            /** Format: double */
            daylightMinutes: number;
            /** Format: double */
            distance: number;
            /** Format: double */
            energyConsumed: number | null;
            /** Format: double */
            exerciseMinutes: number;
            /** Format: double */
            flightsClimbed: number;
            /** Format: int32 */
            standHours: number;
            /** Format: double */
            steps: number;
        };
        BestEffortDto: {
            /** Format: double */
            distance: number;
            name: string;
            /** Format: double */
            time: number;
        };
        BodyPartDto: {
            color: string | null;
            /** Format: int64 */
            id: number | null;
            /** Format: int64 */
            intensity: number | null;
            side: string | null;
            slug: string;
        };
        CalendarEventDto: {
            creator: components["schemas"]["CalendarUserDto"] | null;
            creatorId: string;
            /** Format: date-time */
            endDate: string;
            /** Format: int64 */
            id: number;
            invitees: components["schemas"]["CalendarUserDto"][];
            /** @enum {string|null} */
            recurrence: "DAILY" | "WEEKLY" | "MONTHLY" | null;
            /** Format: date-time */
            recurrenceEnd: string | null;
            /** Format: date-time */
            startDate: string;
            title: string;
            /** Format: int64 */
            workoutId: number | null;
            workoutName: string | null;
        };
        CalendarUserDto: {
            id: string;
            image: string | null;
            name: string | null;
        };
        CardioItemDto: Omit<components["schemas"]["ExerciseItem"], "type"> & {
            exercise: components["schemas"]["ExerciseDto"];
            /** Format: int32 */
            minutes: number;
            sets: components["schemas"]["SetDto"][];
        } & {
            /**
             * @description discriminator enum property added by openapi-typescript
             * @enum {string}
             */
            type: "CARDIO_EXERCISE";
        };
        CompleteFullSyncDto: {
            /** Format: uuid */
            fullSyncId: string;
            type: string;
        };
        ConnectAppleAccountDto: {
            appleId: string;
            password: string;
        };
        CreateCalendarEventDto: {
            creatorId?: string | null;
            /** Format: date-time */
            endDate: string;
            inviteeIds?: string[] | null;
            /** @enum {string|null} */
            recurrence?: "DAILY" | "WEEKLY" | "MONTHLY" | null;
            /** Format: date-time */
            recurrenceEnd?: string | null;
            /** Format: date-time */
            startDate: string;
            title: string;
            /** Format: int64 */
            workoutId?: number | null;
        };
        CreateExerciseDto: {
            bodyParts: components["schemas"]["BodyPartDto"][];
            description?: string | null;
            name: string;
            slug?: string | null;
            type: string;
        };
        CreateExternalCalendarDto: {
            name?: string | null;
            url: string;
        };
        CreateFoodDto: {
            blsCode?: string | null;
            /** Format: double */
            carbohydrates?: number | null;
            /** Format: double */
            fat?: number | null;
            /** Format: double */
            fiber?: number | null;
            /** Format: double */
            kcal?: number | null;
            name: string;
            /** Format: double */
            protein?: number | null;
            /** Format: double */
            water?: number | null;
        };
        CreateFoodPortionDto: {
            /** Format: double */
            grams: number;
            isDefault: boolean;
            label: string;
        };
        CreateFoodUserDto: {
            /** Format: double */
            amount: number;
            /** Format: int64 */
            foodId?: number | null;
            meal: string;
            /** Format: int64 */
            portionId?: number | null;
            userId?: string | null;
        };
        CreateHealthWriteRequestDto: {
            /** Format: date-time */
            endDate: string;
            /** @enum {string} */
            kind: "QUANTITY" | "CATEGORY" | "WORKOUT" | "CORRELATION" | "ECG" | "AUDIOGRAM" | "STATE_OF_MIND" | "SCORED_ASSESSMENT";
            metadata?: Record<string, never> | null;
            /** Format: date-time */
            startDate: string;
            type: string;
            unit?: string | null;
            /** Format: double */
            value?: number | null;
        };
        CreateNotificationDto: {
            link?: string | null;
            linkLabel?: string | null;
            message?: string | null;
            title: string;
            /** @enum {string|null} */
            type?: "INFO" | "SUCCESS" | "WARNING" | "ERROR" | null;
        };
        CreateOpenFoodDto: {
            barCode?: string | null;
            /** Format: double */
            carbohydrates?: number | null;
            company?: string | null;
            /** Format: double */
            fat?: number | null;
            /** Format: double */
            fiber?: number | null;
            /** Format: double */
            kcal?: number | null;
            name: string;
            /** Format: double */
            protein?: number | null;
            /** Format: double */
            water?: number | null;
        };
        CreateWorkoutDto: {
            exercises: (components["schemas"]["CardioItemDto"] | components["schemas"]["StrengthItemDto"])[];
            name: string;
            /** Format: date-time */
            scheduledFor?: string | null;
        };
        CreateWorkoutSessionDto: {
            name?: string | null;
            /** Format: int64 */
            workoutId?: number | null;
        };
        DayScoreDto: {
            /** Format: double */
            activeEnergy: number;
            /** Format: date */
            date: string;
            /** Format: double */
            hrv: number | null;
            /** Format: int32 */
            recovery: number | null;
            /** Format: double */
            restingHeartRate: number | null;
            /** Format: double */
            sleep: number | null;
            /** Format: int32 */
            sleepScore: number | null;
            /** Format: double */
            steps: number;
            /** Format: int32 */
            strain: number | null;
        };
        DeleteHealthSamplesDto: {
            uuids: string[];
        };
        EnergyDto: {
            /** Format: double */
            activeEnergy: number;
            /** Format: date */
            date: string;
        };
        ExerciseDto: {
            bodyParts: components["schemas"]["BodyPartDto"][];
            description: string | null;
            /** Format: int64 */
            id: number;
            name: string;
            type: string;
        };
        ExerciseItem: {
            type: string;
        };
        ExternalCalendarDto: {
            /** Format: int64 */
            accountId: number | null;
            appleId: string | null;
            error: string | null;
            /** Format: int64 */
            id: number;
            name: string;
            url: string;
        };
        ExternalCalendarEventDto: {
            allDay: boolean;
            /** Format: int64 */
            calendarId: number;
            calendarName: string;
            /** Format: date-time */
            endDate: string;
            /** Format: date-time */
            startDate: string;
            title: string | null;
            uid: string | null;
        };
        ExtractFoodRequestDto: {
            text: string;
        };
        ExtractedFoodDto: {
            /** Format: double */
            amount: number;
            food: components["schemas"]["FoodWithPortionsDto"] | null;
            /** Format: int64 */
            openFoodId: number | null;
            query: string;
            unit: string | null;
        };
        FoodDto: {
            blsCode: string | null;
            /** Format: double */
            carbohydrates: number | null;
            /** Format: double */
            fat: number | null;
            /** Format: double */
            fiber: number | null;
            /** Format: int64 */
            id: number;
            /** Format: double */
            kcal: number | null;
            name: string;
            /** Format: double */
            protein: number | null;
            /** Format: double */
            water: number | null;
        };
        FoodImageAnalysisDto: {
            /** Format: date-time */
            analyzedAt: string | null;
            /** Format: date-time */
            createdAt: string;
            description: string | null;
            error: string | null;
            /** Format: int64 */
            id: number;
            items: components["schemas"]["FoodImageItemDto"][];
            /** @enum {string} */
            meal: "BREAKFAST" | "LUNCH" | "DINNER" | "SNACK";
            /** Format: date-time */
            reviewedAt: string | null;
            /** @enum {string} */
            status: "PENDING" | "READY" | "FAILED" | "ACCEPTED" | "REJECTED";
        };
        FoodImageItemDto: {
            /** Format: double */
            amount: number;
            food: components["schemas"]["FoodWithPortionsDto"] | null;
            /** Format: double */
            gramsPerUnit: number;
            /** Format: int64 */
            openFoodId: number | null;
            query: string;
            unit: string;
        };
        FoodImageUsageDto: {
            /** Format: int32 */
            limit: number;
            /** Format: int64 */
            used: number;
        };
        FoodPortionDto: {
            /** Format: double */
            grams: number;
            /** Format: int64 */
            id: number;
            isDefault: boolean;
            label: string;
        };
        FoodWithLastEntryAndPortionsDto: {
            blsCode: string | null;
            /** Format: double */
            carbohydrates: number | null;
            /** Format: double */
            fat: number | null;
            /** Format: double */
            fiber: number | null;
            /** Format: int64 */
            id: number;
            /** Format: double */
            kcal: number | null;
            lastEntry: components["schemas"]["LastEntry"] | null;
            name: string;
            portions: components["schemas"]["FoodPortionDto"][];
            /** Format: double */
            protein: number | null;
            /** Format: double */
            water: number | null;
        };
        FoodWithPortionsDto: {
            blsCode: string | null;
            /** Format: double */
            carbohydrates: number | null;
            /** Format: double */
            fat: number | null;
            /** Format: double */
            fiber: number | null;
            /** Format: int64 */
            id: number | null;
            /** Format: double */
            kcal: number | null;
            name: string;
            portions: components["schemas"]["FoodPortionDto"][];
            /** Format: double */
            protein: number | null;
            /** Format: double */
            water: number | null;
        };
        FriendDto: {
            id: string;
            image: string | null;
            name: string | null;
            /** Format: date-time */
            since: string;
            training: components["schemas"]["TrainingDto"] | null;
        };
        FriendProfileDto: {
            friend: components["schemas"]["FriendDto"];
            profile: components["schemas"]["ProfileDto"];
        };
        FriendRequestDto: {
            /** Format: date-time */
            createdAt: string;
            /** Format: int64 */
            id: number;
            user: components["schemas"]["CalendarUserDto"];
        };
        FriendsDto: {
            friends: components["schemas"]["FriendDto"][];
            incoming: components["schemas"]["FriendRequestDto"][];
            outgoing: components["schemas"]["FriendRequestDto"][];
        };
        HealthOverviewDto: {
            activity: components["schemas"]["ActivityDto"];
            /** Format: date */
            date: string;
            heartRate: number[][];
            history: components["schemas"]["DayScoreDto"][];
            recovery: components["schemas"]["RecoveryDto"];
            sleep: components["schemas"]["SleepDto"] | null;
            strain: components["schemas"]["StrainDto"];
            timeZone: string;
            vitals: components["schemas"]["VitalsDto"];
            workouts: components["schemas"]["OverviewWorkoutDto"][];
        };
        HealthSampleDto: {
            /** Format: date-time */
            endDate: string;
            /** @enum {string} */
            kind: "QUANTITY" | "CATEGORY" | "WORKOUT" | "CORRELATION" | "ECG" | "AUDIOGRAM" | "STATE_OF_MIND" | "SCORED_ASSESSMENT";
            payload: Record<string, never> | null;
            sourceBundleId: string | null;
            sourceName: string | null;
            /** Format: date-time */
            startDate: string;
            type: string;
            unit: string | null;
            /** Format: uuid */
            uuid: string;
            /** Format: double */
            value: number | null;
        };
        HealthSyncResultDto: {
            /** Format: int32 */
            count: number;
        };
        HealthTypeSummaryDto: {
            /** Format: int64 */
            count: number;
            /** Format: date-time */
            firstDate: string | null;
            /** @enum {string} */
            kind: "QUANTITY" | "CATEGORY" | "WORKOUT" | "CORRELATION" | "ECG" | "AUDIOGRAM" | "STATE_OF_MIND" | "SCORED_ASSESSMENT";
            /** Format: date-time */
            lastDate: string | null;
            /** Format: date-time */
            lastSyncedAt: string | null;
            type: string;
        };
        HealthWriteRequestDto: {
            /** Format: date-time */
            createdAt: string;
            /** Format: date-time */
            endDate: string;
            error: string | null;
            /** Format: int64 */
            id: number;
            /** @enum {string} */
            kind: "QUANTITY" | "CATEGORY" | "WORKOUT" | "CORRELATION" | "ECG" | "AUDIOGRAM" | "STATE_OF_MIND" | "SCORED_ASSESSMENT";
            metadata: Record<string, never> | null;
            /** Format: uuid */
            sampleUuid: string | null;
            /** Format: date-time */
            startDate: string;
            /** @enum {string} */
            status: "PENDING" | "WRITTEN" | "FAILED";
            type: string;
            unit: string | null;
            /** Format: double */
            value: number | null;
        };
        Item: {
            /** Format: double */
            amount: number;
            /** Format: int64 */
            foodId?: number | null;
            /** Format: double */
            gramsPerUnit: number;
            /** Format: int64 */
            openFoodId?: number | null;
            unit: string;
        };
        LastEntry: {
            /** Format: double */
            amount: number;
        };
        MealDto: {
            type: string;
            typeLabel: string;
        };
        NotificationDto: {
            /** Format: date-time */
            createdAt: string;
            /** Format: int64 */
            id: number;
            link: string | null;
            linkLabel: string | null;
            message: string | null;
            /** Format: date-time */
            readAt: string | null;
            title: string;
            /** @enum {string} */
            type: "INFO" | "SUCCESS" | "WARNING" | "ERROR";
        };
        OverviewWorkoutDto: {
            /** Format: int32 */
            activityType: number;
            /** Format: double */
            averageHeartRate: number | null;
            /** Format: double */
            distance: number | null;
            /** Format: double */
            duration: number;
            /** Format: date-time */
            endDate: string;
            /** Format: double */
            energy: number | null;
            /** Format: uuid */
            id: string;
            /** Format: date-time */
            startDate: string;
        };
        PageFoodDto: {
            content: components["schemas"]["FoodDto"][];
            empty: boolean;
            first: boolean;
            last: boolean;
            /** Format: int32 */
            number: number;
            /** Format: int32 */
            numberOfElements: number;
            pageable: components["schemas"]["PageableObject"];
            /** Format: int32 */
            size: number;
            sort: components["schemas"]["SortObject"];
            /** Format: int64 */
            totalElements: number;
            /** Format: int32 */
            totalPages: number;
        };
        PageableObject: {
            /** Format: int64 */
            offset: number;
            /** Format: int32 */
            pageNumber: number;
            /** Format: int32 */
            pageSize: number;
            paged: boolean;
            sort: components["schemas"]["SortObject"];
            unpaged: boolean;
        };
        ProfileDto: {
            achievements: components["schemas"]["AchievementDto"][];
            /** Format: int32 */
            calorieGoal: number | null;
            days: components["schemas"]["ActivityDayDto"][];
            /** Format: date-time */
            memberSince: string | null;
            streak: components["schemas"]["StreakDto"];
            /** Format: date */
            today: string;
        };
        RecoveryDto: {
            /** Format: double */
            hrv: number | null;
            /** Format: double */
            hrvBaseline: number | null;
            /** Format: double */
            oxygenSaturation: number | null;
            /** Format: double */
            respiratoryRate: number | null;
            /** Format: double */
            respiratoryRateBaseline: number | null;
            /** Format: double */
            restingHeartRate: number | null;
            /** Format: double */
            restingHeartRateBaseline: number | null;
            /** Format: int32 */
            score: number | null;
            /** Format: double */
            wristTemperatureDeviation: number | null;
        };
        RunDetailDto: {
            points: components["schemas"]["RunPointDto"][];
            splits: components["schemas"]["RunSplitDto"][];
            summary: components["schemas"]["RunSummaryDto"];
        };
        RunPointDto: {
            /** Format: double */
            distance: number;
            /** Format: double */
            elevation: number | null;
            /** Format: double */
            heartRate: number | null;
            /** Format: double */
            latitude: number;
            /** Format: double */
            longitude: number;
            /** Format: double */
            pace: number | null;
            /** Format: double */
            time: number;
        };
        RunSplitDto: {
            /** Format: double */
            distance: number;
            /** Format: double */
            heartRate: number | null;
            /** Format: int32 */
            index: number;
            /** Format: double */
            time: number;
        };
        RunSummaryDto: {
            /** Format: double */
            averageHeartRate: number | null;
            bestEfforts: components["schemas"]["BestEffortDto"][];
            /** Format: double */
            cadence: number | null;
            /** Format: double */
            distance: number;
            /** Format: double */
            duration: number;
            /** Format: double */
            elevationGain: number | null;
            /** Format: date-time */
            endDate: string;
            /** Format: double */
            energy: number | null;
            /** Format: uuid */
            id: string;
            indoor: boolean;
            /** Format: double */
            maxHeartRate: number | null;
            name: string;
            sourceName: string | null;
            /** Format: date-time */
            startDate: string;
            thumbnail: number[][];
            timeZone: string;
            zoneSeconds: number[];
        };
        SaveUserSettingsDto: {
            /** Format: int32 */
            calorieGoal?: number | null;
        };
        SearchFoodResponseDto: {
            /** Format: int64 */
            id: number | null;
            name: string;
            /** Format: int64 */
            openFoodId: number | null;
        };
        SendFriendRequestDto: {
            userId: string;
        };
        SessionCardioDto: {
            done: boolean;
            /** Format: int32 */
            minutes: number;
        };
        SessionExerciseDto: {
            cardio: components["schemas"]["SessionCardioDto"] | null;
            /** Format: int64 */
            exerciseId: number | null;
            name: string;
            note: string | null;
            sets: components["schemas"]["SessionSetDto"][];
        };
        SessionSetDto: {
            done: boolean;
            /** Format: int32 */
            reps: number;
            /** Format: int32 */
            restSeconds: number | null;
            /** Format: int32 */
            rpe: number | null;
            /** Format: double */
            weightKg: number;
        };
        SetDto: {
            /** Format: int32 */
            order: number;
            /** Format: int32 */
            reps: number;
            /** Format: int32 */
            restSeconds: number | null;
            /** Format: int32 */
            rpe: number | null;
            /** Format: double */
            weightKg: number;
        };
        ShareWorkoutDto: {
            userIds: string[];
        };
        SleepDto: {
            /** Format: double */
            asleep: number;
            /** Format: double */
            awake: number;
            /** Format: double */
            core: number;
            /** Format: double */
            deep: number;
            /** Format: date-time */
            end: string;
            /** Format: double */
            inBed: number;
            /** Format: double */
            need: number;
            /** Format: double */
            oxygenSaturation: number | null;
            /** Format: double */
            rem: number;
            /** Format: double */
            respiratoryRate: number | null;
            /** Format: int32 */
            score: number;
            stages: components["schemas"]["SleepStageDto"][];
            /** Format: date-time */
            start: string;
        };
        SleepStageDto: {
            /** Format: date-time */
            end: string;
            stage: string;
            /** Format: date-time */
            start: string;
        };
        SliceSearchFoodResponseDto: {
            content: components["schemas"]["SearchFoodResponseDto"][];
            empty: boolean;
            first: boolean;
            last: boolean;
            /** Format: int32 */
            number: number;
            /** Format: int32 */
            numberOfElements: number;
            pageable: components["schemas"]["PageableObject"];
            /** Format: int32 */
            size: number;
            sort: components["schemas"]["SortObject"];
        };
        SortObject: {
            empty: boolean;
            sorted: boolean;
            unsorted: boolean;
        };
        StrainDto: {
            /** Format: double */
            averageHeartRate: number | null;
            /** Format: double */
            load: number;
            /** Format: double */
            maxHeartRate: number;
            /** Format: double */
            peakHeartRate: number | null;
            /** Format: int32 */
            score: number;
            zoneSeconds: number[];
        };
        StreakDto: {
            /** Format: int32 */
            current: number;
            /** Format: int32 */
            record: number;
            today: boolean;
        };
        StrengthItemDto: Omit<components["schemas"]["ExerciseItem"], "type"> & {
            exercise: components["schemas"]["ExerciseDto"];
            sets: components["schemas"]["SetDto"][];
        } & {
            /**
             * @description discriminator enum property added by openapi-typescript
             * @enum {string}
             */
            type: "STRENGTH_EXERCISE";
        };
        TrackedFoodDto: {
            /** Format: double */
            amount: number;
            /** Format: date-time */
            createDate: string;
            food: components["schemas"]["FoodDto"];
            /** Format: int64 */
            id: number;
            meal: components["schemas"]["MealDto"];
            portion: components["schemas"]["FoodPortionDto"] | null;
        };
        TrainingDto: {
            cheered: boolean;
            name: string | null;
            /** Format: int64 */
            sessionId: number;
            /** Format: date-time */
            startedAt: string;
        };
        UnreadCountDto: {
            /** Format: int64 */
            count: number;
        };
        UpdateCalendarEventDto: {
            /** Format: date-time */
            endDate: string;
            /** Format: int64 */
            id?: number | null;
            inviteeIds?: string[] | null;
            /** Format: date-time */
            occurrenceStartDate?: string | null;
            /** @enum {string|null} */
            recurrence?: "DAILY" | "WEEKLY" | "MONTHLY" | null;
            /** Format: date-time */
            recurrenceEnd?: string | null;
            /** Format: date-time */
            startDate: string;
            title: string;
            /** Format: int64 */
            workoutId?: number | null;
        };
        UpdateFoodDto: {
            blsCode?: string | null;
            /** Format: double */
            carbohydrates?: number | null;
            /** Format: double */
            fat?: number | null;
            /** Format: double */
            fiber?: number | null;
            /** Format: int64 */
            id?: number | null;
            /** Format: double */
            kcal?: number | null;
            name: string;
            portions?: components["schemas"]["UpdateFoodPortionDto"][] | null;
            /** Format: double */
            protein?: number | null;
            /** Format: double */
            water?: number | null;
        };
        UpdateFoodPortionDto: {
            /** Format: double */
            grams: number;
            /** Format: int64 */
            id?: number | null;
            isDefault?: boolean | null;
            label: string;
        };
        UpdateWorkoutSessionDto: {
            exercises: components["schemas"]["SessionExerciseDto"][];
            name: string;
        };
        UploadHealthSamplesDto: {
            /** Format: uuid */
            fullSyncId?: string | null;
            samples: components["schemas"]["HealthSampleDto"][];
        };
        UserProfileDto: {
            email: string | null;
            id: string;
            image: string | null;
            name: string | null;
        };
        UserSettingsDto: {
            /** Format: int32 */
            calorieGoal: number | null;
            userId: string;
        };
        VitalsDto: {
            /** Format: double */
            bodyMass: number | null;
            /** Format: double */
            bodyMassChange: number | null;
            /** Format: double */
            vo2Max: number | null;
        };
        WorkoutDto: {
            editable: boolean;
            /** Format: int64 */
            id: number;
            name: string;
            ownerName: string | null;
            sharedWith: components["schemas"]["CalendarUserDto"][];
            sharedWithMe: boolean;
            workoutExercises: (components["schemas"]["CardioItemDto"] | components["schemas"]["StrengthItemDto"])[];
        };
        WorkoutSessionDto: {
            /** Format: date-time */
            endedAt: string | null;
            exercises: components["schemas"]["SessionExerciseDto"][];
            /** Format: int64 */
            id: number;
            name: string;
            /** Format: date-time */
            startedAt: string;
            /** Format: int64 */
            workoutId: number | null;
        };
    };
    responses: never;
    parameters: never;
    requestBodies: never;
    headers: never;
    pathItems: never;
}
export type $defs = Record<string, never>;
export interface operations {
    calendarGetAllCalendarEvents: {
        parameters: {
            query?: {
                startDate?: string;
                endDate?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CalendarEventDto"][];
                };
            };
        };
    };
    calendarCreateCalendarEvent: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateCalendarEventDto"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CalendarEventDto"];
                };
            };
        };
    };
    externalCalendarGetCalendars: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ExternalCalendarDto"][];
                };
            };
        };
    };
    externalCalendarAddCalendar: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateExternalCalendarDto"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ExternalCalendarDto"];
                };
            };
        };
    };
    externalCalendarConnectAppleAccount: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ConnectAppleAccountDto"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ExternalCalendarDto"][];
                };
            };
        };
    };
    externalCalendarDisconnectAppleAccount: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                accountId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    externalCalendarGetEvents: {
        parameters: {
            query: {
                startDate: string;
                endDate: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ExternalCalendarEventDto"][];
                };
            };
        };
    };
    externalCalendarSyncEvents: {
        parameters: {
            query: {
                startDate: string;
                endDate: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ExternalCalendarEventDto"][];
                };
            };
        };
    };
    externalCalendarDeleteCalendar: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    calendarUpdateCalendarEvent: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["UpdateCalendarEventDto"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CalendarEventDto"];
                };
            };
        };
    };
    calendarDeleteCalendarEvent: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    foodGetAll: {
        parameters: {
            query?: {
                query?: string;
                /** @description Zero-based page index (0..N) */
                page?: number;
                /** @description The size of the page to be returned */
                size?: number;
                /** @description Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported. */
                sort?: string[];
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["PageFoodDto"];
                };
            };
        };
    };
    foodCreate: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateFoodDto"];
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    foodGetByBarcode: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                code: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["FoodDto"];
                };
            };
        };
    };
    foodExtract: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ExtractFoodRequestDto"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ExtractedFoodDto"][];
                };
            };
        };
    };
    foodCreateOpenFood: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateOpenFoodDto"];
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    foodGetOpenFood: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                openFoodId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["FoodWithPortionsDto"];
                };
            };
        };
    };
    foodImportOpenFood: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                openFoodId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["FoodWithPortionsDto"];
                };
            };
        };
    };
    foodTrackOpenFood: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                openFoodId: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateFoodUserDto"];
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    foodSearch: {
        parameters: {
            query: {
                /** @description Zero-based page index (0..N) */
                page?: number;
                /** @description The size of the page to be returned */
                size?: number;
                /** @description Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported. */
                sort?: string[];
                name: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SliceSearchFoodResponseDto"];
                };
            };
        };
    };
    foodUpdateFood: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["UpdateFoodDto"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["FoodDto"];
                };
            };
        };
    };
    foodAddFoodPortions: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateFoodPortionDto"];
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    foodTrackFoodForUser: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateFoodUserDto"];
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    userSearchUsers: {
        parameters: {
            query: {
                query: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CalendarUserDto"][];
                };
            };
        };
    };
    userGetProfile: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["UserProfileDto"];
                };
            };
        };
    };
    foodImageGetAll: {
        parameters: {
            query?: {
                limit?: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["FoodImageAnalysisDto"][];
                };
            };
        };
    };
    foodImageUpload: {
        parameters: {
            query: {
                meal: string;
                description?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "multipart/form-data": {
                    /** Format: binary */
                    image: string;
                };
            };
        };
        responses: {
            /** @description Accepted */
            202: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["FoodImageAnalysisDto"];
                };
            };
        };
    };
    foodImageUsage: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["FoodImageUsageDto"];
                };
            };
        };
    };
    foodImageGet: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["FoodImageAnalysisDto"];
                };
            };
        };
    };
    foodImageAccept: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["AcceptFoodImageDto"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["FoodImageAnalysisDto"];
                };
            };
        };
    };
    foodImageGetImage: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "image/*": string;
                };
            };
        };
    };
    foodImageReject: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["FoodImageAnalysisDto"];
                };
            };
        };
    };
    userGetLastTracked: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                foodId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["FoodWithLastEntryAndPortionsDto"];
                };
            };
        };
    };
    friendGetFriends: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["FriendsDto"];
                };
            };
        };
    };
    friendSendRequest: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["SendFriendRequestDto"];
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    friendAcceptRequest: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    friendDeleteRequest: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    friendRemoveFriend: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                friendId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    friendCheer: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                friendId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    friendGetProfile: {
        parameters: {
            query?: {
                zone?: string;
            };
            header?: never;
            path: {
                friendId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["FriendProfileDto"];
                };
            };
        };
    };
    healthGetCharacteristics: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": Record<string, never>;
                };
            };
        };
    };
    healthSaveCharacteristics: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": Record<string, never>;
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    healthOverviewGetOverview: {
        parameters: {
            query?: {
                date?: string;
                zone?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["HealthOverviewDto"];
                };
            };
        };
    };
    healthOverviewGetEnergy: {
        parameters: {
            query?: {
                date?: string;
                zone?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["EnergyDto"];
                };
            };
        };
    };
    healthGetSamples: {
        parameters: {
            query?: {
                type?: string;
                from?: string;
                to?: string;
                page?: number;
                size?: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["HealthSampleDto"][];
                };
            };
        };
    };
    healthUploadSamples: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["UploadHealthSamplesDto"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["HealthSyncResultDto"];
                };
            };
        };
    };
    healthDeleteSamples: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["DeleteHealthSamplesDto"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["HealthSyncResultDto"];
                };
            };
        };
    };
    healthCompleteFullSync: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CompleteFullSyncDto"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["HealthSyncResultDto"];
                };
            };
        };
    };
    healthGetSummary: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["HealthTypeSummaryDto"][];
                };
            };
        };
    };
    healthGetWriteRequests: {
        parameters: {
            query?: {
                status?: "PENDING" | "WRITTEN" | "FAILED";
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["HealthWriteRequestDto"][];
                };
            };
        };
    };
    healthCreateWriteRequest: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateHealthWriteRequestDto"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["HealthWriteRequestDto"];
                };
            };
        };
    };
    healthAcknowledgeWriteRequest: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["AckHealthWriteRequestDto"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["HealthWriteRequestDto"];
                };
            };
        };
    };
    notificationGetNotifications: {
        parameters: {
            query?: {
                unreadOnly?: boolean;
                limit?: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["NotificationDto"][];
                };
            };
        };
    };
    notificationCreateNotification: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateNotificationDto"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["NotificationDto"];
                };
            };
        };
    };
    notificationMarkAllRead: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    notificationGetUnreadCount: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["UnreadCountDto"];
                };
            };
        };
    };
    notificationDeleteNotification: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    notificationMarkRead: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["NotificationDto"];
                };
            };
        };
    };
    notificationMarkUnread: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["NotificationDto"];
                };
            };
        };
    };
    userProfileGetProfile: {
        parameters: {
            query?: {
                zone?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ProfileDto"];
                };
            };
        };
    };
    runGetRuns: {
        parameters: {
            query?: {
                from?: string;
                to?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["RunSummaryDto"][];
                };
            };
        };
    };
    runGetRun: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["RunDetailDto"];
                };
            };
        };
    };
    userSettingsGetSettings: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["UserSettingsDto"];
                };
            };
        };
    };
    userSettingsUpdateSettings: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["SaveUserSettingsDto"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["UserSettingsDto"];
                };
            };
        };
    };
    userSettingsCreateSettings: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["SaveUserSettingsDto"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["UserSettingsDto"];
                };
            };
        };
    };
    userSettingsDeleteSettings: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    userGetTrackedFoodForUser: {
        parameters: {
            query?: {
                date?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["TrackedFoodDto"][];
                };
            };
        };
    };
    userGetTrackedFoodById: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                trackedFoodId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["TrackedFoodDto"];
                };
            };
        };
    };
    userUpdateTrackedFoodById: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                trackedFoodId: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateFoodUserDto"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    userDeleteTrackedFoodById: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                trackedFoodId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    workoutSessionGetSessions: {
        parameters: {
            query?: {
                from?: string;
                to?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["WorkoutSessionDto"][];
                };
            };
        };
    };
    workoutSessionStart: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateWorkoutSessionDto"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["WorkoutSessionDto"];
                };
            };
        };
    };
    workoutSessionGetActive: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["WorkoutSessionDto"];
                };
            };
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    workoutSessionGetSession: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["WorkoutSessionDto"];
                };
            };
        };
    };
    workoutSessionUpdate: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["UpdateWorkoutSessionDto"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["WorkoutSessionDto"];
                };
            };
        };
    };
    workoutSessionDelete: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    workoutSessionFinish: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: {
            content: {
                "application/json": components["schemas"]["UpdateWorkoutSessionDto"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["WorkoutSessionDto"];
                };
            };
        };
    };
    workoutGetAllWorkouts: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["WorkoutDto"][];
                };
            };
        };
    };
    workoutCreateWorkout: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateWorkoutDto"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["WorkoutDto"];
                };
            };
        };
    };
    workoutGetAllExercises: {
        parameters: {
            query?: {
                name?: string;
                type?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ExerciseDto"][];
                };
            };
        };
    };
    workoutCreateExercise: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateExerciseDto"];
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    workoutUpdateExerciseById: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ExerciseDto"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ExerciseDto"];
                };
            };
        };
    };
    workoutDeleteExercise: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    workoutGetWorkoutById: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["WorkoutDto"];
                };
            };
        };
    };
    workoutUpdateWorkout: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateWorkoutDto"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["WorkoutDto"];
                };
            };
        };
    };
    workoutDeleteWorkout: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    workoutShareWorkout: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ShareWorkoutDto"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["WorkoutDto"];
                };
            };
        };
    };
    workoutLeaveWorkout: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
}

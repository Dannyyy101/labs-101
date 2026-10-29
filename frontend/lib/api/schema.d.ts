export interface paths {
    "/api/calendar": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["getAllCalendarEvents"];
        put?: never;
        post: operations["createCalendarEvent"];
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
        get: operations["getCalendars"];
        put?: never;
        post: operations["addCalendar"];
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
        post: operations["connectAppleAccount"];
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
        delete: operations["disconnectAppleAccount"];
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
        get: operations["getEvents"];
        put?: never;
        post?: never;
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
        delete: operations["deleteCalendar"];
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
        put: operations["updateCalendarEvent"];
        post?: never;
        delete: operations["deleteCalendarEvent"];
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
        get: operations["getAll_1"];
        put?: never;
        post: operations["create"];
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
        get: operations["getByBarcode"];
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
        post: operations["extract"];
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
        post: operations["createOpenFood"];
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
        get: operations["getOpenFood"];
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
        post: operations["importOpenFood"];
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
        post: operations["trackOpenFood"];
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
        get: operations["search"];
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
        put: operations["updateFood"];
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
        post: operations["addFoodPortions"];
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
        post: operations["trackFoodForUser"];
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
        get: operations["searchUsers"];
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
        get: operations["getProfile"];
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
        get: operations["getAll"];
        put?: never;
        post: operations["upload"];
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
        get: operations["usage"];
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
        get: operations["get"];
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
        post: operations["accept"];
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
        get: operations["getImage"];
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
        post: operations["reject"];
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
        get: operations["getLastTracked"];
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
        get: operations["getFriends"];
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
        post: operations["sendRequest"];
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
        put: operations["acceptRequest"];
        post?: never;
        delete: operations["deleteRequest"];
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
        delete: operations["removeFriend"];
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
        post: operations["cheer"];
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
        get: operations["getProfile_2"];
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
        get: operations["getCharacteristics"];
        put: operations["saveCharacteristics"];
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
        get: operations["getOverview"];
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
        get: operations["getEnergy"];
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
        get: operations["getSamples"];
        put?: never;
        post: operations["uploadSamples"];
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
        post: operations["deleteSamples"];
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
        post: operations["completeFullSync"];
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
        get: operations["getSummary"];
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
        get: operations["getWriteRequests"];
        put?: never;
        post: operations["createWriteRequest"];
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
        post: operations["acknowledgeWriteRequest"];
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
        get: operations["getNotifications"];
        put?: never;
        post: operations["createNotification"];
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
        put: operations["markAllRead"];
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
        get: operations["getUnreadCount"];
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
        delete: operations["deleteNotification"];
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
        put: operations["markRead"];
        post?: never;
        delete: operations["markUnread"];
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
        get: operations["getProfile_1"];
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
        get: operations["getRuns"];
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
        get: operations["getRun"];
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
        get: operations["getSettings"];
        put: operations["updateSettings"];
        post: operations["createSettings"];
        delete: operations["deleteSettings"];
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
        get: operations["getTrackedFoodForUser"];
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
        get: operations["getTrackedFoodById"];
        put: operations["updateTrackedFoodById"];
        post?: never;
        delete: operations["deleteTrackedFoodById"];
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
        get: operations["getSessions"];
        put?: never;
        post: operations["start"];
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
        get: operations["getActive"];
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
        get: operations["getSession"];
        put: operations["update"];
        post?: never;
        delete: operations["delete"];
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
        post: operations["finish"];
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
        get: operations["getAllWorkouts"];
        put?: never;
        post: operations["createWorkout"];
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
        get: operations["getAllExercises"];
        put?: never;
        post: operations["createExercise"];
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
        put: operations["updateExerciseById"];
        post?: never;
        delete: operations["deleteExercise"];
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
        get: operations["getWorkoutById"];
        put: operations["updateWorkout"];
        post?: never;
        delete: operations["deleteWorkout"];
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
        put: operations["shareWorkout"];
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
        delete: operations["leaveWorkout"];
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
    getAllCalendarEvents: {
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
    createCalendarEvent: {
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
    getCalendars: {
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
    addCalendar: {
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
    connectAppleAccount: {
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
    disconnectAppleAccount: {
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
    getEvents: {
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
    deleteCalendar: {
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
    updateCalendarEvent: {
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
    deleteCalendarEvent: {
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
    getAll_1: {
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
    create: {
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
    getByBarcode: {
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
    extract: {
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
    createOpenFood: {
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
    getOpenFood: {
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
    importOpenFood: {
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
    trackOpenFood: {
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
    search: {
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
    updateFood: {
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
    addFoodPortions: {
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
    trackFoodForUser: {
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
    searchUsers: {
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
    getProfile: {
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
    getAll: {
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
    upload: {
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
    usage: {
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
    get: {
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
    accept: {
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
    getImage: {
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
    reject: {
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
    getLastTracked: {
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
    getFriends: {
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
    sendRequest: {
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
    acceptRequest: {
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
    deleteRequest: {
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
    removeFriend: {
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
    cheer: {
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
    getProfile_2: {
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
    getCharacteristics: {
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
    saveCharacteristics: {
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
    getOverview: {
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
    getEnergy: {
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
    getSamples: {
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
    uploadSamples: {
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
    deleteSamples: {
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
    completeFullSync: {
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
    getSummary: {
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
    getWriteRequests: {
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
    createWriteRequest: {
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
    acknowledgeWriteRequest: {
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
    getNotifications: {
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
    createNotification: {
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
    markAllRead: {
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
    getUnreadCount: {
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
    deleteNotification: {
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
    markRead: {
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
    markUnread: {
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
    getProfile_1: {
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
    getRuns: {
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
    getRun: {
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
    getSettings: {
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
    updateSettings: {
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
    createSettings: {
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
    deleteSettings: {
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
    getTrackedFoodForUser: {
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
    getTrackedFoodById: {
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
    updateTrackedFoodById: {
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
    deleteTrackedFoodById: {
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
    getSessions: {
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
    start: {
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
    getActive: {
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
    getSession: {
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
    update: {
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
    delete: {
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
    finish: {
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
    getAllWorkouts: {
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
    createWorkout: {
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
    getAllExercises: {
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
    createExercise: {
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
    updateExerciseById: {
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
    deleteExercise: {
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
    getWorkoutById: {
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
    updateWorkout: {
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
    deleteWorkout: {
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
    shareWorkout: {
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
    leaveWorkout: {
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

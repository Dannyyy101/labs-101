package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.labs_101.backend.annotation.NoAutoTest;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.repositories.CalendarRepository;
import com.labs_101.backend.repositories.FoodRepository;
import com.labs_101.backend.repositories.OpenFoodRepository;
import com.labs_101.backend.repositories.WorkoutRepository;
import com.labs_101.backend.services.CalendarService;
import com.labs_101.backend.services.FoodService;
import com.labs_101.backend.services.WorkoutService;

@ExtendWith(MockitoExtension.class)
class BackendApplicationTests {

	@Mock
	private FoodRepository foodRepository;
	@Mock
	private OpenFoodRepository openFoodRepository;
	@Mock
	private CalendarRepository calendarRepository;
	@Mock
	private WorkoutRepository workoutRepository;

	@InjectMocks
	private FoodService foodService;

	@InjectMocks
	private CalendarService calendarService;

	@InjectMocks
	private WorkoutService workoutService;

	private static final Map<Class<?>, Object> DEFAULT_VALUES = Map.of(
			String.class, "-1",
			Long.class, 1L);

	@Test
	void testServices() {
		testAllGetByIdMethods(foodService);
		testAllGetByIdMethods(calendarService);
		testAllGetByIdMethods(workoutService);
	}

	void testAllGetByIdMethods(Object service) {
		for (Method m : service.getClass().getMethods()) {
			boolean skipped = false;
			if (m.getName().contains("getBy")) {
				for (Annotation annotation : m.getAnnotations()) {
					if (annotation instanceof NoAutoTest) {
						skipped = true;
						break;
					}
				}
				if (skipped) {
					break;
				}

				List<Object> objects = new ArrayList<>();
				for (Parameter p : m.getParameters()) {
					switch (p.getType().getSimpleName()) {
						case "String" -> objects.add("-1");
						case "Long" -> objects.add(1L);
						default -> objects.add(null);
					}
				}
				Exception exception = assertThrows(InvocationTargetException.class,
						() -> m.invoke(foodService, objects.toArray()));
				assertTrue(exception.getCause() instanceof NotFoundException);
			}
		}

	}

}

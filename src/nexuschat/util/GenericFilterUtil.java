package nexuschat.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Unit II Syllabus:
 * - Introduction to Generics: Generic methods
 * - A generic method defines its own type parameter(s) before the return type (<T>).
 * - Enables writing type-safe, reusable algorithms across different data types.
 */
public final class GenericFilterUtil {

    private GenericFilterUtil() {
        // Prevent instantiation
    }

    /**
     * Generic method: Filters any generic List<T> matching a given Predicate<T>
     * 
     * @param <T> The generic type parameter
     * @param inputList Source list of elements of type T
     * @param predicate Condition to evaluate for each element
     * @return Filtered list containing only elements that satisfy the predicate
     */
    public static <T> List<T> filter(List<T> inputList, Predicate<T> predicate) {
        List<T> result = new ArrayList<>();
        if (inputList == null || predicate == null) {
            return result;
        }

        // Unit I: Control statements - enhanced for loop
        for (T item : inputList) {
            if (predicate.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Generic method: Finds the first matching element in a list of type T
     */
    public static <T> T findFirst(List<T> inputList, Predicate<T> predicate) {
        if (inputList == null || predicate == null) {
            return null;
        }
        for (T item : inputList) {
            if (predicate.test(item)) {
                return item;
            }
        }
        return null;
    }
}

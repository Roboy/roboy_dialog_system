package roboy.context;

/**
 * Interface for an enum which lists Context values and valueHistories.
 * Methods enable retrieving values over generic methods with AttributeManager.
 */
public interface ContextValueInterface {
    Class getClassType();

    Class getReturnType();
}
package anpilot.client.features.setting

import anpilot.client.bootstrap.ANServiceRegistry
import anpilot.client.features.event.impl.ANSettingEvent
import anpilot.client.features.setting.impl.Bind
import anpilot.client.features.setting.impl.EnumConverter
import anpilot.client.features.setting.impl.ItemSelectSetting
import java.util.function.Predicate

class ANSetting<T>(
    val name: String,
    val defaultValue: T,
    private var min: T? = null,
    private var max: T? = null,
    private val visibility: Predicate<T>? = null
) {
    var group: ANSetting<*>? = null
    private var plannedValue: T = defaultValue
    var module: Any? = null

    var value: T = defaultValue
        private set

    fun setValue(value: T) {
        setValueSilent(value)
        if (ANServiceRegistry.isInitialized) {
            ANServiceRegistry.runtime.eventBus.post(ANSettingEvent(this))
        }
    }

    fun setValueSilent(value: T) {
        plannedValue = restrict(value)
        this.value = plannedValue
    }

    fun setPlannedValue(value: T) {
        plannedValue = value
        this.value = value
    }

    fun getPlannedValue(): T = plannedValue

    fun isVisible(): Boolean = visibility?.test(value) ?: true

    fun getMin(): T? = min

    fun setMin(min: T?) {
        this.min = min
        setValueSilent(value)
    }

    fun getMax(): T? = max

    fun setMax(max: T?) {
        this.max = max
        setValueSilent(value)
    }

    fun hasRestriction(): Boolean = min != null && max != null

    fun getPow2Value(): Float = when (val current = value) {
        is Float -> current * current
        is Int -> (current * current).toFloat()
        is Double -> (current * current).toFloat()
        else -> 0f
    }

    fun addToGroup(group: ANSetting<*>): ANSetting<T> {
        this.group = group
        return this
    }

    fun isValue(expected: T): Boolean = value == expected

    fun `is`(expected: T): Boolean = value == expected

    fun not(expected: T): Boolean = value != expected

    fun currentEnumName(): String = EnumConverter.getProperName(value as Enum<*>)

    fun getModes(): Array<String> = EnumConverter.getNames(value as Enum<*>)

    @Suppress("UNCHECKED_CAST")
    fun setEnum(mode: String) {
        val current = value as? Enum<*> ?: return
        val constants = current.javaClass.enumConstants
        val next = constants.firstOrNull { it.name.equals(mode, ignoreCase = true) } ?: return
        setValue(next as T)
    }

    @Suppress("UNCHECKED_CAST")
    fun increaseEnum() {
        setValue(EnumConverter.increaseEnum(value as Enum<*>) as T)
    }

    @Suppress("UNCHECKED_CAST")
    fun setEnumByNumber(id: Int) {
        setValue(EnumConverter.setEnumInt(value as Enum<*>, id) as T)
    }

    fun isFloatSetting(): Boolean = value is Float

    fun isFloat(): Boolean = value is Double

    fun isDoubleSetting(): Boolean = value is Double

    fun isEnumSetting(): Boolean = value is Enum<*>

    fun isBindSetting(): Boolean = value is Bind

    fun isStringSetting(): Boolean = value is String

    fun isItemSelectSetting(): Boolean = value is ItemSelectSetting

    @Suppress("UNCHECKED_CAST")
    private fun restrict(value: T): T {
        if (!hasRestriction() || value !is Number || min !is Number || max !is Number) {
            return value
        }

        val numericValue = value.toDouble()
        val minValue = (min as Number).toDouble()
        val maxValue = (max as Number).toDouble()
        
        return when {
            numericValue < minValue -> min as T
            numericValue > maxValue -> max as T
            else -> value
        }
    }
}

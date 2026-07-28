package anpilot.client.features.setting.impl

object EnumConverter {
    fun currentEnum(value: Enum<*>): Int = value.javaClass.enumConstants.indexOfFirst { it.name.equals(value.name, ignoreCase = true) }

    fun increaseEnum(value: Enum<*>): Enum<*> {
        val constants = value.javaClass.enumConstants
        val nextIndex = currentEnum(value) + 1
        return constants.getOrElse(nextIndex) { constants.first() }
    }

    fun setEnumInt(value: Enum<*>, id: Int): Enum<*> {
        val constants = value.javaClass.enumConstants
        return constants.getOrElse(id) { constants.first() }
    }

    fun getNames(value: Enum<*>): Array<String> = value.javaClass.enumConstants.map { it.name }.toTypedArray()

    fun getProperName(value: Enum<*>): String = value.name
}

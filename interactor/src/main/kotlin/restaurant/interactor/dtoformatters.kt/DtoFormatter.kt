package restaurant.interactor.dtoformatters.kt

abstract class DtoFormatter {
    protected fun appendTableLine(sb: StringBuilder, vararg columsDataAndLen: Pair<String, Int>) {
        sb.append("| ")
        for ((i, pair) in columsDataAndLen.withIndex()) {
            if (i != 0) {
                sb.append(" | ")
            }
            val (columnData, fullColumnLen) = pair
            appendStringWithFill(sb, columnData, fullColumnLen)
        }
        sb.append(" |\n")
    }

    protected fun makeSepLine(vararg columnLens: Int): String {
        var capacity: Int = "+-".length + "-+\n".length + "-+-".length * (columnLens.size - 1)
        for (columnLen in columnLens) {
            capacity += "-".length * columnLen
        }
        val sepLine = java.lang.StringBuilder(capacity)
        sepLine.append("+-")
        
        for ((i, columnLen) in columnLens.withIndex()) {
            if (i != 0) {
                sepLine.append("-+-")
            }
            sepLine.append("-".repeat(columnLen))
            
        }
        sepLine.append("-+\n")
        return sepLine.toString()
    }

    protected fun appendStringWithFill(sb: StringBuilder, columnData: String, fullColumnLen: Int) {
        sb.append(columnData)
        val freeAppendLen = fullColumnLen - columnData.length
        if (freeAppendLen > 0) {
            sb.append(CharArray(freeAppendLen) { ' ' })
        }
    }

    protected fun stringBuilderCapacityFormula(tableLineLength: Int, totalEntitiesSize: Int) =
        tableLineLength * (3 + totalEntitiesSize * 2)
}

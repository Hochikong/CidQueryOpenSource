package me.ckho

import javax.swing.table.AbstractTableModel

class SelectTableModule(
    channel: Int,
    var data: Array<Array<Any>> = arrayOf(arrayOf("CST empty", "empty", "0", "1970-01-01", true))
) :
    AbstractTableModel() {

    private val channels = mapOf(1 to "HotMarket", 2 to "HotItems", 3 to "HotShops", 4 to "HotBrands")

    private val columnNames = arrayOf("CST", "CID", "渠道数-${channels[channel]}", "日期", "选择")

    private var selectedDates: MutableList<String> = mutableListOf()

    override fun getRowCount(): Int {
        return data.size
    }

    override fun getColumnCount(): Int {
        return this.columnNames.size
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        return data[rowIndex][columnIndex]
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
        return columnIndex == 4
    }

    override fun getColumnName(column: Int): String {
        return columnNames[column]
    }

    override fun getColumnClass(columnIndex: Int): Class<*> {
        return getValueAt(0, columnIndex).javaClass
    }

    override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
        data[rowIndex][columnIndex] = aValue!!

        // update selected dates
        selectedDates = mutableListOf()
        for (i in data.indices) {
            if (data[i][4] == true) {
                selectedDates.add(data[i][3] as String)
            }
        }
    }

    fun getSelectedDates(): List<String> {
        return selectedDates.toList()
    }
}
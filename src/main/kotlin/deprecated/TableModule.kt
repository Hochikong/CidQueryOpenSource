package deprecated

import javax.swing.table.AbstractTableModel

class TableModule(
    channel: Int,
    private var data: Array<Array<String>> = arrayOf(arrayOf("empty", "0", "1970-01-01"))
) :
    AbstractTableModel() {

    private val channels = mapOf(1 to "HotMarket", 2 to "HotItems", 3 to "HotShops", 4 to "HotBrands")

    private val columnNames = arrayOf("CID", "渠道数-${channels[channel]}", "日期")

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
        return false
    }

    override fun getColumnName(column: Int): String {
        return columnNames[column]
    }
}
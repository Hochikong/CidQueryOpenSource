package me.ckho

import codegen.impMainScene
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.extension.ExtensionCallback
import java.awt.Color
import java.awt.event.ActionEvent
import java.awt.event.ItemEvent
import java.io.File
import java.io.FileOutputStream
import java.lang.management.ManagementFactory
import java.lang.management.OperatingSystemMXBean
import java.sql.ResultSet
import java.sql.Statement
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.ListSelectionModel
import kotlin.concurrent.thread
import kotlin.system.exitProcess


class DeveloperMainApp(options: Array<String>) : impMainScene(options, SelectTableModule(1)) {
    private val dbConfig = HikariConfig().apply {
        username = "<YOUR_DB_USER>"
        password = "<YOUR_PASSWORD>"
        jdbcUrl = "<JDBC_URL>"
        maximumPoolSize = 1
    }

    // control to use suffix to choose table, e.g, marketing_situation{_exportSuffix}
    private val suffixLimit = listOf("", "suffix1", "suffix2")
    private var exportSuffix = ""
    private var titleTemplate =
        "Cid Query Tool - v0.0.4_4 Developer Version with suffix - ${exportSuffix.ifBlank { "origin" }}"

    private lateinit var ds: HikariDataSource
    private var selection: Int = 1

    private lateinit var jdbi: Jdbi
    private var connected = false

    private lateinit var fileChooser: JFileChooser
    private var outputDir = "."

    private var hasCid = false

    private var rowSelectedDates: MutableList<String> = mutableListOf()

    // control user can delete or not
    private val enableDelete = true

    init {
        // default status
        this.ButtonDeleteData.isEnabled = false
        this.title = titleTemplate

        this.TableMain.showHorizontalLines = false
    }

    override fun impButtonCleanCidInputActionPerformed(evt: ActionEvent?) {
        this.TextFieldCidInput.text = ""
//        JOptionPane.showMessageDialog(this, "输入框已清空")
    }

    override fun impMenuItemExitActionPerformed(evt: ActionEvent?) {
        try {
            ds.close()
        } catch (e: UninitializedPropertyAccessException) {
        }
        exitProcess(0)
    }

    override fun impMenuItemConnActionPerformed(evt: ActionEvent?) {
        try {
            this.ds = HikariDataSource(this.dbConfig)
            this.jdbi = Jdbi.create(this.ds)
            this.jdbi.installPlugins()
            this.LabelStatusValue.foreground = Color(0, 204, 153)
            this.LabelStatusValue.text = "Connected"
            this.connected = true
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, e, "ERROR:无法连接到数据库，请联系开发者", JOptionPane.ERROR_MESSAGE)
        }
    }

    override fun impComboBoxModulesItemStateChanged(evt: ItemEvent) {
        if (evt.stateChange == ItemEvent.SELECTED) {
            val item = evt.item as String
            this.selection = when (item) {
                "HotMarket" -> 1
                "HotItems" -> 2
                "HotShops" -> 3
                "HotBrands" -> 4
                else -> throw IllegalStateException("不被支持的选项")
            }
        }
    }

    override fun impMenuItemAboutActionPerformed(evt: ActionEvent?) {
        val system: OperatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean()
        val runtime = ManagementFactory.getRuntimeMXBean()
        val info = "OS: ${system.name} \nArch: ${system.arch} " +
                "\nVM: ${runtime.vmName} \nVendor: ${runtime.vmVendor}" +
                "\nSpec: ${runtime.specVersion} \nVersion: ${runtime.vmVersion}"
        JOptionPane.showMessageDialog(
            this,
            "CidQuery OpenSource Version \nckhoidea@hotmail.com \n\n$info",
            "About",
            JOptionPane.INFORMATION_MESSAGE
        )
    }

    override fun impButtonSearchActionPerformed(evt: ActionEvent?) {
        if (!this.connected) {
            JOptionPane.showMessageDialog(this, "你尚未连接数据，无法查询", "查询失败", JOptionPane.WARNING_MESSAGE)
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                val cid = TextFieldCidInput.text.trim()
                if (cid.trim().isBlank()) {
                    JOptionPane.showMessageDialog(
                        this@DeveloperMainApp,
                        "无CID输入，不执行查询/导出",
                        "查询失败",
                        JOptionPane.WARNING_MESSAGE
                    )
                    TextFieldCidInput.text = ""
                } else {
                    ProgressBarDoing.isIndeterminate = true
                    LabelDoing.text = "正在执行查询"
                    val start = System.nanoTime()

                    try {
                        val qr = when (selection) {
                            1 -> jdbi.withExtension(StatisticsDAO::class.java, ExtensionCallback {
                                if (exportSuffix.isNotBlank()) {
                                    it.queryWM(cid.toLong(), "marketing_situation_${exportSuffix}")
                                } else {
                                    it.queryWM(cid.toLong())
                                }
                            })

                            2 -> jdbi.withExtension(StatisticsDAO::class.java, ExtensionCallback {
                                if (exportSuffix.isNotBlank()) {
                                    it.queryHI(cid.toLong(), "hotitem_ranking_${exportSuffix}")
                                } else {
                                    it.queryHI(cid.toLong())
                                }
                            })

                            3 -> jdbi.withExtension(StatisticsDAO::class.java, ExtensionCallback {
                                if (exportSuffix.isNotBlank()) {
                                    it.queryHS(cid.toLong(), "hotshop_ranking_${exportSuffix}")
                                } else {
                                    it.queryHS(cid.toLong())
                                }
                            })

                            4 -> jdbi.withExtension(StatisticsDAO::class.java, ExtensionCallback {
                                if (exportSuffix.isNotBlank()) {
                                    it.queryPR(cid.toLong(), "property_situation_${exportSuffix}")
                                } else {
                                    it.queryPR(cid.toLong())
                                }
                            })

                            else -> throw IllegalStateException("不被支持的选项")
                        }

                        val arrayQr = qr.map { it.toDVArray() }.toTypedArray()

                        if (arrayQr.isNotEmpty()) {
                            TableMain.model = buildModel(arrayQr)
                            TableMain.autoCreateRowSorter = true
                            TableMain.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
                            if (selection == 1) {
                                TableMain.cellSelectionEnabled = true
                            }
                            hasCid = true

                            if (enableDelete) {
                                this@DeveloperMainApp.ButtonDeleteData.isEnabled = true
                            }

                        } else {
                            TableMain.model = buildModel(arrayOf(arrayOf("EMPTY", "EMPTY", "0", "EMPTY", false)))
                            TableMain.autoCreateRowSorter = true
                            hasCid = false

                            this@DeveloperMainApp.ButtonDeleteData.isEnabled = false
                        }
                    } catch (e: NumberFormatException) {
                        JOptionPane.showMessageDialog(
                            this@DeveloperMainApp,
                            "CID格式错误，无法查询",
                            "查询失败",
                            JOptionPane.WARNING_MESSAGE
                        )
                    }


                    val end = System.nanoTime()

                    ProgressBarDoing.isIndeterminate = false
                    LabelDoing.text = "查询完成, 耗时${TimeUnit.NANOSECONDS.toMillis(end - start)}毫秒"
                }
            }
        }
    }

    private fun buildModel(data: Array<Array<Any>>): SelectTableModule {
        return SelectTableModule(this.selection, data)
    }

    override fun impButtonExportDataActionPerformed(evt: ActionEvent?) {
        if (!this.connected) {
            JOptionPane.showMessageDialog(this, "你尚未连接数据，无法查询", "查询失败", JOptionPane.WARNING_MESSAGE)
        } else if (!this.hasCid) {
            JOptionPane.showMessageDialog(this, "CID不存在，无法导出", "查询失败", JOptionPane.WARNING_MESSAGE)
        } else {
            fileChooser = JFileChooser().apply {
                dialogTitle = "选择导出目录"
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                isAcceptAllFileFilterUsed = false
            }

            if (this.outputDir.length > 1) {
                fileChooser.currentDirectory = File(outputDir)
            } else {
                fileChooser.currentDirectory = File(".")
            }

            // open dialog
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                this.outputDir = fileChooser.selectedFile.toString()
                // export
                val cid = TextFieldCidInput.text.trim()

                CoroutineScope(Dispatchers.IO).launch {
                    if (cid.trim().isBlank()) {
                        JOptionPane.showMessageDialog(
                            this@DeveloperMainApp,
                            "无CID输入，不执行查询/导出",
                            "查询失败",
                            JOptionPane.WARNING_MESSAGE
                        )
                        TextFieldCidInput.text = ""
                    } else {
                        ProgressBarDoing.isIndeterminate = true
                        LabelDoing.text = "正在执行数据导出"
                        val start = System.nanoTime()

                        val dates = (TableMain.model as SelectTableModule).getSelectedDates()

                        // do
                        val conn = ds.connection
                        val stmt = conn.createStatement()
                        val rs = fetchData(dates, stmt, cid)

                        val mt = rs.metaData
                        val columnNames = (1..mt.columnCount).map { mt.getColumnName(it) }.toTypedArray()
                        var rowData: MutableList<Array<String>> = mutableListOf(columnNames)

                        // scan data
                        while (rs.next()) {
                            val tmp = mutableListOf<String>()

                            for (key in columnNames) {
                                val t = rs.getString(key)
                                if (t != null) {
                                    when (val nt = dataCheck(key, t, rs)) {
                                        "EMPTY" -> tmp.add(t)
                                        else -> tmp.add(nt)
                                    }
                                } else {
                                    tmp.add("")
                                }
                            }

                            rowData.add(tmp.toTypedArray())
                        }

                        // generate excel
//                        val workbook = XSSFWorkbook()
                        val workbook = SXSSFWorkbook(100)
                        val sheet = workbook.createSheet("Export")

                        for (i in 0 until rowData.size) {
                            val row = sheet.createRow(i)
                            for (j in columnNames.indices) {
                                val cell = row.createCell(j)
                                cell.setCellValue(rowData[i][j])
                            }
                        }

                        thread(start = true) {
                            FileOutputStream(
                                File("$outputDir/${TextFieldCidInput.text}-${options[selection - 1]}.xlsx")
                            ).use {
                                workbook.write(it)
                            }

                            workbook.close()
                        }

                        stmt.close()
                        conn.close()
                        // do end

                        val end = System.nanoTime()
                        ProgressBarDoing.isIndeterminate = false
                        LabelDoing.text = "查询完成, 耗时${TimeUnit.NANOSECONDS.toMillis(end - start)}毫秒"

                        rowData = mutableListOf(arrayOf(""))
                    }
                }
            } else {
                LabelDoing.text = "取消文件导出"
            }
        }
    }

    private fun fetchData(dates: List<String>, stmt: Statement, cid: String?): ResultSet {
        val iN = generateInClause(dates)

        return when (selection) {
            1 -> {
                val sql = """
                    SELECT *
                    FROM marketing_situation${if (exportSuffix.isBlank()) "" else "_${exportSuffix}"}
                    WHERE cid = $cid;
                    """.trimIndent()

                if (dates.isEmpty()) {
                    stmt.executeQuery(
                        sql
                    )
                } else {
                    val sql = """
                    SELECT *
                    FROM marketing_situation${if (exportSuffix.isBlank()) "" else "_${exportSuffix}"}
                    WHERE cid = $cid AND `日期` in $iN ORDER BY `日期` DESC;
                    """.trimIndent()

                    stmt.executeQuery(
                        sql
                    )
                }
            }

            2 -> {
                if (dates.isEmpty()) {
                    val sql = """
                        SELECT *
                        FROM hotitem_ranking${if (exportSuffix.isBlank()) "" else "_${exportSuffix}"}
                        WHERE cid = $cid;
                        """.trimIndent()

                    stmt.executeQuery(
                        sql
                    )
                } else {
                    val sql = """
                    SELECT *
                    FROM hotitem_ranking${if (exportSuffix.isBlank()) "" else "_${exportSuffix}"}
                    WHERE cid = $cid AND date in $iN ORDER BY date DESC;
                    """.trimIndent()

                    stmt.executeQuery(
                        sql
                    )
                }
            }

            3 -> {
                if (dates.isEmpty()) {
                    val sql = """
                        SELECT *
                        FROM hotshop_ranking${if (exportSuffix.isBlank()) "" else "_${exportSuffix}"}
                        WHERE cid = $cid;
                        """.trimIndent()
                    stmt.executeQuery(
                        sql
                    )
                } else {
                    val sql = """
                    SELECT *
                    FROM hotshop_ranking${if (exportSuffix.isBlank()) "" else "_${exportSuffix}"}
                    WHERE cid = $cid AND date in $iN ORDER BY date DESC;
                    """.trimIndent()

                    stmt.executeQuery(
                        sql
                    )
                }
            }

            4 -> {
                if (dates.isEmpty()) {
                    val sql = """
                        SELECT *
                        FROM property_situation${if (exportSuffix.isBlank()) "" else "_${exportSuffix}"}
                        WHERE cid = $cid;
                        """.trimIndent()
                    stmt.executeQuery(
                        sql
                    )
                } else {
                    val sql = """
                    SELECT *
                    FROM property_situation${if (exportSuffix.isBlank()) "" else "_${exportSuffix}"}
                    WHERE cid = $cid AND `日期` in $iN ORDER BY `日期` DESC;
                    """.trimIndent()

                    stmt.executeQuery(
                        sql
                    )
                }
            }
            else -> throw IllegalStateException("不被支持的选项")
        }
    }

    private fun generateInClause(dates: List<String>): String {
        var iN = dates.toString()
        iN = iN.replace("[", "")
        iN = iN.replace("]", "")
        var iNs = iN.split(",")
        iNs = iNs.map { it.trim() }.map { "'$it'" }
        iN = "(${iNs.reduce { x, y -> "$x, $y" }})"
        return iN
    }

    private fun dataCheck(key: String, value: String, rs: ResultSet): String {
        // 科学记数法
        if (value.contains(Regex("\\.+\\d+E\\d+"))) {
            val tmp = rs.getDouble(key)
            val f = DecimalFormat()
            return f.format(tmp).split(",").reduce { st0, st1 -> st0 + st1 }
        }

        return "EMPTY"
    }

    // delete data
    override fun impButtonDeleteDataActionPerformed(evt: ActionEvent?) {
        CoroutineScope(Dispatchers.IO).launch {
            val dates = (TableMain.model as SelectTableModule).getSelectedDates()
            if (dates.isEmpty()) {
                JOptionPane.showMessageDialog(
                    this@DeveloperMainApp,
                    "没有选择任何数据，不执行删除操作",
                    "操作失败",
                    JOptionPane.WARNING_MESSAGE
                )
            } else {
                val opt = JOptionPane.showConfirmDialog(
                    this@DeveloperMainApp,
                    "是否要执行删除操作？",
                    "行事请三思",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                )

                if (opt == JOptionPane.YES_NO_OPTION) {
                    try {
                        val cid = TextFieldCidInput.text.trim()
                        ProgressBarDoing.isIndeterminate = true
                        LabelDoing.text = "正在执行数据删除"
                        val start = System.nanoTime()

                        val rs = when (selection) {
                            1 -> deleteData(
                                this@DeveloperMainApp.jdbi,
                                "marketing_situation${if (exportSuffix.isBlank()) "" else "_${exportSuffix}"}",
                                cid.toLong(),
                                dates,
                                0
                            )

                            2 -> deleteData(
                                this@DeveloperMainApp.jdbi,
                                "hotitem_ranking${if (exportSuffix.isBlank()) "" else "_${exportSuffix}"}",
                                cid.toLong(),
                                dates,
                                1
                            )

                            3 -> deleteData(
                                this@DeveloperMainApp.jdbi,
                                "hotshop_ranking${if (exportSuffix.isBlank()) "" else "_${exportSuffix}"}",
                                cid.toLong(),
                                dates,
                                1
                            )

                            4 -> deleteData(
                                this@DeveloperMainApp.jdbi,
                                "property_situation${if (exportSuffix.isBlank()) "" else "_${exportSuffix}"}",
                                cid.toLong(),
                                dates,
                                0
                            )

                            else -> throw IllegalStateException("Unsupported selection code")
                        }

                        val end = System.nanoTime()
                        ProgressBarDoing.isIndeterminate = false
                        LabelDoing.text = "操作完成, 耗时${TimeUnit.NANOSECONDS.toMillis(end - start)}毫秒, 删除${rs}条数据"

                        JOptionPane.showMessageDialog(
                            this@DeveloperMainApp,
                            "已删除${rs}条数据",
                            "操作成功",
                            JOptionPane.INFORMATION_MESSAGE
                        )
                    } catch (e: Exception) {
                        JOptionPane.showMessageDialog(
                            this@DeveloperMainApp,
                            e,
                            "操作失败",
                            JOptionPane.WARNING_MESSAGE
                        )
                    }
                } else {
                    LabelDoing.text = "取消执行删除操作"
                }
            }
        }

    }

    /**
     * By default, when type = 0, delete on WM and PR table
     * */
    private fun deleteData(jdbi: Jdbi, table: String, cid: Long, dates: List<String>, type: Int = 0): Int {
        return when (type) {
            0 -> jdbi.withExtension(DeleteDAO::class.java, ExtensionCallback {
                it.deleteWMPRData(table, cid, dates)
            })

            1 -> jdbi.withExtension(DeleteDAO::class.java, ExtensionCallback {
                it.deleteHIHSData(table, cid, dates)
            })

            else -> throw IllegalStateException("Unsupported type $type")
        }
    }

    override fun impMenuItemAddSuffixActionPerformed(evt: ActionEvent?) {
        when (val suffix = JOptionPane.showInputDialog(this, "Enter Query Suffix to Change target Table")) {
            null -> {
                this.exportSuffix = ""
                this.title =
                    "Cid Query Tool - v0.0.4_4 Developer Version with suffix - ${exportSuffix.ifBlank { "origin" }}"
            }
            in suffixLimit -> {
                this.exportSuffix = suffix.trim().ifBlank { "" }
                this.title =
                    "Cid Query Tool - v0.0.4_4 Developer Version with suffix - ${exportSuffix.ifBlank { "origin" }}"
            }
            else -> {
                JOptionPane.showMessageDialog(
                    this,
                    "不被支持的suffix值：$suffix，将设置为默认值",
                    "操作失败",
                    JOptionPane.WARNING_MESSAGE
                )
                this.exportSuffix = ""
                this.title =
                    "Cid Query Tool - v0.0.4_4 Developer Version with suffix - ${exportSuffix.ifBlank { "origin" }}"
            }
        }
    }
}
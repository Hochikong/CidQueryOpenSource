package deprecated
//
//import codegen.impMainScene
//import com.zaxxer.hikari.HikariConfig
//import com.zaxxer.hikari.HikariDataSource
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import me.ckho.StatisticsDAO
//import org.apache.poi.xssf.streaming.SXSSFWorkbook
//import org.jdbi.v3.core.Jdbi
//import org.jdbi.v3.core.extension.ExtensionCallback
//import java.awt.Color
//import java.awt.event.ActionEvent
//import java.awt.event.ItemEvent
//import java.io.File
//import java.io.FileOutputStream
//import java.lang.NumberFormatException
//import java.lang.management.ManagementFactory
//import java.lang.management.OperatingSystemMXBean
//import java.sql.ResultSet
//import java.text.DecimalFormat
//import java.util.concurrent.TimeUnit
//import javax.swing.JFileChooser
//import javax.swing.JOptionPane
//import kotlin.concurrent.thread
//import kotlin.system.exitProcess
//
//
//class UserMainApp(options: Array<String>) : impMainScene(options, TableModule(1)) {
//    private val dbConfig = HikariConfig().apply {
//        username = "apps"
//        password = "Di_2021^01@25"
//        jdbcUrl = "jdbc:mysql://rm-uf6g43nsand8zqplio.mysql.rds.aliyuncs.com:3306/taodata_source"
//        maximumPoolSize = 1
//    }
//
//    private lateinit var ds: HikariDataSource
//
//    private var selection: Int = 1
//
//    private lateinit var jdbi: Jdbi
//
//    private var connected = false
//
//    private lateinit var fileChooser: JFileChooser
//
//    private var outputDir = "."
//
//    private var hasCid = false
//
//    init {
//        this.title = "Cid Query Tool - v0.0.3 User Version"
//    }
//
//    override fun impButtonCleanCidInputActionPerformed(evt: ActionEvent?) {
//        this.TextFieldCidInput.text = ""
//        JOptionPane.showMessageDialog(this, "输入框已清空")
//    }
//
//    override fun impMenuItemExitActionPerformed(evt: ActionEvent?) {
//        try {
//            ds.close()
//        } catch (e: UninitializedPropertyAccessException) {
//        }
//        exitProcess(0)
//    }
//
//    override fun impMenuItemConnActionPerformed(evt: ActionEvent?) {
//        try {
//            this.ds = HikariDataSource(this.dbConfig)
//            this.jdbi = Jdbi.create(this.ds)
//            this.jdbi.installPlugins()
//            this.LabelStatusValue.foreground = Color(0, 204, 153)
//            this.LabelStatusValue.text = "Connected"
//            this.connected = true
//        } catch (e: Exception) {
//            JOptionPane.showMessageDialog(this, e, "ERROR:无法连接到数据库，请联系开发者", JOptionPane.ERROR_MESSAGE)
//        }
//    }
//
//    override fun impComboBoxModulesItemStateChanged(evt: ItemEvent) {
//        if (evt.stateChange == ItemEvent.SELECTED) {
//            val item = evt.item as String
//            this.selection = when (item) {
//                "市场关注规模" -> 1
//                "热销宝贝" -> 2
//                "热销店铺" -> 3
//                "属性成交分布-品牌" -> 4
//                else -> throw IllegalStateException("不被支持的选项")
//            }
//        }
//    }
//
//    override fun impMenuItemAboutActionPerformed(evt: ActionEvent?) {
//        val system: OperatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean()
//        val runtime = ManagementFactory.getRuntimeMXBean()
//        val info = "OS: ${system.name} \nArch: ${system.arch} " +
//                "\nVM: ${runtime.vmName} \nVendor: ${runtime.vmVendor}" +
//                "\nSpec: ${runtime.specVersion} \nVersion: ${runtime.vmVersion}"
//        JOptionPane.showMessageDialog(
//            this,
//            "淘数据cid自助查询工具 \ndeckardhe@seasda.com \n\n$info",
//            "About",
//            JOptionPane.INFORMATION_MESSAGE
//        )
//    }
//
//    override fun impButtonSearchActionPerformed(evt: ActionEvent?) {
//        if (!this.connected) {
//            JOptionPane.showMessageDialog(this, "你尚未连接数据，无法查询", "查询失败", JOptionPane.WARNING_MESSAGE)
//        } else {
//            CoroutineScope(Dispatchers.IO).launch {
//                val cid = TextFieldCidInput.text
//                if (cid.trim().isBlank()) {
//                    JOptionPane.showMessageDialog(this@UserMainApp, "无CID输入，不执行查询/导出", "查询失败", JOptionPane.WARNING_MESSAGE)
//                    TextFieldCidInput.text = ""
//                } else {
//                    ProgressBarDoing.isIndeterminate = true
//                    LabelDoing.text = "正在执行查询"
//                    val start = System.nanoTime()
//
//                    try {
//                        val qr = when (selection) {
//                            1 -> jdbi.withExtension(StatisticsDAO::class.java, ExtensionCallback {
//                                it.queryWM(cid.toLong())
//                            })
//
//                            2 -> jdbi.withExtension(StatisticsDAO::class.java, ExtensionCallback {
//                                it.queryHI(cid.toLong())
//                            })
//
//                            3 -> jdbi.withExtension(StatisticsDAO::class.java, ExtensionCallback {
//                                it.queryHS(cid.toLong())
//                            })
//
//                            4 -> jdbi.withExtension(StatisticsDAO::class.java, ExtensionCallback {
//                                it.queryPR(cid.toLong())
//                            })
//
//                            else -> throw IllegalStateException("不被支持的选项")
//                        }
//                        val arrayQr = qr.map { it.toUAArray() }.toTypedArray()
//                        if (arrayQr.isNotEmpty()) {
//                            TableMain.model = buildModel(arrayQr)
//                            TableMain.autoCreateRowSorter = true
//                            hasCid = true
//                        } else {
//                            TableMain.model = buildModel(arrayOf(arrayOf("EMPTY", "0", "EMPTY")))
//                            TableMain.autoCreateRowSorter = true
//                            hasCid = false
//                        }
//                    } catch (e: NumberFormatException) {
//                        JOptionPane.showMessageDialog(this@UserMainApp, "CID格式错误，无法查询", "查询失败", JOptionPane.WARNING_MESSAGE)
//                    }
//
//
//                    val end = System.nanoTime()
//
//                    ProgressBarDoing.isIndeterminate = false
//                    LabelDoing.text = "查询完成, 耗时${TimeUnit.NANOSECONDS.toMillis(end - start)}毫秒"
//                }
//            }
//        }
//    }
//
//    private fun buildModel(data: Array<Array<String>>): TableModule {
//        return TableModule(this.selection, data)
//    }
//
//    override fun impButtonExportDataActionPerformed(evt: ActionEvent?) {
//        if (!this.connected) {
//            JOptionPane.showMessageDialog(this, "你尚未连接数据，无法查询", "查询失败", JOptionPane.WARNING_MESSAGE)
//        } else if (!this.hasCid) {
//            JOptionPane.showMessageDialog(this, "CID不存在，无法导出", "查询失败", JOptionPane.WARNING_MESSAGE)
//        } else {
//            fileChooser = JFileChooser().apply {
//                dialogTitle = "选择导出目录"
//                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
//                isAcceptAllFileFilterUsed = false
//            }
//
//            if (this.outputDir.length > 1) {
//                fileChooser.currentDirectory = File(outputDir)
//            } else {
//                fileChooser.currentDirectory = File(".")
//            }
//
//            // open dialog
//            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
//                this.outputDir = fileChooser.selectedFile.toString()
//                // export
//                val cid = TextFieldCidInput.text
//
//                CoroutineScope(Dispatchers.IO).launch {
//                    if (cid.trim().isBlank()) {
//                        JOptionPane.showMessageDialog(
//                            this@UserMainApp,
//                            "无CID输入，不执行查询/导出",
//                            "查询失败",
//                            JOptionPane.WARNING_MESSAGE
//                        )
//                        TextFieldCidInput.text = ""
//                    } else {
//                        ProgressBarDoing.isIndeterminate = true
//                        LabelDoing.text = "正在执行数据导出"
//                        val start = System.nanoTime()
//
//                        // do
//                        val conn = ds.connection
//                        val stmt = conn.createStatement()
//                        val rs = when (selection) {
//                            1 -> {
//                                stmt.executeQuery(
//                                    """
//                                SELECT *
//                                FROM marketing_situation
//                                WHERE cid = $cid;
//                            """.trimIndent()
//                                )
//                            }
//
//                            2 -> {
//                                stmt.executeQuery(
//                                    """
//                                SELECT *
//                                FROM hotitem_ranking
//                                WHERE cid = $cid;
//                            """.trimIndent()
//                                )
//                            }
//
//                            3 -> {
//                                stmt.executeQuery(
//                                    """
//                                SELECT *
//                                FROM hotshop_ranking
//                                WHERE cid = $cid;
//                            """.trimIndent()
//                                )
//                            }
//
//                            4 -> {
//                                stmt.executeQuery(
//                                    """
//                                SELECT *
//                                FROM property_situation
//                                WHERE cid = $cid;
//                            """.trimIndent()
//                                )
//                            }
//                            else -> throw IllegalStateException("不被支持的选项")
//                        }
//
//                        val mt = rs.metaData
//                        val columnNames = (1..mt.columnCount).map { mt.getColumnName(it) }.toTypedArray()
//                        val rowData: MutableList<Array<String>> = mutableListOf(columnNames)
//
//                        // scan data
//                        while (rs.next()) {
//                            val tmp = mutableListOf<String>()
//
//                            for (key in columnNames) {
//                                val t = rs.getString(key)
//                                if (t != null) {
//                                    when (val nt = dataCheck(key, t, rs)) {
//                                        "EMPTY" -> tmp.add(t)
//                                        else -> tmp.add(nt)
//                                    }
//                                } else {
//                                    tmp.add("")
//                                }
//                            }
//
//                            rowData.add(tmp.toTypedArray())
//                        }
//
//                        // generate excel
////                        val workbook = XSSFWorkbook()
//                        val workbook = SXSSFWorkbook(200)
//                        val sheet = workbook.createSheet("Export")
//
//                        for (i in 0 until rowData.size) {
//                            val row = sheet.createRow(i)
//                            for (j in columnNames.indices) {
//                                val cell = row.createCell(j)
//                                cell.setCellValue(rowData[i][j])
//                            }
//                        }
//
//                        thread(start = true) {
//                            FileOutputStream(
//                                File("$outputDir/${TextFieldCidInput.text}-${options[selection - 1]}.xlsx")
//                            ).use {
//                                workbook.write(it)
//                            }
//
//                            workbook.close()
//                        }
//
//                        stmt.close()
//                        conn.close()
//                        // do end
//
//                        val end = System.nanoTime()
//                        ProgressBarDoing.isIndeterminate = false
//                        LabelDoing.text = "查询完成, 耗时${TimeUnit.NANOSECONDS.toMillis(end - start)}毫秒"
//                    }
//                }
//            } else {
//                LabelDoing.text = "取消文件导出"
//            }
//        }
//    }
//
//    private fun dataCheck(key: String, value: String, rs: ResultSet): String {
//        // 科学记数法
//        if (value.contains(Regex("\\.+\\d+E\\d+"))) {
//            val tmp = rs.getDouble(key)
//            val f = DecimalFormat()
//            return f.format(tmp).split(",").reduce { st0, st1 -> st0 + st1 }
//        }
//
//        return "EMPTY"
//    }
//}
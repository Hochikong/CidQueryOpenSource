import com.formdev.flatlaf.FlatIntelliJLaf
import me.ckho.DeveloperMainApp
import java.awt.EventQueue

fun main() {
    FlatIntelliJLaf.install()
    val c = DeveloperMainApp(arrayOf("HotMarket", "HotItems", "HotShops", "HotBrands"))
    EventQueue.invokeLater { c.isVisible = true }
}
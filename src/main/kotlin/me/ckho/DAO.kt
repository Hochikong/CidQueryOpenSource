package me.ckho

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.customizer.BindList
import org.jdbi.v3.sqlobject.customizer.Define
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate

data class Statistics(
    var cst: String? = null,
    var cid: String = "empty",
    var stat: String = "0",
    var date: String = "1970-01-01",
    var select: Boolean = false
) {
    fun toDVArray(): Array<Any> {
        return if (cst == null) {
            arrayOf("Not Found", cid, stat, date, select)
        } else {
            arrayOf(cst as String, cid, stat, date, select)
        }
    }
}

interface StatisticsDAO {
    @SqlQuery(
        """
        SELECT cst, cid, COUNT(DISTINCT platform) stat, 日期 date
        FROM <table> LEFT JOIN (SELECT cid AS d, `四级行业` as cst FROM category_list WHERE cid = :cid) AS tmp ON cid = tmp.d
        WHERE cid = :cid
        GROUP BY tmp.cst, cid, date
        ORDER BY date DESC;
    """
    )
    @RegisterBeanMapper(Statistics::class)
    fun queryWM(@Bind("cid") cid: Long, @Define("table") table: String = "marketing_situation"): List<Statistics>

    @SqlQuery(
        """
        SELECT cst, cid, COUNT(DISTINCT platform) stat, date
        FROM <table> LEFT JOIN (SELECT cid AS d, `四级行业` as cst FROM category_list WHERE cid = :cid) AS tmp ON cid = tmp.d
        WHERE cid = :cid
        GROUP BY tmp.cst, cid, date
        ORDER BY date DESC;
    """
    )
    @RegisterBeanMapper(Statistics::class)
    fun queryHI(@Bind("cid") cid: Long, @Define("table") table: String = "hotitem_ranking"): List<Statistics>

    @SqlQuery(
        """
        SELECT cst, cid, COUNT(DISTINCT platform) stat, date
        FROM <table> LEFT JOIN (SELECT cid AS d, `四级行业` as cst FROM category_list WHERE cid = :cid) AS tmp ON cid = tmp.d
        WHERE cid = :cid
        GROUP BY tmp.cst, cid, date
        ORDER BY date DESC;
    """
    )
    @RegisterBeanMapper(Statistics::class)
    fun queryHS(@Bind("cid") cid: Long, @Define("table") table: String = "hotshop_ranking"): List<Statistics>

    @SqlQuery(
        """
        SELECT cst, cid, COUNT(DISTINCT platform) stat, 日期 date
        FROM <table> LEFT JOIN (SELECT cid AS d, `四级行业` as cst FROM category_list WHERE cid = :cid) AS tmp ON cid = tmp.d
        WHERE cid = :cid
        GROUP BY tmp.cst, cid, date
        ORDER BY date DESC;
    """
    )
    @RegisterBeanMapper(Statistics::class)
    fun queryPR(@Bind("cid") cid: Long, @Define("table") table: String = "property_situation"): List<Statistics>
}

interface DeleteDAO {
    @SqlUpdate(
        """
            DELETE FROM <table> WHERE cid = :cid AND `日期` IN (<dates>);
        """
    )
    fun deleteWMPRData(
        @Define("table") table: String,
        @Bind("cid") cid: Long,
        @BindList("dates") dates: List<String>
    ): Int

    @SqlUpdate(
        """
            DELETE FROM <table> WHERE cid = :cid AND date IN (<dates>);
        """
    )
    fun deleteHIHSData(
        @Define("table") table: String,
        @Bind("cid") cid: Long,
        @BindList("dates") dates: List<String>
    ): Int
}